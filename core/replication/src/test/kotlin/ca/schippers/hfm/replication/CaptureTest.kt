// File path and name: core/replication/src/test/kotlin/ca/schippers/hfm/replication/CaptureTest.kt
// Modified On Timestamp: 2026-10-26 @ 04:19 EDT
// Created On Timestamp: 2026-10-26 @ 04:19 EDT
// File Description: Change capture on the real schemas: triggers, outbox, sealing, and replay into an empty copy.
// Uses: CaptureTriggers, Outbox, HybridClock; the SQLCipher driver; Replay (test helper).
// Used By: ./gradlew :core:replication:test.
// Purpose: Prove every change is captured once, in order, and can rebuild an identical copy (plan-multi-device 4.1, 10).
package ca.schippers.hfm.replication

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import ca.schippers.hfm.data.SchemaManager
import ca.schippers.hfm.data.core.CoreDatabase
import ca.schippers.hfm.data.jdbc.SqlCipherJdbcDriverFactory
import ca.schippers.hfm.data.ledger.LedgerDatabase
import ca.schippers.hfm.security.Random
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CaptureTest {

    @TempDir
    lateinit var temp: Path

    private val device = "0192f0a1-0000-7000-8000-00000000000a"

    private fun open(name: String, database: Database = Database.LEDGER, capture: Boolean = true): SqlDriver =
        SqlCipherJdbcDriverFactory().open(temp.resolve(name), Random.key()).also {
            when (database) {
                Database.CORE -> CoreDatabase.Schema.create(it)
                Database.LEDGER -> LedgerDatabase.Schema.create(it)
            }
            if (capture) CaptureTriggers.install(it, database)
        }

    private val account = "INSERT INTO account(id, name, type, currency, opening_date, created_at, updated_at) " +
        "VALUES ('a1', 'Chequing', 'CHEQUING', 'CAD', '2026-01-01', 1, 1)"

    private fun txn(id: String, amount: Long = -1000, memo: String? = null) =
        "INSERT INTO txn(id, account_id, date, payee_text, amount_minor, memo, cleared, created_at, updated_at) " +
            "VALUES ('$id', 'a1', '2026-01-02', 'IGA', $amount, ${memo?.let { "'${it.replace("'", "''")}'" } ?: "NULL"}, 'UNCLEARED', 1, 1)"

    @Test
    fun `triggers cover every synced table of both schemas and no local-only one`() {
        for (database in Database.entries) {
            open("${database.name}.db", database).use { d ->
                val synced = PolicyManifest.tables.filter { it.database == database && it.policy != Policy.LOCAL_ONLY }.map { it.table }
                val triggers = d.strings("SELECT name FROM sqlite_master WHERE type = 'trigger'")
                assertEquals(synced.size * 3, triggers.size, database.name)
                assertTrue(triggers.none { t -> PolicyManifest.tables.any { it.policy == Policy.LOCAL_ONLY && t.endsWith("_${it.table}") } })
                CaptureTriggers.install(d, database) // installing again replaces them
                assertEquals(synced.size * 3, d.strings("SELECT name FROM sqlite_master WHERE type = 'trigger'").size)
            }
        }
    }

    @Test
    fun `insert, update of changed columns only, and delete are captured with their keys`() {
        open("l.db").use { d ->
            d.exec(account)
            d.exec("UPDATE account SET name = 'Joint chequing' WHERE id = 'a1'")
            d.exec("UPDATE account SET name = 'Joint chequing' WHERE id = 'a1'") // no change: nothing captured
            d.exec("DELETE FROM account WHERE id = 'a1'")
            val ops = d.strings("SELECT kind || ' ' || tbl || ' ' || pk || ' ' || coalesce(cols, '') FROM repl_outbox ORDER BY seq")
            assertEquals(3, ops.size, "$ops")
            assertTrue(ops[0].startsWith("I account {\"id\":\"a1\"} {") && "\"opening_date\":\"2026-01-01\"" in ops[0], ops[0])
            assertEquals("U account {\"id\":\"a1\"} {\"name\":\"Joint chequing\"}", ops[1])
            assertEquals("D account {\"id\":\"a1\"} ", ops[2])
        }
    }

    @Test
    fun `nothing is captured while another computer's changes are applied`() {
        open("l.db").use { d ->
            d.exec("INSERT INTO repl_applying(id) VALUES (1)")
            d.exec(account)
            d.exec("DELETE FROM repl_applying")
            d.exec(txn("t1"))
            assertEquals(listOf("txn"), d.strings("SELECT tbl FROM repl_outbox"))
        }
    }

    @Test
    fun `sealing stamps the waiting changes in order, once`() {
        open("l.db").use { d ->
            d.exec(account)
            d.exec(txn("t1"))
            var wall = 1_790_000_000_000L
            val clock = HybridClock(device) { wall }
            val first = Outbox.seal(d, clock, "perry")
            assertEquals(listOf("account", "txn"), first.map { it.table })
            assertTrue(first[0].hlc < first[1].hlc)
            assertTrue(first.all { it.userId == "perry" })
            assertEquals(emptyList(), Outbox.seal(d, clock, "perry"))
            wall -= 1_000
            d.exec(txn("t2"))
            val second = Outbox.seal(d, clock, "anne")
            assertTrue(second.single().hlc > first.last().hlc, "a later change is later even when the clock went back")
            assertEquals(0L, d.long("SELECT count(*) FROM repl_outbox WHERE hlc IS NULL"))
        }
    }

    @Test
    fun `a table without a merge policy stops the install`() {
        open("l.db", capture = false).use { d ->
            d.exec("CREATE TABLE brand_new(id TEXT NOT NULL PRIMARY KEY)")
            assertFailsWith<IllegalStateException> { CaptureTriggers.install(d, Database.LEDGER) }
            assertEquals(false, CaptureTriggers.installed(d), "nothing half-installed")
        }
    }

    @Test
    fun `an upgrade removes the triggers, so a migration is never captured`() {
        open("l.db").use { d ->
            assertTrue(CaptureTriggers.installed(d))
            SchemaManager.dropCaptureTriggers(d)
            assertEquals(false, CaptureTriggers.installed(d))
            d.exec(account)
            assertEquals(0L, d.long("SELECT count(*) FROM repl_outbox"))
        }
    }

    @Test
    fun `replaying the captured changes into an empty copy gives identical tables`() {
        val a = open("a.db")
        val b = open("b.db", capture = false)
        a.exec(account)
        a.exec("INSERT INTO account(id, name, type, currency, opening_date, created_at, updated_at, notes) VALUES ('a2', 'Épargne « été »', 'SAVINGS', 'CAD', '2026-01-01', 1, 1, 'l''été')")
        a.exec("INSERT INTO account_owner(account_id, member_id) VALUES ('a1', 'm1'), ('a1', 'm2'), ('a2', 'm1')")
        repeat(120) { i ->
            a.exec(txn("t$i", amount = -(i + 1) * 137L, memo = if (i % 3 == 0) null else "memo $i"))
            a.exec("INSERT INTO txn_split(id, txn_id, category_id, amount_minor) VALUES ('s$i', 't$i', 'c${i % 7}', ${-(i + 1) * 137L})")
        }
        repeat(30) { i -> a.exec("UPDATE txn SET memo = NULL, cleared = 'CLEARED', updated_at = ${1_000 + i} WHERE id = 't${i * 2}'") }
        repeat(10) { i -> a.exec("DELETE FROM txn WHERE id = 't${i * 5 + 1}'") } // cascades to the splits
        a.exec("DELETE FROM account_owner WHERE account_id = 'a1' AND member_id = 'm2'")
        a.exec("INSERT INTO account(id, name, type, currency, opening_date, created_at, updated_at) VALUES ('a2', 'Savings', 'SAVINGS', 'CAD', '2026-01-01', 1, 9) ON CONFLICT(id) DO UPDATE SET name = excluded.name, updated_at = excluded.updated_at")
        Replay.apply(from = a, into = b)
        for (table in CaptureTriggers.tables(a).filter { !it.startsWith("repl_") }) assertEquals(Replay.dump(a, table), Replay.dump(b, table), table)
        assertEquals(110L, b.long("SELECT count(*) FROM txn"))
        a.close(); b.close()
    }

    @Test
    fun `a binary column survives capture and replay`() {
        val a = open("core-a.db", Database.CORE)
        val b = open("core-b.db", Database.CORE, capture = false)
        a.exec("INSERT INTO app_user(id, login_name, display_name, role, public_key, created_at) VALUES ('u1', 'perry', 'Perry', 'ADMINISTRATOR', x'00ff10a7', 1)")
        a.exec("UPDATE app_user SET public_key = x'cafe0001' WHERE id = 'u1'")
        Replay.apply(from = a, into = b)
        assertEquals(Replay.dump(a, "app_user"), Replay.dump(b, "app_user"))
        assertEquals("blob", b.strings("SELECT typeof(public_key) FROM app_user").single())
        a.close(); b.close()
    }
}

