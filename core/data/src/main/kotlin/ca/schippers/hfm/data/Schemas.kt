// File path and name: core/data/src/main/kotlin/ca/schippers/hfm/data/Schemas.kt
// Modified On Timestamp: 2026-10-26 @ 04:09 EDT
// Created On Timestamp: 2026-10-01 @ 15:53 EDT
// File Description: Opens encrypted databases and creates or upgrades their schema (NFR-11), with a copy before each upgrade.
// Uses: SQLDelight schemas of core/data; the platform's EncryptedDriverFactory.
// Used By: HouseholdSession (every database it opens), MigrationTest, core/replication.
// Purpose: Households made by any earlier version open safely in this one.
package ca.schippers.hfm.data

import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** Opens an encrypted SQLite database file with a raw 256-bit key. Implemented per platform. */
fun interface EncryptedDriverFactory {
    /** @throws WrongKeyException if the key does not decrypt the file. */
    fun open(file: Path, key: ByteArray): SqlDriver
}

class WrongKeyException(file: Path, cause: Throwable? = null) : Exception("Cannot decrypt $file", cause)

/**
 * Creates or upgrades a database to the schema version this build expects (NFR-11).
 * Before any upgrade the database file is copied to `backups/pre-upgrade/`, so a failed or
 * unwanted upgrade can always be undone.
 */
object SchemaManager {

    fun prepare(driver: SqlDriver, schema: SqlSchema<QueryResult.Value<Unit>>, file: Path, now: LocalDateTime = LocalDateTime.now()) {
        val current = userVersion(driver)
        when {
            current == 0L -> driver.transaction {
                schema.create(driver)
                setUserVersion(driver, schema.version)
            }
            current < schema.version -> {
                // Fold the write-ahead log into the main file so the copy is complete.
                driver.executeQuery(null, "PRAGMA wal_checkpoint(TRUNCATE)", { QueryResult.Value(Unit) }, 0)
                backupBeforeUpgrade(file, current, now)
                driver.transaction {
                    // A migration is never sent to other computers: each runs it itself (plan-multi-device
                    // 5.8). The change-capture triggers go first; they are made again for the new schema.
                    dropCaptureTriggers(driver)
                    schema.migrate(driver, current, schema.version)
                    setUserVersion(driver, schema.version)
                }
            }
            current > schema.version -> throw NewerFormatException(current.toInt(), schema.version.toInt())
        }
    }

    fun userVersion(driver: SqlDriver): Long =
        driver.executeQuery(null, "PRAGMA user_version", { cursor ->
            cursor.next()
            QueryResult.Value(cursor.getLong(0) ?: 0L)
        }, 0).value

    /** Removes every change-capture trigger (named `repl_...`, made by core/replication). */
    fun dropCaptureTriggers(driver: SqlDriver) {
        val names = driver.executeQuery(null, "SELECT name FROM sqlite_master WHERE type = 'trigger' AND name LIKE 'repl\\_%' ESCAPE '\\'", { c ->
            val out = ArrayList<String>()
            while (c.next().value) out += c.getString(0)!!
            QueryResult.Value(out)
        }, 0).value
        for (name in names) driver.execute(null, "DROP TRIGGER \"$name\"", 0)
    }

    private fun setUserVersion(driver: SqlDriver, version: Long) {
        driver.execute(null, "PRAGMA user_version = $version", 0)
    }

    internal fun backupBeforeUpgrade(file: Path, fromVersion: Long, now: LocalDateTime): Path {
        val dir = file.parent.resolve("backups").resolve("pre-upgrade")
        Files.createDirectories(dir)
        val stamp = now.format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
        val target = dir.resolve("${file.fileName}.v$fromVersion.$stamp")
        Files.copy(file, target)
        return target
    }

    private fun SqlDriver.transaction(block: () -> Unit) {
        object : TransacterImpl(this) {}.transaction { block() }
    }
}
