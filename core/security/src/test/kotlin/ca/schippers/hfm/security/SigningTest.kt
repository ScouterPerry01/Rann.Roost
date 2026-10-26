// File path and name: core/security/src/test/kotlin/ca/schippers/hfm/security/SigningTest.kt
// Modified On Timestamp: 2026-10-26 @ 04:33 EDT
// Created On Timestamp: 2026-10-26 @ 04:33 EDT
// File Description: Tests of Ed25519 signing (Signing.kt), including the RFC 8032 test vector.
// Uses: SigningKeyPair, Signatures.
// Used By: ./gradlew :core:security:test.
// Purpose: Signatures are standard Ed25519 and refuse every kind of tampering.
package ca.schippers.hfm.security

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SigningTest {

    private fun hex(s: String) = s.chunked(2).map { it.toInt(16).toByte() }.toByteArray()

    @Test
    fun `matches RFC 8032 test 1`() {
        val key = SigningKeyPair.fromPrivate(hex("9d61b19deffd5a60ba844af492ec2cc44449c5697b326919703bac031cae7f60"))
        assertContentEquals(hex("d75a980182b10ab7d54bfed3c964073a0ee172f3daa62325af021a68f707511a"), key.publicKey)
        val signature = Signatures.sign(key.privateKey, ByteArray(0))
        assertContentEquals(
            hex("e5564300c360ac729086e2cc806e828a84877f1eb8e5d974d873e065224901555fb8821590a33bacc61e39701cf9b46bd25bf5f0595bbe24655141438e7a100b"),
            signature,
        )
        assertTrue(Signatures.verify(key.publicKey, ByteArray(0), signature))
    }

    @Test
    fun `a signature fails with another key, another message or a changed byte, and bad input never throws`() {
        val key = SigningKeyPair.generate()
        val other = SigningKeyPair.generate()
        val message = "change set".encodeToByteArray()
        val signature = Signatures.sign(key.privateKey, message)
        assertTrue(Signatures.verify(key.publicKey, message, signature))
        assertFalse(Signatures.verify(other.publicKey, message, signature))
        assertFalse(Signatures.verify(key.publicKey, "change sets".encodeToByteArray(), signature))
        assertFalse(Signatures.verify(key.publicKey, message, signature.copyOf().also { it[10] = (it[10] + 1).toByte() }))
        assertFalse(Signatures.verify(key.publicKey, message, ByteArray(3)))
        assertFalse(Signatures.verify(ByteArray(5), message, signature))
    }
}