/** Applies one database's outbox to another, using only SQL made from the schema (the apply engine comes in phase 3). */
object Replay {
    fun apply(from: SqlDriver, into: SqlDriver) {
        val ops = from.executeQuery(null, "SELECT tbl, pk, kind, cols FROM repl_outbox ORDER BY seq", { c ->
            val out = ArrayList<List<String?>>()
            while (c.next().value) out += listOf(c.getString(0), c.getString(1), c.getString(2), c.getString(3))
            QueryResult.Value(out)
        }, 0).value
        transaction(into) {
            for ((tbl, pk, kind, cols) in ops) {
                val columns = CaptureTriggers.columns(into, tbl!!)
                fun field(c: TableColumn, json: Int) =
                    if (c.isBlob) "unhex(json_extract(?$json, '$.${quoteJson(c.name)}'))" else "json_extract(?$json, '$.${quoteJson(c.name)}')"
                val where = CaptureTriggers.keys(columns).joinToString(" AND ") { "${quote(it.name)} = ${field(it, 2)}" }
                val sql = when (kind) {
                    "I" -> "INSERT INTO ${quote(tbl)}(${columns.joinToString { quote(it.name) }}) VALUES (${columns.joinToString { field(it, 1) }})"
                    "U" -> "UPDATE ${quote(tbl)} SET ${columns.joinToString { "${quote(it.name)} = CASE WHEN json_type(?1, '$.${quoteJson(it.name)}') IS NULL THEN ${quote(it.name)} ELSE ${field(it, 1)} END" }} WHERE $where"
                    else -> "DELETE FROM ${quote(tbl)} WHERE $where AND ?1 IS NOT NULL"
                }
                // SQLite counts parameters up to the highest one used: an insert has only ?1.
                if (kind == "I") into.execute(null, sql, 1) { bindString(0, cols) }
                else into.execute(null, sql, 2) { bindString(0, cols ?: "{}"); bindString(1, pk) }
            }
        }
    }

    fun dump(d: SqlDriver, table: String): List<String> {
        val columns = CaptureTriggers.columns(d, table)
        val order = CaptureTriggers.keys(columns).joinToString { quote(it.name) }
        return d.strings("SELECT json_array(${columns.joinToString { it.sql(quote(table)) }}) FROM ${quote(table)} ORDER BY $order")
    }

    private fun quoteJson(name: String) = "\"$name\""
}

internal fun SqlDriver.exec(sql: String) {
    execute(null, sql, 0)
}

internal fun SqlDriver.long(sql: String): Long = executeQuery(null, sql, { c -> c.next(); QueryResult.Value(c.getLong(0)!!) }, 0).value

internal fun SqlDriver.strings(sql: String): List<String> = executeQuery(null, sql, { c ->
    val out = ArrayList<String>()
    while (c.next().value) out += c.getString(0)!!
    QueryResult.Value(out)
}, 0).value
