// File path and name: core/replication/src/main/kotlin/ca/schippers/hfm/replication/ChangeSetFile.kt
// Modified On Timestamp: 2026-10-26 @ 04:36 EDT
// Created On Timestamp: 2026-10-26 @ 04:36 EDT
// File Description: The change-set file shared by every transport: sealed with the group's key and signed by its author (plan-multi-device 5.2).
// Uses: core/security (Aead, Hkdf, Signatures); kotlinx.serialization JSON; CapturedChange, HlcStamp.
// Used By: The exchange folder (phase 4) and home Wi-Fi (phase 6); ChangeSetFileTest.
// Purpose: Carry one transaction's changes to other computers so only holders of the group's key can read them and nobody can forge them.
package ca.schippers.hfm.replication

import ca.schippers.hfm.security.Aead
import ca.schippers.hfm.security.DecryptionException
import ca.schippers.hfm.security.Hkdf
import ca.schippers.hfm.security.Signatures
import ca.schippers.hfm.security.SigningKeyPair
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/** The plain part of a change set: enough to file and route it, nothing about the household's money. */
@Serializable
data class ChangeSetHeader(
    val format: Int = ChangeSetFile.FORMAT,
    val householdId: String,
    /** The database the changes belong to: the core database or one group's ledger. */
    val partitionId: String,
    val deviceId: String,
    val userId: String,
    /** This computer's number for the change set in this partition: 1, 2, 3... with no gaps. */
    val seq: Long,
    val firstHlc: String,
    val lastHlc: String,
    /** Schema versions of the author's databases, so a computer holds changes newer than it understands (5.8). */
    val coreVersion: Long,
    val ledgerVersion: Long,
)

/** One captured change, as carried in a change set. */
@Serializable
data class Change(val table: String, val pk: JsonObject, val kind: String, val cols: JsonObject? = null, val hlc: String)

data class ChangeSet(val header: ChangeSetHeader, val changes: List<Change>) {
    companion object {
        /** The change set of one sealed transaction ([captured], from Outbox.seal). */
        fun of(
            captured: List<CapturedChange>,
            householdId: String,
            partitionId: String,
            deviceId: String,
            seq: Long,
            coreVersion: Long,
            ledgerVersion: Long,
        ): ChangeSet {
            require(captured.isNotEmpty()) { "An empty change set" }
            val userId = captured.map { it.userId }.distinct().singleOrNull() ?: throw IllegalArgumentException("One author per change set")
            val changes = captured.sortedBy { it.hlc }.map {
                Change(it.table, Json.parseToJsonElement(it.pk).jsonObject, it.kind, it.cols?.let { c -> Json.parseToJsonElement(c).jsonObject }, it.hlc.toString())
            }
            return ChangeSet(
                ChangeSetHeader(
                    householdId = householdId, partitionId = partitionId, deviceId = deviceId, userId = userId, seq = seq,
                    firstHlc = changes.first().hlc, lastHlc = changes.last().hlc, coreVersion = coreVersion, ledgerVersion = ledgerVersion,
                ),
                changes,
            )
        }
    }
}

