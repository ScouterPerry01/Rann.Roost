// File path and name: core/security/src/main/kotlin/ca/schippers/hfm/security/Signing.kt
// Modified On Timestamp: 2026-10-26 @ 04:33 EDT
// Created On Timestamp: 2026-10-26 @ 04:33 EDT
// File Description: Ed25519 signing keys, signatures and checks (plan-multi-device 4.7, 5.2).
// Uses: Bouncy Castle's Ed25519.
// Used By: core/replication (change sets now, key-ring entries in phase 5).
// Purpose: Let every computer check who made a change, so a member cannot forge another's changes.
package ca.schippers.hfm.security

import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters
import org.bouncycastle.crypto.signers.Ed25519Signer

/** An Ed25519 key pair for signing; 32-byte private seed and 32-byte public key. */
class SigningKeyPair(val privateKey: ByteArray, val publicKey: ByteArray) {
    companion object {
        /** An Ed25519 private key is a 32-byte random seed. */
        fun generate(): SigningKeyPair = fromPrivate(Random.bytes(Ed25519PrivateKeyParameters.KEY_SIZE))

        fun fromPrivate(privateKey: ByteArray): SigningKeyPair =
            SigningKeyPair(privateKey, Ed25519PrivateKeyParameters(privateKey, 0).generatePublicKey().encoded)
    }
}

object Signatures {
    const val SIGNATURE_BYTES = 64

    fun sign(privateKey: ByteArray, data: ByteArray): ByteArray = Ed25519Signer().run {
        init(true, Ed25519PrivateKeyParameters(privateKey, 0))
        update(data, 0, data.size)
        generateSignature()
    }

    /** False for a wrong key, a changed message or a malformed signature; never throws on bad input. */
    fun verify(publicKey: ByteArray, data: ByteArray, signature: ByteArray): Boolean = runCatching {
        if (publicKey.size != Ed25519PublicKeyParameters.KEY_SIZE || signature.size != SIGNATURE_BYTES) return false
        Ed25519Signer().run {
            init(false, Ed25519PublicKeyParameters(publicKey, 0))
            update(data, 0, data.size)
            verifySignature(signature)
        }
    }.getOrDefault(false)
}
