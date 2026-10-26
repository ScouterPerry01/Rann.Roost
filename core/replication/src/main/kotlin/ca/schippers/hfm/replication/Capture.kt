// File path and name: core/replication/src/main/kotlin/ca/schippers/hfm/replication/Capture.kt
// Modified On Timestamp: 2026-10-26 @ 04:19 EDT
// Created On Timestamp: 2026-10-26 @ 04:19 EDT
// File Description: Change capture inside SQLite: generated triggers that write every change to repl_outbox (plan-multi-device 4.1).
// Uses: PolicyManifest; the repl_outbox and repl_applying tables (core/data Replication.sq); SchemaManager.
// Used By: Turning sharing on and opening a shared household (later phases); CaptureTest.
// Purpose: Capture every change in one place, below the ~50 services that write the database (problem P1).
package ca.schippers.hfm.replication

import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import ca.schippers.hfm.data.SchemaManager

/** A table column as SQLite reports it: its name, declared type and place in the primary key (0 = not in it). */
data class TableColumn(val name: String, val type: String, val pk: Int) {
    val isBlob: Boolean get() = type.equals("BLOB", ignoreCase = true)

    /** The column's value in a trigger or query, with binary values as hex text so they fit in JSON. */
    fun sql(row: String): String = if (isBlob) "hex($row.${quote(name)})" else "$row.${quote(name)}"
}

/**
 * Makes and removes the capture triggers of one database. Each synced table gets three triggers:
 * after insert (every column), after update (only the columns whose value changed; nothing when
 * nothing changed) and after delete (the key). They write to `repl_outbox` unless a row is in
 * `repl_applying`. Local-only tables are never captured. The list comes from [PolicyManifest]: a
 * table without a policy stops the install, so nothing is ever left out by accident.
 */
object CaptureTriggers {

    fun install(driver: SqlDriver, database: Database) {
        val tables = tables(driver)
        val missing = tables.filter { PolicyManifest.of(database, it) == null }
        check(missing.isEmpty()) { "Tables without a merge policy: $missing" }
        val idle = "NOT EXISTS (SELECT 1 FROM repl_applying)"
        transaction(driver) {
            SchemaManager.dropCaptureTriggers(driver)
            for (table in tables.filter { PolicyManifest.of(database, it)!!.policy != Policy.LOCAL_ONLY }) {
                val columns = columns(driver, table)
                val keys = keys(columns)
                check(keys.isNotEmpty()) { "$table has no primary key" }
                val t = quote(table)
                fun key(row: String) = "json_object(${keys.joinToString { "'${it.name}', ${it.sql(row)}" }})"
                // Built from rows, not one json_object call: wide tables would pass SQLite's argument limit.
                val all = "(SELECT json_group_object(k, v) FROM (${columns.joinToString(" UNION ALL ") { "SELECT '${it.name}' AS k, ${it.sql("NEW")} AS v" }}))"
                val changed = columns.joinToString(" UNION ALL ") {
                    "SELECT '${it.name}' AS k, ${it.sql("NEW")} AS v WHERE OLD.${quote(it.name)} IS NOT NEW.${quote(it.name)}"
                }
                val anyChange = columns.joinToString(" OR ") { "OLD.${quote(it.name)} IS NOT NEW.${quote(it.name)}" }
                exec(
                    driver,
                    "CREATE TRIGGER ${quote("repl_i_$table")} AFTER INSERT ON $t WHEN $idle BEGIN " +
                        "INSERT INTO repl_outbox(tbl, pk, kind, cols) VALUES ('$table', ${key("NEW")}, 'I', $all); END",
                )
                exec(
                    driver,
                    "CREATE TRIGGER ${quote("repl_u_$table")} AFTER UPDATE ON $t WHEN $idle AND ($anyChange) BEGIN " +
                        "INSERT INTO repl_outbox(tbl, pk, kind, cols) VALUES ('$table', ${key("NEW")}, 'U', " +
                        "(SELECT json_group_object(k, v) FROM ($changed))); END",
                )
                exec(
                    driver,
                    "CREATE TRIGGER ${quote("repl_d_$table")} AFTER DELETE ON $t WHEN $idle BEGIN " +
                        "INSERT INTO repl_outbox(tbl, pk, kind) VALUES ('$table', ${key("OLD")}, 'D'); END",
                )
            }
        }
    }

