// File path and name: core/replication/src/test/kotlin/ca/schippers/hfm/replication/NoReplaceTest.kt
// Modified On Timestamp: 2026-10-26 @ 04:00 EDT
// Created On Timestamp: 2026-10-26 @ 04:00 EDT
// File Description: No query saves with INSERT OR REPLACE, apart from the few listed here.
// Uses: The SQLDelight .sq files of core/data.
// Used By: ./gradlew :core:replication:test.
// Purpose: A replace rewrites every column (and deletes children), so a change cannot be merged field by field.
package ca.schippers.hfm.replication

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.name
import kotlin.io.path.readLines
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail

class NoReplaceTest {

    /**
     * Queries that still replace, each on a table with a second unique key, where a replace also
     * removes the clashing row: they need their own decision before change capture (phase 2).
     * putLocalSetting is on a local-only table.
     */
    private val allowed = setOf("putLocalSetting", "insertInvTxn", "upsertCardStatement", "upsertRoom", "upsertSlip")

    @Test
    fun `queries update in place instead of INSERT OR REPLACE`() {
        val root = Path.of("../data/src/main/sqldelight")
        assertTrue(Files.isDirectory(root), "run from core/replication")
        val found = ArrayList<String>()
        Files.walk(root).use { paths ->
            for (file in paths.filter { it.extension == "sq" }.sorted().toList()) {
                var query = ""
                for (line in file.readLines()) {
                    Regex("^(\\w+):\\s*$").find(line)?.let { query = it.groupValues[1] }
                    if (line.trimStart().startsWith("INSERT OR REPLACE", ignoreCase = true)) found += "${file.name}: $query"
                }
            }
        }
        val unexpected = found.filter { it.substringAfter(": ") !in allowed }
        if (unexpected.isNotEmpty()) fail("Use INSERT ... ON CONFLICT(key) DO UPDATE instead:\n" + unexpected.joinToString("\n"))
        assertTrue(found.size == allowed.size, "an allowed query no longer replaces: remove it from the list ($found)")
    }
}
