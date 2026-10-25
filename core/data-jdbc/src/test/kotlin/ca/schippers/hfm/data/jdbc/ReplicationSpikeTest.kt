// File path and name: core/data-jdbc/src/test/kotlin/ca/schippers/hfm/data/jdbc/ReplicationSpikeTest.kt
// Modified On Timestamp: 2026-10-25 @ 14:40 EDT
// Created On Timestamp: 2026-10-25 @ 14:40 EDT
// File Description: Phase 0 spike for 2.0.0 (docs/plan-multi-device.md §4.1, §4.2): change capture with
//   generated SQLite triggers on the real encrypted ledger and core schemas, replay into a second copy,
//   and a hybrid logical clock.
// Uses: SqlCipherJdbcDriverFactory, LedgerDatabase.Schema, CoreDatabase.Schema (core/data).
// Used By: ./gradlew :core:data-jdbc:test; results recorded in docs/adr/0011-several-computers.md.
// Purpose: Prove (or disprove) the capture design before phase 2 builds it for real. HFM_SPIKE_N sets the
//   number of transactions in the cost test (default 20,000; the plan's figure is 250,000).
package ca.schippers.hfm.data.jdbc

import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import ca.schippers.hfm.data.core.CoreDatabase
import ca.schippers.hfm.data.ledger.LedgerDatabase
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.security.SecureRandom
import java.util.Collections
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.measureTime

class ReplicationSpikeTest {

    @TempDir
    lateinit var temp: Path

    private val factory = SqlCipherJdbcDriverFactory()

    private fun open(name: String): SqlDriver =
        factory.open(temp.resolve(name), ByteArray(32).also { SecureRandom().nextBytes(it) })

    private fun ledger(name: String = "ledger.db", capture: Boolean = true): SqlDriver =
        open(name).also { LedgerDatabase.Schema.create(it); if (capture) Capture.install(it) }

    @Test
    fun `triggers install on every table of both schemas`() {
        val ledger = ledger()
        val core = open("core.db").also { CoreDatabase.Schema.create(it); Capture.install(it) }
        for ((name, driver) in listOf("ledger" to ledger, "core" to core)) {
            val tables = Capture.tables(driver)
            val triggers = driver.strings("SELECT name FROM sqlite_master WHERE type = 'trigger' AND name LIKE 'sync_%'")
            val noKey = tables.filter { t -> Capture.columns(driver, t).none { it.pk > 0 } }
            val widest = tables.maxBy { Capture.columns(driver, it).size }
            val blobs = tables.flatMap { t -> Capture.columns(driver, t).filter { it.isBlob }.map { "$t.${it.name}" } }
            println("$name: ${tables.size} tables, ${triggers.size} triggers, widest $widest (${Capture.columns(driver, widest).size} columns), without key $noKey, BLOB $blobs")
            assertEquals(tables.size * 3, triggers.size)
            assertTrue(noKey.isEmpty(), "every table has a primary key: $noKey")
        }
        ledger.close(); core.close()
    }

    @Test
    fun `insert, update of changed columns only, and delete are captured`() {
        val db = ledger()
        db.exec(ACCOUNT)
        db.exec("UPDATE account SET name = 'Joint chequing' WHERE id = 'a1'")
        db.exec("UPDATE account SET name = 'Joint chequing' WHERE id = 'a1'") // no change: nothing captured
        db.exec("DELETE FROM account WHERE id = 'a1'")
        val ops = db.strings("SELECT kind || ' ' || tbl || ' ' || pk || ' ' || coalesce(cols, '') FROM sync_outbox ORDER BY seq")
        ops.forEach(::println)
        assertEquals(3, ops.size)
        assertTrue(ops[0].startsWith("I account [\"a1\"]") && "\"opening_date\":\"2026-01-01\"" in ops[0])
        assertEquals("U account [\"a1\"] {\"name\":\"Joint chequing\"}", ops[1])
        assertEquals("D account [\"a1\"] ", ops[2])
        db.close()
    }

