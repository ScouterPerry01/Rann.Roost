// File path and name: core/data/src/main/kotlin/ca/schippers/hfm/data/DeviceIdentity.kt
// Modified On Timestamp: 2026-10-26 @ 04:45 EDT
// Created On Timestamp: 2026-10-26 @ 03:16 EDT
// File Description: device.json: this computer's own identity for one household (plan-multi-device 5.1).
// Uses: kotlinx.serialization; Ids; the same atomic write as household.json (HeaderFile).
// Used By: HouseholdStore (created when a household is created or first opened), HouseholdSession.device.
// Purpose: Give each copy of a household an author id for its changes, never synced and never backed up.
package ca.schippers.hfm.data

import ca.schippers.hfm.domain.Ids
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

/**
 * `device.json`, next to `household.json`: which computer this copy of the household is on. It is
 * never synced and never backed up (backups take only `household.json`, the databases and the
 * vault), so a restored backup gets a new [deviceId] and change numbers are never reused.
 */
@Serializable
data class DeviceIdentity(
    val format: Int = CURRENT_FORMAT,
    val deviceId: String,
    /** The computer's name when the file was made, shown to the other computers. */
    val name: String,
    val createdAt: Long,
    /**
     * The last change-set number this computer gave in each partition (plan-multi-device 5.1): the
     * next one is one more, with no gaps. Kept here, so a restored backup (a new computer) starts again at 1.
     */
    val lastSequence: Map<String, Long> = emptyMap(),
) {
    companion object {
        const val CURRENT_FORMAT = 1
        const val FILE_NAME = "device.json"
    }
}

internal object DeviceFile {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = false
        encodeDefaults = true
    }

    /**
     * This computer's identity in [dir], made on first use. A file that cannot be read is kept as
     * `device.json.bad` and replaced: a new identity only means a new computer to the others.
     */
    fun readOrCreate(dir: Path, now: Long): DeviceIdentity {
        val file = dir.resolve(DeviceIdentity.FILE_NAME)
        if (Files.exists(file)) {
            val read = runCatching { json.decodeFromString(DeviceIdentity.serializer(), Files.readString(file)) }.getOrNull()
            if (read != null) {
                if (read.format > DeviceIdentity.CURRENT_FORMAT) throw NewerFormatException(read.format, DeviceIdentity.CURRENT_FORMAT)
                if (Ids.isValid(read.deviceId)) return read
            }
            Files.move(file, dir.resolve("${DeviceIdentity.FILE_NAME}.bad"), StandardCopyOption.REPLACE_EXISTING)
        }
        return DeviceIdentity(deviceId = Ids.newId(), name = computerName(), createdAt = now).also { write(dir, it) }
    }

    /** Gives the next change-set number of [partitionId] and saves it before returning, so no number is given twice. */
    @Synchronized
    fun nextSequence(dir: Path, partitionId: String, now: Long): Long {
        val device = readOrCreate(dir, now)
        val next = (device.lastSequence[partitionId] ?: 0L) + 1
        write(dir, device.copy(lastSequence = device.lastSequence + (partitionId to next)))
        return next
    }

    private fun write(dir: Path, device: DeviceIdentity) {
        val temp = dir.resolve("${DeviceIdentity.FILE_NAME}.tmp")
        Files.writeString(temp, json.encodeToString(DeviceIdentity.serializer(), device))
        Files.move(temp, dir.resolve(DeviceIdentity.FILE_NAME), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
    }

    private fun computerName(): String =
        (System.getenv("COMPUTERNAME") ?: System.getenv("HOSTNAME"))?.trim()?.takeIf { it.isNotEmpty() }?.take(80)
            ?: runCatching { java.net.InetAddress.getLocalHost().hostName }.getOrNull()?.take(80)
            ?: "Computer"
}