    /** Stops capturing: the outbox keeps what it already holds. */
    fun remove(driver: SqlDriver) = SchemaManager.dropCaptureTriggers(driver)

    fun installed(driver: SqlDriver): Boolean =
        strings(driver, "SELECT name FROM sqlite_master WHERE type = 'trigger' AND name LIKE 'repl\\_%' ESCAPE '\\' LIMIT 1").isNotEmpty()

    fun tables(driver: SqlDriver): List<String> =
        strings(driver, "SELECT name FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite\\_%' ESCAPE '\\' ORDER BY name")

    fun columns(driver: SqlDriver, table: String): List<TableColumn> =
        driver.executeQuery(null, "SELECT name, type, pk FROM pragma_table_info(?)", { c ->
            val out = ArrayList<TableColumn>()
            while (c.next().value) out += TableColumn(c.getString(0)!!, c.getString(1).orEmpty(), c.getLong(2)!!.toInt())
            QueryResult.Value(out)
        }, 1) { bindString(0, table) }.value

    fun keys(columns: List<TableColumn>): List<TableColumn> = columns.filter { it.pk > 0 }.sortedBy { it.pk }

    private fun exec(driver: SqlDriver, sql: String) {
        driver.execute(null, sql, 0)
    }

    private fun strings(driver: SqlDriver, sql: String): List<String> = driver.executeQuery(null, sql, { c ->
        val out = ArrayList<String>()
        while (c.next().value) out += c.getString(0)!!
        QueryResult.Value(out)
    }, 0).value
}

/** One change waiting in the outbox. [pk] and [cols] are JSON objects; [kind] is I, U or D. */
data class CapturedChange(
    val seq: Long,
    val table: String,
    val pk: String,
    val kind: String,
    val cols: String?,
    val hlc: HlcStamp,
    val userId: String,
)

/**
 * After each committed transaction, gives the changes it captured their clock values and author,
 * in the order they were made (plan 4.1). One call is one transaction's worth of changes: they
 * travel together as one unit to every computer.
 */
object Outbox {

    fun seal(driver: SqlDriver, clock: HybridClock, userId: String): List<CapturedChange> {
        val pending = driver.executeQuery(null, "SELECT seq, tbl, pk, kind, cols FROM repl_outbox WHERE hlc IS NULL ORDER BY seq", { c ->
            val out = ArrayList<Pending>()
            while (c.next().value) out += Pending(c.getLong(0)!!, c.getString(1)!!, c.getString(2)!!, c.getString(3)!!, c.getString(4))
            QueryResult.Value(out)
        }, 0).value
        if (pending.isEmpty()) return emptyList()
        val sealed = pending.map { CapturedChange(it.seq, it.table, it.pk, it.kind, it.cols, clock.now(), userId) }
        transaction(driver) {
            for (c in sealed) {
                driver.execute(null, "UPDATE repl_outbox SET hlc = ?, user_id = ? WHERE seq = ?", 3) {
                    bindString(0, c.hlc.toString())
                    bindString(1, c.userId)
                    bindLong(2, c.seq)
                }
            }
        }
        return sealed
    }

    private class Pending(val seq: Long, val table: String, val pk: String, val kind: String, val cols: String?)
}

internal fun transaction(driver: SqlDriver, block: () -> Unit) {
    object : TransacterImpl(driver) {}.transaction { block() }
}

internal fun quote(name: String): String = "\"" + name.replace("\"", "\"\"") + "\""