    @Test
    fun `cascaded deletes fire the children's triggers`() {
        val db = ledger()
        db.exec(ACCOUNT)
        db.exec("INSERT INTO account_owner(account_id, member_id) VALUES ('a1', 'm1')")
        db.exec(txn("t1"))
        db.exec("INSERT INTO txn_split(id, txn_id, category_id, amount_minor) VALUES ('s1', 't1', 'c1', -1000), ('s2', 't1', 'c2', -500)")
        db.exec("DELETE FROM sync_outbox")
        db.exec("DELETE FROM txn WHERE id = 't1'")
        val ops = db.strings("SELECT kind || ' ' || tbl || ' ' || pk FROM sync_outbox ORDER BY seq")
        println(ops)
        assertTrue("D txn_split [\"s1\"]" in ops && "D txn_split [\"s2\"]" in ops && "D txn [\"t1\"]" in ops, "$ops")
        db.close()
    }

    @Test
    fun `an upsert that hits an existing row is captured as an update`() {
        val db = ledger()
        db.exec(ACCOUNT)
        db.exec("DELETE FROM sync_outbox")
        db.exec(
            "INSERT INTO account(id, name, type, currency, opening_date, created_at, updated_at) " +
                "VALUES ('a1', 'Renamed', 'CHEQUING', 'CAD', '2026-01-01', 1, 2) " +
                "ON CONFLICT(id) DO UPDATE SET name = excluded.name, updated_at = excluded.updated_at",
        )
        val ops = db.strings("SELECT kind || ' ' || coalesce(cols, '') FROM sync_outbox ORDER BY seq")
        println(ops)
        assertEquals(listOf("U {\"name\":\"Renamed\",\"updated_at\":2}"), ops)
        db.close()
    }

    @Test
    fun `writes made while applying are not captured, and other threads wait for the apply to finish`() {
        val db = ledger()
        db.exec(ACCOUNT)
        db.exec("DELETE FROM sync_outbox")
        val inApply = CountDownLatch(1)
        val errors = Collections.synchronizedList(ArrayList<Throwable>())
        val applier = thread {
            runCatching {
                db.inTransaction {
                    db.exec("INSERT INTO sync_apply(flag) VALUES (1)")
                    inApply.countDown()
                    db.exec(txn("remote"))
                    Thread.sleep(300) // the screen thread tries to write meanwhile
                    db.exec("DELETE FROM sync_apply")
                }
            }.exceptionOrNull()?.let(errors::add)
        }
        inApply.await()
        val screen = thread { runCatching { db.exec(txn("local")) }.exceptionOrNull()?.let(errors::add) }
        applier.join(); screen.join()
        assertTrue(errors.isEmpty(), "$errors")
        val ops = db.strings("SELECT pk FROM sync_outbox WHERE tbl = 'txn'")
        assertEquals(listOf("[\"local\"]"), ops)
        assertEquals(2L, db.long("SELECT count(*) FROM txn"))
        db.close()
    }

    @Test
    fun `a trigger in the database file cannot read a temp table`() {
        val db = ledger(capture = false)
        db.exec("CREATE TEMP TABLE ctx(user_id TEXT)")
        val result = runCatching {
            db.exec("CREATE TRIGGER probe AFTER INSERT ON account BEGIN INSERT INTO ctx(user_id) VALUES ('x'); END")
            db.exec(ACCOUNT)
        }
        println("main trigger using a temp table: ${result.exceptionOrNull()?.message ?: "works"}")
        assertTrue(result.isFailure, "SQLite keeps triggers of the file from reaching temp objects")
        db.close()
    }