/** A change-set file that must not be used: damaged, forged, renamed, or for another household or group. */
class ChangeSetException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * `<seq>.roostchg`: the line `ROOSTCHG1`, a one-line JSON [ChangeSetHeader], the body (the changes as
 * gzip JSON, sealed with AES-256-GCM under a key derived from the partition key, bound to the
 * header's household, partition, device, user and number), then the author's 64-byte Ed25519
 * signature of everything before it.
 */
object ChangeSetFile {
    const val MAGIC = "ROOSTCHG1"
    const val FORMAT = 1
    const val EXTENSION = "roostchg"

    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = false }

    fun fileName(seq: Long): String = "$seq.$EXTENSION"

    fun write(set: ChangeSet, partitionKey: ByteArray, author: SigningKeyPair): ByteArray {
        val h = set.header
        require(set.changes.isNotEmpty() && h.firstHlc == set.changes.first().hlc && h.lastHlc == set.changes.last().hlc) { "Header does not match the changes" }
        val head = "$MAGIC\n${json.encodeToString(ChangeSetHeader.serializer(), h)}\n".encodeToByteArray()
        val body = Aead.seal(bodyKey(partitionKey, h), gzip(json.encodeToString(ListSerializer(Change.serializer()), set.changes).encodeToByteArray()), aad(h))
        val signed = head + body
        return signed + Signatures.sign(author.privateKey, signed)
    }

    /** Reads only the plain header, without any key: to file the change set or skip it. */
    fun header(file: ByteArray): ChangeSetHeader {
        val (header, _) = split(file)
        return header
    }

    /**
     * Opens a change set after checking, in order: the format, where it was found ([expectedDevice],
     * [expectedSeq], so a renamed or moved file is refused), the author's signature with the key
     * [signerKey] gives for the header's user (null: unknown user), and the sealed body.
     */
    fun read(
        file: ByteArray,
        partitionKey: ByteArray,
        signerKey: (userId: String) -> ByteArray?,
        expectedDevice: String? = null,
        expectedSeq: Long? = null,
    ): ChangeSet {
        val (header, bodyStart) = split(file)
        if (expectedDevice != null && header.deviceId != expectedDevice) throw ChangeSetException("Found under another computer's folder")
        if (expectedSeq != null && header.seq != expectedSeq) throw ChangeSetException("Found under another number")
        val signedEnd = file.size - Signatures.SIGNATURE_BYTES
        val publicKey = signerKey(header.userId) ?: throw ChangeSetException("Signed by an unknown user")
        if (!Signatures.verify(publicKey, file.copyOfRange(0, signedEnd), file.copyOfRange(signedEnd, file.size))) {
            throw ChangeSetException("The signature does not match: the file was changed or not made by this user")
        }
        val plain = try {
            Aead.open(bodyKey(partitionKey, header), file.copyOfRange(bodyStart, signedEnd), aad(header))
        } catch (e: DecryptionException) {
            throw ChangeSetException("The changes cannot be opened with this group's key", e)
        }
        val changes = try {
            json.decodeFromString(ListSerializer(Change.serializer()), gunzip(plain).decodeToString())
        } catch (e: Exception) {
            throw ChangeSetException("The changes are not readable", e)
        }
        if (changes.isEmpty() || changes.first().hlc != header.firstHlc || changes.last().hlc != header.lastHlc) throw ChangeSetException("The changes do not match the header")
        for (c in changes) {
            val stamp = runCatching { HlcStamp.parse(c.hlc) }.getOrNull() ?: throw ChangeSetException("A change has no valid clock value")
            if (stamp.device != header.deviceId) throw ChangeSetException("A change was stamped by another computer")
        }
        return ChangeSet(header, changes)
    }

    private fun split(file: ByteArray): Pair<ChangeSetHeader, Int> {
        val magicEnd = MAGIC.length
        if (file.size < magicEnd + 2 + Signatures.SIGNATURE_BYTES || file.copyOfRange(0, magicEnd).decodeToString() != MAGIC || file[magicEnd] != '\n'.code.toByte()) {
            throw ChangeSetException("Not a change-set file")
        }
        val headerEnd = (magicEnd + 1 until file.size).firstOrNull { file[it] == '\n'.code.toByte() } ?: throw ChangeSetException("The header is incomplete")
        val header = try {
            json.decodeFromString(ChangeSetHeader.serializer(), file.copyOfRange(magicEnd + 1, headerEnd).decodeToString())
        } catch (e: Exception) {
            throw ChangeSetException("The header is not readable", e)
        }
        if (header.format > FORMAT) throw ChangeSetException("Made by a newer version (format ${header.format})")
        if (headerEnd + 1 > file.size - Signatures.SIGNATURE_BYTES) throw ChangeSetException("The file is incomplete")
        return header to headerEnd + 1
    }

    private fun bodyKey(partitionKey: ByteArray, h: ChangeSetHeader): ByteArray =
        Hkdf.derive(partitionKey, h.householdId.encodeToByteArray(), "hfm-changes-v1")

    private fun aad(h: ChangeSetHeader): ByteArray =
        listOf(MAGIC, h.householdId, h.partitionId, h.deviceId, h.userId, h.seq.toString()).joinToString("|").encodeToByteArray()

    private fun gzip(data: ByteArray): ByteArray = ByteArrayOutputStream().also { out -> GZIPOutputStream(out).use { it.write(data) } }.toByteArray()

    private fun gunzip(data: ByteArray): ByteArray = GZIPInputStream(ByteArrayInputStream(data)).use { it.readBytes() }
}
