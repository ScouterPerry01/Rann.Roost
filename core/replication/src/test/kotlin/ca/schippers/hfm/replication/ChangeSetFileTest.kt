// File path and name: core/replication/src/test/kotlin/ca/schippers/hfm/replication/ChangeSetFileTest.kt
// Modified On Timestamp: 2026-10-26 @ 04:36 EDT
// Created On Timestamp: 2026-10-26 @ 04:36 EDT
// File Description: Tests of the change-set file: round trip from real captured changes, and every refusal (plan-multi-device 5.2, 10).
// Uses: ChangeSetFile, CaptureTriggers, Outbox, HybridClock; the SQLCipher driver; core/security.
// Used By: ./gradlew :core:replication:test.
// Purpose: Only the group's key holders can read a change set, and nobody can forge, change or move one unnoticed.
package ca.schippers.hfm.replication

import ca.schippers.hfm.data.ledger.LedgerDatabase
import ca.schippers.hfm.data.jdbc.SqlCipherJdbcDriverFactory
import ca.schippers.hfm.security.Random
import ca.schippers.hfm.security.SigningKeyPair
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ChangeSetFileTest {

    @TempDir
    lateinit var temp: Path

    private val household = "0192f0a1-0000-7000-8000-0000000000aa"
    private val partition = "0192f0a1-0000-7000-8000-0000000000bb"
    private val device = "0192f0a1-0000-7000-8000-00000000000a"
    private val partitionKey = Random.key()
    private val perry = SigningKeyPair.generate()
    private val anne = SigningKeyPair.generate()
    private val keys = mapOf("perry" to perry.publicKey, "anne" to anne.publicKey)
    private var opened = 0

    /** A real transaction's changes: captured by the triggers, sealed by the clock. */
    private fun captured(): List<CapturedChange> {
        val d = SqlCipherJdbcDriverFactory().open(temp.resolve("l${++opened}.db"), Random.key())
        d.use {
            LedgerDatabase.Schema.create(d)
            CaptureTriggers.install(d, Database.LEDGER)
            d.exec("INSERT INTO account(id, name, type, currency, opening_date, created_at, updated_at, notes) VALUES ('a1', 'Épargne « été »', 'SAVINGS', 'CAD', '2026-01-01', 1, 1, 'l''été')")
            d.exec("UPDATE account SET name = 'Savings' WHERE id = 'a1'")
            d.exec("DELETE FROM account WHERE id = 'a1'")
            return Outbox.seal(d, HybridClock(device) { 1_790_000_000_000L }, "perry")
        }
    }

    private fun set(seq: Long = 7) = ChangeSet.of(captured(), household, partition, device, seq, coreVersion = 10, ledgerVersion = 37)

    @Test
    fun `a change set made from captured changes reads back exactly`() {
        val set = set()
        val file = ChangeSetFile.write(set, partitionKey, perry)
        assertEquals(set, ChangeSetFile.read(file, partitionKey, keys::get, expectedDevice = device, expectedSeq = 7))
        assertEquals(listOf("I", "U", "D"), set.changes.map { it.kind })
        assertEquals(JsonPrimitive("Savings"), set.changes[1].cols!!["name"])
        assertEquals("7.roostchg", ChangeSetFile.fileName(7))
    }

    @Test
    fun `the header can be read without a key, but names and amounts cannot`() {
        val file = ChangeSetFile.write(set(), partitionKey, perry)
        assertEquals(7L, ChangeSetFile.header(file).seq)
        val text = file.decodeToString()
        assertTrue("Savings" !in text && "Épargne" !in text && "account" !in text, "the body is sealed")
    }

    @Test
    fun `a forged author, another group's key, an unknown user and any changed byte are refused`() {
        val file = ChangeSetFile.write(set(), partitionKey, anne) // Anne signs a set that claims to be Perry's
        assertFailsWith<ChangeSetException> { ChangeSetFile.read(file, partitionKey, keys::get) }
        val good = ChangeSetFile.write(set(), partitionKey, perry)
        assertFailsWith<ChangeSetException> { ChangeSetFile.read(good, Random.key(), keys::get) }
        assertFailsWith<ChangeSetException> { ChangeSetFile.read(good, partitionKey, { null }) }
        for (i in listOf(3, 40, good.size / 2, good.size - 70, good.size - 1)) {
            val changed = good.copyOf().also { it[i] = (it[i].toInt() xor 1).toByte() }
            assertFailsWith<ChangeSetException>("byte $i") { ChangeSetFile.read(changed, partitionKey, keys::get) }
        }
    }

    @Test
    fun `a file renamed, moved to another computer's folder, cut short or of a newer format is refused`() {
        val file = ChangeSetFile.write(set(seq = 7), partitionKey, perry)
        assertFailsWith<ChangeSetException> { ChangeSetFile.read(file, partitionKey, keys::get, expectedSeq = 8) }
        assertFailsWith<ChangeSetException> { ChangeSetFile.read(file, partitionKey, keys::get, expectedDevice = "another") }
        assertFailsWith<ChangeSetException> { ChangeSetFile.read(file.copyOf(file.size - 10), partitionKey, keys::get) }
        assertFailsWith<ChangeSetException> { ChangeSetFile.read("not a change set".encodeToByteArray(), partitionKey, keys::get) }
        val newer = file.decodeToString(0, 60).replaceFirst("\"format\":1", "\"format\":9")
        assertFailsWith<ChangeSetException> { ChangeSetFile.header(newer.encodeToByteArray() + file.copyOfRange(60, file.size)) }
    }

    @Test
    fun `a change stamped by another computer and a set with two authors are refused`() {
        val good = set()
        val foreign = good.copy(changes = good.changes.mapIndexed { i, c -> if (i == 1) c.copy(hlc = c.hlc.replace(device, "elsewhere")) else c })
        assertFailsWith<ChangeSetException> { ChangeSetFile.read(ChangeSetFile.write(foreign, partitionKey, perry), partitionKey, keys::get) }
        val mixed = captured().mapIndexed { i, c -> if (i == 0) c.copy(userId = "anne") else c }
        assertFailsWith<IllegalArgumentException> { ChangeSet.of(mixed, household, partition, device, 1, 10, 37) }
    }
}