    @Test
    fun `replaying the captured changes gives an identical second copy`() {
        val a = ledger("a.db")
        val b = ledger("b.db", capture = false)
        a.exec(ACCOUNT)
        a.exec("INSERT INTO account(id, name, type, currency, opening_date, created_at, updated_at, notes) VALUES ('a2', 'Épargne « été »', 'SAVINGS', 'CAD', '2026-01-01', 1, 1, 'l''été')")
        a.exec("INSERT INTO account_owner(account_id, member_id) VALUES ('a1', 'm1'), ('a1', 'm2'), ('a2', 'm1')")
        repeat(200) { i ->
            a.exec(txn("t$i", amount = -(i + 1) * 137L, memo = if (i % 3 == 0) null else "memo $i"))
            a.exec("INSERT INTO txn_split(id, txn_id, category_id, amount_minor) VALUES ('s$i', 't$i', 'c${i % 7}', ${-(i + 1) * 137L})")
        }
        repeat(50) { i -> a.exec("UPDATE txn SET memo = NULL, cleared = 'CLEARED', updated_at = ${1_000 + i} WHERE id = 't${i * 2}'") }
        repeat(20) { i -> a.exec("DELETE FROM txn WHERE id = 't${i * 5 + 1}'") }
        a.exec("UPDATE txn SET refund_of = 't2' WHERE id = 't3'")
        a.exec("DELETE FROM account_owner WHERE account_id = 'a1' AND member_id = 'm2'")
        a.exec("INSERT INTO account(id, name, type, currency, opening_date, created_at, updated_at) VALUES ('a2', 'Savings', 'SAVINGS', 'CAD', '2026-01-01', 1, 9) ON CONFLICT(id) DO UPDATE SET name = excluded.name, updated_at = excluded.updated_at")

        val replayed = measureTime { Capture.replay(from = a, into = b) }
        println("replayed ${a.long("SELECT count(*) FROM sync_outbox")} changes in $replayed")
        for (table in Capture.tables(a)) assertEquals(Capture.dump(a, table), Capture.dump(b, table), table)
        assertEquals(180L, b.long("SELECT count(*) FROM txn"))
        a.close(); b.close()
    }

    @Test
    fun `a binary column survives capture and replay`() {
        val a = open("core-a.db").also { CoreDatabase.Schema.create(it); Capture.install(it) }
        val b = open("core-b.db").also { CoreDatabase.Schema.create(it) }
        a.exec("INSERT INTO app_user(id, login_name, display_name, role, public_key, created_at) VALUES ('u1', 'perry', 'Perry', 'ADMINISTRATOR', x'00ff10a7', 1)")
        a.exec("UPDATE app_user SET public_key = x'cafe0001' WHERE id = 'u1'")
        Capture.replay(from = a, into = b)
        assertEquals(Capture.dump(a, "app_user"), Capture.dump(b, "app_user"))
        assertEquals("CAFE0001", b.strings("SELECT hex(public_key) FROM app_user").single())
        assertEquals("blob", b.strings("SELECT typeof(public_key) FROM app_user").single())
        a.close(); b.close()
    }

    @Test
    fun `cost of capture on a large import`() {
        val n = System.getenv("HFM_SPIKE_N")?.toIntOrNull() ?: 20_000
        fun load(db: SqlDriver) = measureTime {
            db.exec(ACCOUNT)
            db.inTransaction {
                for (i in 0 until n) {
                    db.exec(txn("t$i", amount = -(i % 9000).toLong() - 1, memo = if (i % 7 == 0) "note $i" else null))
                    db.exec("INSERT INTO txn_split(id, txn_id, category_id, amount_minor) VALUES ('s$i', 't$i', 'c${i % 40}', ${-(i % 9000) - 1})")
                }
            }
        }
        val plainDb = ledger("plain.db", capture = false)
        val plain = load(plainDb)
        plainDb.close()
        val captured = ledger("captured.db")
        val withCapture = load(captured)
        val bytes = captured.long("SELECT sum(length(tbl) + length(pk) + length(coalesce(cols, ''))) FROM sync_outbox")
        val changes = captured.long("SELECT count(*) FROM sync_outbox")
        println("$n transactions + splits: without capture $plain, with capture $withCapture; $changes changes, ${bytes / changes} bytes each before compression")
        assertEquals(2L * n + 1, changes)
        captured.close()
    }

    @Test
    fun `the hybrid logical clock moves forward even when the computer clock goes back`() {
        var wall = 1_000_000L
        val clock = Hlc("dev-a") { wall }
        val first = clock.now()
        wall -= 60_000 // the clock is set back a minute
        val second = clock.now()
        val third = clock.now()
        val received = Hlc.parse(clock.receive("0000001005000-00000-dev-b"))
        val fourth = clock.now()
        println(listOf(first, second, third, fourth))
        assertTrue(first < second && second < third && third < fourth)
        assertTrue(received.millis == 1_005_000L && fourth > "0000001005000-00000-dev-b")
        assertEquals(listOf(first, second, third, fourth).sorted(), listOf(first, second, third, fourth))
    }

