// File path and name: core/replication/src/test/kotlin/ca/schippers/hfm/replication/UniqueResolutionsTest.kt
// Modified On Timestamp: 2026-10-26 @ 03:48 EDT
// Created On Timestamp: 2026-10-26 @ 03:48 EDT
// File Description: Checks UniqueResolutions against every UNIQUE constraint of the real schemas (plan-multi-device 4.5, 10).
// Uses: CoreDatabase and LedgerDatabase schemas (core/data), the SQLCipher driver (core/data-jdbc).
// Used By: ./gradlew :core:replication:test.
// Purpose: A new UNIQUE constraint without a resolution fails the build.
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
import kotlin.test.fail

class UniqueResolutionsTest {

    @TempDir
    lateinit var temp: Path

    /** Every unique constraint or index that is not the primary key, as table and ordered columns. */
    private fun uniques(database: Database): Set<Pair<String, List<String>>> {
        SqlCipherJdbcDriverFactory().open(temp.resolve("${database.name}.db"), Random.key()).use { d ->
            when (database) {
                Database.CORE -> CoreDatabase.Schema.create(d)
                Database.LEDGER -> LedgerDatabase.Schema.create(d)
            }
            val tables = rows(d, "SELECT name FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%'").map { it[0] }
            return tables.flatMap { table ->
                rows(d, "SELECT name, \"unique\", origin FROM pragma_index_list('$table')", columns = 3)
                    .filter { it[1] == "1" && it[2] != "pk" }
                    .map { index -> table to rows(d, "SELECT name FROM pragma_index_info('${index[0]}') ORDER BY seqno").map { it[0] } }
            }.toSet()
        }
    }

    private fun rows(driver: SqlDriver, sql: String, columns: Int = 1): List<List<String>> = driver.executeQuery(null, sql, { c ->
        val out = ArrayList<List<String>>()
        while (c.next().value) out += (0 until columns).map { c.getString(it) ?: "" }
        QueryResult.Value(out)
    }, 0).value

    @Test
    fun `every unique constraint of both schemas has a resolution, and none names a missing one`() {
        val problems = ArrayList<String>()
        for (database in Database.entries) {
            val found = uniques(database)
            for ((table, columns) in found.sortedBy { "${it.first}(${it.second})" }) {
                if (UniqueResolutions.of(database, table, columns) == null) problems += "missing: $database.$table(${columns.joinToString(",")})"
            }
            for (rule in UniqueResolutions.rules.filter { it.database == database }) {
                if ((rule.table to rule.columns) !in found) problems += "not in the schema: ${rule.key}"
            }
        }
        if (problems.isNotEmpty()) fail(problems.joinToString("\n"))
    }

    @Test
    fun `local-only tables are settled locally, and only they are`() {
        for (rule in UniqueResolutions.rules) {
            val local = PolicyManifest.of(rule.database, rule.table)?.policy == Policy.LOCAL_ONLY
            assertEquals(local, rule.resolution == Resolution.LOCAL, rule.key)
        }
    }
}
