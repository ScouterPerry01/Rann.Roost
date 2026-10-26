// File path and name: app/desktop/src/test/kotlin/ca/schippers/hfm/desktop/CaptureCompletenessTest.kt
// Modified On Timestamp: 2026-10-26 @ 04:19 EDT
// Created On Timestamp: 2026-10-26 @ 04:19 EDT
// File Description: Capture completeness (plan-multi-device 10): the sample households are built with change
//   capture on, and replaying what was captured into empty databases gives identical tables.
// Uses: SampleHousehold, core/replication (CaptureTriggers, PolicyManifest), the SQLCipher driver.
// Used By: ./gradlew :app:desktop:test (single person); :app:desktop:sampleHouseholdsTest (family).
// Purpose: Prove that every write the app makes, through any service, is captured (phase 2 exit check).
package ca.schippers.hfm.desktop

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import ca.schippers.hfm.calc.Province
import ca.schippers.hfm.data.HouseholdStore
import ca.schippers.hfm.data.PartitionKind
import ca.schippers.hfm.data.core.CoreDatabase
import ca.schippers.hfm.data.jdbc.SqlCipherJdbcDriverFactory
import ca.schippers.hfm.data.ledger.LedgerDatabase
import ca.schippers.hfm.i18n.Language
import ca.schippers.hfm.replication.CaptureTriggers
import ca.schippers.hfm.replication.Database
import ca.schippers.hfm.replication.Policy
import ca.schippers.hfm.replication.PolicyManifest
import ca.schippers.hfm.security.KdfParams
import ca.schippers.hfm.security.Random
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CaptureCompletenessTest {

    @TempDir
    lateinit var temp: Path

    /** About a minute and a half with capture on: run with the other sample checks (sampleHouseholdsTest). */
    @Test
    @Tag("samples")
    fun `everything the family sample household writes is captured and replays identically`() = check(SampleHousehold.FAMILY, Language.FRENCH, Province.QC)

    @Test
    fun `everything the single-person sample household writes is captured and replays identically`() = check(SampleHousehold.SINGLE, Language.ENGLISH, Province.ON)

    private fun check(kind: SampleHousehold, language: Language, province: Province) {
        val opened = ArrayList<Pair<SqlDriver, Database>>()
        val store = HouseholdStore(SqlCipherJdbcDriverFactory(), KdfParams.TESTING, onDatabaseOpened = { driver, partition ->
            val database = if (partition == PartitionKind.CORE) Database.CORE else Database.LEDGER
            CaptureTriggers.install(driver, database)
            opened += driver to database
        })
        kind.create(store, language, province).use {
            assertTrue(opened.size >= 2, "the core database and at least one ledger")
            var compared = 0
            for ((i, pair) in opened.withIndex()) {
                val (source, database) = pair
                val copy = SqlCipherJdbcDriverFactory().open(temp.resolve("copy-$i.db"), Random.key())
                copy.use {
                    when (database) {
                        Database.CORE -> CoreDatabase.Schema.create(copy)
                        Database.LEDGER -> LedgerDatabase.Schema.create(copy)
                    }
                    replay(source, copy)
                    val synced = PolicyManifest.tables.filter { it.database == database && it.policy != Policy.LOCAL_ONLY }.map { it.table }
                    for (table in synced) {
                        assertEquals(dump(source, table), dump(copy, table), "$kind: $database.$table")
                        if (count(source, table) > 0) compared++
                    }
                }
            }
            assertTrue(compared > 40, "$kind: only $compared tables held data")
        }
    }

    /** Applies the outbox of [from] to [into] in order, with SQL made from the schema only. */
    private fun replay(from: SqlDriver, into: SqlDriver) {
        val ops = from.executeQuery(null, "SELECT tbl, pk, kind, cols FROM repl_outbox ORDER BY seq", { c ->
            val out = ArrayList<List<String?>>()
            while (c.next().value) out += listOf(c.getString(0), c.getString(1), c.getString(2), c.getString(3))
            QueryResult.Value(out)
        }, 0).value
        into.execute(null, "PRAGMA foreign_keys = OFF", 0) // rows arrive in the order they were made; checks are not the point here
        for ((tbl, pk, kind, cols) in ops) {
            val columns = CaptureTriggers.columns(into, tbl!!)
            fun field(name: String, blob: Boolean, json: Int) =
                if (blob) "unhex(json_extract(?$json, '$.\"$name\"'))" else "json_extract(?$json, '$.\"$name\"')"
            val where = CaptureTriggers.keys(columns).joinToString(" AND ") { "\"${it.name}\" = ${field(it.name, it.isBlob, 2)}" }
            when (kind) {
                "I" -> into.execute(null, "INSERT INTO \"$tbl\"(${columns.joinToString { "\"${it.name}\"" }}) VALUES (${columns.joinToString { field(it.name, it.isBlob, 1) }})", 1) { bindString(0, cols) }
                "U" -> into.execute(
                    null,
                    "UPDATE \"$tbl\" SET ${columns.joinToString { "\"${it.name}\" = CASE WHEN json_type(?1, '$.\"${it.name}\"') IS NULL THEN \"${it.name}\" ELSE ${field(it.name, it.isBlob, 1)} END" }} WHERE $where",
                    2,
                ) { bindString(0, cols); bindString(1, pk) }
                else -> into.execute(null, "DELETE FROM \"$tbl\" WHERE $where AND ?1 IS NOT NULL", 2) { bindString(0, "{}"); bindString(1, pk) }
            }
        }
    }

    private fun dump(d: SqlDriver, table: String): List<String> {
        val columns = CaptureTriggers.columns(d, table)
        val order = CaptureTriggers.keys(columns).joinToString { "\"${it.name}\"" }
        return d.executeQuery(null, "SELECT json_array(${columns.joinToString { it.sql("\"$table\"") }}) FROM \"$table\" ORDER BY $order", { c ->
            val out = ArrayList<String>()
            while (c.next().value) out += c.getString(0)!!
            QueryResult.Value(out)
        }, 0).value
    }

    private fun count(d: SqlDriver, table: String): Long =
        d.executeQuery(null, "SELECT count(*) FROM \"$table\"", { c -> c.next(); QueryResult.Value(c.getLong(0)!!) }, 0).value
}