    // --- The capture prototype (phase 2 rebuilds this properly in core/replication) ---

    data class Column(val name: String, val type: String, val pk: Int) {
        val isBlob get() = type.equals("BLOB", ignoreCase = true)
        fun value(row: String) = if (isBlob) "hex($row.\"$name\")" else "$row.\"$name\""
    }

    object Capture {
        fun tables(d: SqlDriver) = d.strings(
            "SELECT name FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%' AND name NOT LIKE 'sync_%' ORDER BY name",
        )

        fun columns(d: SqlDriver, table: String): List<Column> = d.executeQuery(null, "PRAGMA table_info(\"$table\")", { c ->
            val out = ArrayList<Column>()
            while (c.next().value) out += Column(c.getString(1)!!, c.getString(2).orEmpty(), c.getLong(5)!!.toInt())
            QueryResult.Value(out)
        }, 0).value

        private fun keys(cols: List<Column>) = cols.filter { it.pk > 0 }.sortedBy { it.pk }

        fun install(d: SqlDriver) {
            d.exec("CREATE TABLE IF NOT EXISTS sync_outbox(seq INTEGER PRIMARY KEY, tbl TEXT NOT NULL, pk TEXT NOT NULL, kind TEXT NOT NULL, cols TEXT)")
            d.exec("CREATE TABLE IF NOT EXISTS sync_apply(flag INTEGER)")
            for (t in tables(d)) {
                val cols = columns(d, t)
                fun pk(row: String) = "json_array(${keys(cols).joinToString { it.value(row) }})"
                val all = cols.joinToString(" UNION ALL ") { "SELECT '${it.name}' AS k, ${it.value("NEW")} AS v" }
                val changed = cols.joinToString(" UNION ALL ") {
                    "SELECT '${it.name}' AS k, ${it.value("NEW")} AS v, OLD.\"${it.name}\" IS NOT NEW.\"${it.name}\" AS ch"
                }
                val anyChange = cols.joinToString(" OR ") { "OLD.\"${it.name}\" IS NOT NEW.\"${it.name}\"" }
                val idle = "NOT EXISTS (SELECT 1 FROM sync_apply)"
                d.exec(
                    "CREATE TRIGGER \"sync_i_$t\" AFTER INSERT ON \"$t\" WHEN $idle BEGIN " +
                        "INSERT INTO sync_outbox(tbl, pk, kind, cols) VALUES ('$t', ${pk("NEW")}, 'I', (SELECT json_group_object(k, v) FROM ($all))); END",
                )
                d.exec(
                    "CREATE TRIGGER \"sync_u_$t\" AFTER UPDATE ON \"$t\" WHEN $idle AND ($anyChange) BEGIN " +
                        "INSERT INTO sync_outbox(tbl, pk, kind, cols) VALUES ('$t', ${pk("NEW")}, 'U', (SELECT json_group_object(k, v) FROM ($changed) WHERE ch)); END",
                )
                d.exec(
                    "CREATE TRIGGER \"sync_d_$t\" AFTER DELETE ON \"$t\" WHEN $idle BEGIN " +
                        "INSERT INTO sync_outbox(tbl, pk, kind) VALUES ('$t', ${pk("OLD")}, 'D'); END",
                )
            }
        }

