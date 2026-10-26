// File path and name: core/replication/src/test/kotlin/ca/schippers/hfm/replication/PolicyManifestTest.kt
// Modified On Timestamp: 2026-10-26 @ 04:20 EDT
// Created On Timestamp: 2026-10-26 @ 03:45 EDT
// File Description: Checks PolicyManifest against the real core and ledger schemas (plan-multi-device 4.3, 10).
// Uses: CoreDatabase and LedgerDatabase schemas (core/data), the SQLCipher driver (core/data-jdbc).
// Used By: ./gradlew :core:replication:test.
// Purpose: A new table without a merge policy fails the build, as a text without a translation does.
package ca.schippers.hfm.replication

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import ca.schippers.hfm.data.core.CoreDatabase
import ca.schippers.hfm.data.jdbc.SqlCipherJdbcDriverFactory
import ca.schippers.hfm.data.ledger.LedgerDatabase
import ca.schippers.hfm.security.Random
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class PolicyManifestTest {

    @TempDir
    lateinit var temp: Path

    /** Every table and its columns, from a database made with the current schema. */
    private fun schema(database: Database): Map<String, Set<String>> {
        val driver = SqlCipherJdbcDriverFactory().open(temp.resolve("${database.name}.db"), Random.key())
        driver.use {
            when (database) {
                Database.CORE -> CoreDatabase.Schema.create(it)
                Database.LEDGER -> LedgerDatabase.Schema.create(it)
            }
            return strings(it, "SELECT name FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%'")
                .associateWith { table -> strings(it, "SELECT name FROM pragma_table_info('$table')").toSet() }
        }
    }

    private fun strings(driver: SqlDriver, sql: String): List<String> = driver.executeQuery(null, sql, { c ->
        val out = ArrayList<String>()
        while (c.next().value) out += c.getString(0)!!
        QueryResult.Value(out)
    }, 0).value

    @Test
    fun `every table of both schemas has exactly one policy, and no policy names a missing table`() {
        val problems = ArrayList<String>()
        for (database in Database.entries) {
            val tables = schema(database).keys
            val listed = PolicyManifest.tables.filter { it.database == database }.map { it.table }
            listed.groupBy { it }.filterValues { it.size > 1 }.keys.forEach { problems += "$database.$it is listed twice" }
            (tables - listed.toSet()).sorted().forEach { problems += "$database.$it has no policy: add it to PolicyManifest" }
            (listed.toSet() - tables).sorted().forEach { problems += "$database.$it is in PolicyManifest but not in the schema" }
        }
        if (problems.isNotEmpty()) fail(problems.joinToString("\n"))
    }

    @Test
    fun `parents, counters and cursor columns name real tables and columns`() {
        val schemas = Database.entries.associateWith { schema(it) }
        for (p in PolicyManifest.tables) {
            val columns = schemas.getValue(p.database)[p.table] ?: continue
            if (p.policy == Policy.AGGREGATE) {
                val parent = p.parent ?: fail("${p.table}: an aggregate needs its parent")
                assertTrue(parent in schemas.getValue(p.database), "${p.table}: parent $parent is not a ${p.database} table")
            } else {
                assertEquals(null, p.parent, "${p.table}: only an aggregate has a parent")
            }
            if (p.policy == Policy.COUNTER) assertTrue(p.counterColumns.isNotEmpty(), "${p.table}: a counter table names its counters")
            for (c in p.counterColumns + p.maxColumns) assertTrue(c in columns, "${p.table}.$c does not exist")
        }
    }

    @Test
    fun `local-only tables are exactly the phone pairing, this computer's settings and the capture bookkeeping`() {
        // plan-multi-device 4.1, 5.1 and 7. A new local-only table is a decision: add it here on purpose.
        assertEquals(
            setOf("CORE.sync_device", "CORE.sync_item", "CORE.repl_local_setting", "CORE.repl_outbox", "CORE.repl_applying", "LEDGER.repl_outbox", "LEDGER.repl_applying"),
            PolicyManifest.tables.filter { it.policy == Policy.LOCAL_ONLY }.map { "${it.database}.${it.table}" }.toSet(),
        )
    }

    @Test
    fun `every bookkeeping table starts with repl_ and is local only`() {
        // The spike's lesson (ADR 0011): capture is driven by the manifest, never by a name filter.
        for (p in PolicyManifest.tables.filter { it.table.startsWith("repl_") }) assertEquals(Policy.LOCAL_ONLY, p.policy, p.table)
    }
}
