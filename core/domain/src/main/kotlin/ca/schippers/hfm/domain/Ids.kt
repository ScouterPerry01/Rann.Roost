// File path and name: core/domain/src/main/kotlin/ca/schippers/hfm/domain/Ids.kt
// Modified On Timestamp: 2026-10-26 @ 03:04 EDT
// Created On Timestamp: 2026-10-01 @ 15:53 EDT
// File Description: Record identifiers: time-ordered UUIDv7 for new records, name-based UUIDv5 for rows the app makes itself.
// Uses: java.security.SecureRandom, java.security.MessageDigest.
// Used By: Every service that creates records (core/books, core/sync, the desktop app).
// Purpose: Ids that never clash between devices, and the same id on every computer for the same built-in or automatic row.
package ca.schippers.hfm.domain

import java.nio.ByteBuffer
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID

/**
 * Record identifiers are UUID version 7: unique across every device that creates records
 * (desktop, each paired phone), so items are never imported twice (SYNC-02), and time-ordered,
 * which keeps database indexes compact.
 */
object Ids {
    private val random = SecureRandom()
    private var lastMillis = 0L
    private var sequence = 0

    fun newId(): String = newUuid().toString()

    @Synchronized
    fun newUuid(nowMillis: Long = System.currentTimeMillis()): UUID {
        // Keep ids strictly increasing within one process even if the clock stalls or steps back.
        val millis: Long
        if (nowMillis > lastMillis) {
            millis = nowMillis
            sequence = random.nextInt(0x800)
        } else {
            millis = lastMillis
            sequence++
            check(sequence < 0x1000) { "Too many ids in one millisecond" }
        }
        lastMillis = millis

        val msb = (millis shl 16) or (0x7L shl 12) or sequence.toLong()
        val lsb = (random.nextLong() and 0x3FFFFFFFFFFFFFFFL) or Long.MIN_VALUE // variant 10
        return UUID(msb, lsb)
    }

    /**
     * A name-based UUID version 5 (RFC 9562): the same [namespace] and [parts] give the same id on
     * every computer. Used for rows the app makes by itself (built-in categories, scheduled
     * set-asides, bill occurrences), so two computers of one household make one row, not two
     * (plan-multi-device 4.5, 4.6). [namespace] is an existing id: the household's, or the parent row's.
     */
    fun nameBased(namespace: String, vararg parts: String): String {
        require(parts.isNotEmpty()) { "No name" }
        val ns = UUID.fromString(namespace)
        val sha1 = MessageDigest.getInstance("SHA-1")
        sha1.update(ByteBuffer.allocate(16).putLong(ns.mostSignificantBits).putLong(ns.leastSignificantBits).array())
        sha1.update(parts.joinToString(SEPARATOR).toByteArray(Charsets.UTF_8))
        val hash = ByteBuffer.wrap(sha1.digest())
        val msb = (hash.long and 0xF000L.inv()) or (0x5L shl 12)
        val lsb = (hash.long and 0x3FFFFFFFFFFFFFFFL) or Long.MIN_VALUE // variant 10
        return UUID(msb, lsb).toString()
    }

    /** Joins the parts of a name; a control character, so no part can imitate two. */
    private const val SEPARATOR = "\u001F"

    fun isValid(id: String): Boolean = try {
        UUID.fromString(id).toString() == id.lowercase()
    } catch (_: IllegalArgumentException) {
        false
    }
}