        /** Applies [from]'s outbox to [into] in order, using only SQL generated from the schema. */
        fun replay(from: SqlDriver, into: SqlDriver) {
            val ops = from.executeQuery(null, "SELECT tbl, pk, kind, cols FROM sync_outbox ORDER BY seq", { c ->
                val out = ArrayList<List<String?>>()
                while (c.next().value) out += listOf(c.getString(0), c.getString(1), c.getString(2), c.getString(3))
                QueryResult.Value(out)
            }, 0).value
            into.inTransaction {
                for ((tbl, pk, kind, cols) in ops) {
                    val columns = columns(into, tbl!!)
                    val key = keys(columns)
                    fun field(c: Column, json: Int) =
                        if (c.isBlob) "unhex(json_extract(?$json, '$.\"${c.name}\"'))" else "json_extract(?$json, '$.\"${c.name}\"')"
                    val where = key.withIndex().joinToString(" AND ") { (i, c) -> "\"${c.name}\" = json_extract(?2, '$[$i]')" }
                    val sql = when (kind) {
                        "I" -> "INSERT INTO \"$tbl\"(${columns.joinToString { "\"${it.name}\"" }}) VALUES (${columns.joinToString { field(it, 1) }})"
                        "U" -> "UPDATE \"$tbl\" SET ${columns.joinToString { "\"${it.name}\" = CASE WHEN json_type(?1, '$.\"${it.name}\"') IS NULL THEN \"${it.name}\" ELSE ${field(it, 1)} END" }} WHERE $where"
                        else -> "DELETE FROM \"$tbl\" WHERE $where"
                    }
                    when (kind) {
                        "I" -> into.execute(null, sql, 1) { bindString(0, cols) }
                        "U" -> into.execute(null, sql, 2) { bindString(0, cols); bindString(1, pk) }
                        // The DELETE has only ?2: SQLite numbers the parameters up to the highest used.
                        else -> into.execute(null, sql, 2) { bindString(0, "{}"); bindString(1, pk) }
                    }
                }
            }
        }

        fun dump(d: SqlDriver, table: String): List<String> {
            val cols = columns(d, table)
            val order = keys(cols).joinToString { "\"${it.name}\"" }
            return d.strings("SELECT json_array(${cols.joinToString { it.value(table) }}) FROM \"$table\" ORDER BY $order")
        }
    }

    /** Hybrid logical clock: fixed-width "millis-counter-device", ordered as text (plan §4.2). */
    class Hlc(private val device: String, private val wall: () -> Long) {
        private var millis = 0L
        private var counter = 0

        @Synchronized
        fun now(): String {
            val w = wall()
            if (w > millis) { millis = w; counter = 0 } else counter++
            return format(millis, counter, device)
        }

        /** Moves this clock past a clock value received from another computer. */
        @Synchronized
        fun receive(remote: String): String {
            val r = parse(remote)
            val w = wall()
            val top = maxOf(w, millis, r.millis)
            counter = if (top == millis && top == r.millis) maxOf(counter, r.counter) + 1
            else if (top == millis) counter + 1
            else if (top == r.millis) r.counter + 1
            else 0
            millis = top
            return format(millis, counter, device)
        }

        data class Parsed(val millis: Long, val counter: Int, val device: String)

        companion object {
            fun format(millis: Long, counter: Int, device: String) = "%013d-%05d-%s".format(millis, counter, device)
            fun parse(value: String) = value.split('-', limit = 3).let { Parsed(it[0].toLong(), it[1].toInt(), it[2]) }
        }
    }

    private companion object {
        const val ACCOUNT = "INSERT INTO account(id, name, type, currency, opening_date, created_at, updated_at) VALUES ('a1', 'Chequing', 'CHEQUING', 'CAD', '2026-01-01', 1, 1)"

        fun txn(id: String, amount: Long = -1500, memo: String? = null) =
            "INSERT INTO txn(id, account_id, date, payee_text, amount_minor, memo, created_at, updated_at) VALUES " +
                "('$id', 'a1', '2026-03-01', 'IGA', $amount, ${memo?.let { "'$it'" } ?: "NULL"}, 1, 1)"
    }
}

private fun SqlDriver.exec(sql: String) {
    execute(null, sql, 0)
}

private fun SqlDriver.strings(sql: String): List<String> = executeQuery(null, sql, { c ->
    val out = ArrayList<String>()
    while (c.next().value) out += c.getString(0).orEmpty()
    QueryResult.Value(out)
}, 0).value

private fun SqlDriver.long(sql: String): Long = executeQuery(null, sql, { c -> c.next(); QueryResult.Value(c.getLong(0) ?: 0) }, 0).value

private fun SqlDriver.inTransaction(block: () -> Unit) {
    object : TransacterImpl(this) {}.transaction { block() }
}
