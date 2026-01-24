package com.tony.appbooster.wear.data.client.pairing

import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.generators.HKDFBytesGenerator
import org.bouncycastle.crypto.params.HKDFParameters
import org.bouncycastle.math.ec.ECPoint
import org.bouncycastle.math.ec.custom.sec.SecP256R1Curve
import java.math.BigInteger
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * SPAKE2+ implementation for ADB pairing protocol.
 *
 * SPAKE2+ is a Password-Authenticated Key Exchange protocol that allows
 * two parties who share a password to establish a shared secret key.
 *
 * This implementation follows the ADB pairing protocol specification which uses:
 * - P-256 (secp256r1) elliptic curve
 * - SHA-256 for hashing
 * - HKDF for key derivation
 * - AES-128-GCM for encryption after key exchange
 *
 * The M and N points are fixed values from BoringSSL's SPAKE2 implementation,
 * which Android's ADB uses for the pairing protocol.
 *
 * Protocol flow:
 * 1. Both parties compute a blinded public key using the password
 * 2. Exchange blinded public keys
 * 3. Both derive the same shared secret
 * 4. Use shared secret to encrypt further communication
 */
class Spake2PlusHandler(
    private val password: ByteArray,
    private val isClient: Boolean = true
) {
    private val curve = SecP256R1Curve()
    private val random = SecureRandom()

    // Curve parameters
    private val n: BigInteger = curve.order
    private val g: ECPoint = curve.createPoint(
        BigInteger("6b17d1f2e12c4247f8bce6e563a440f277037d812deb33a0f4a13945d898c296", 16),
        BigInteger("4fe342e2fe1a7f9b8ee7eb4a7c0f9e162bce33576b315ececbb6406837bf51f5", 16)
    )

    // SPAKE2 M and N points for P-256 from RFC 9382 / BoringSSL
    // These are the standard SPAKE2 points used in ADB pairing
    private val pointM: ECPoint = curve.decodePoint(
        hexToBytes("04" +
            "886e2f97ace46e55ba9dd7242579f2993b64e16ef3dcab95afd497333d8fa12f" +
            "5ff355163e43ce224e0b0e65ff02ac8e5c7be09419c785e0ca547d55a12e2d20")
    )

    private val pointN: ECPoint = curve.decodePoint(
        hexToBytes("04" +
            "d048032c6ea0b6d697ddc2e86bda85a33adac920f1bf18e1b0c6d166a5cecdaf" +
            "3001dc2e6f2239a5294e0a277a8ff8ee93f09e7b2ac4c0c16e4a8e52bdfd5e21")
    )

    // Our secret scalar
    private var secretScalar: BigInteger? = null

    // Our blinded public key
    private var ourPublicKey: ByteArray? = null

    // Derived shared secret
    private var sharedSecret: ByteArray? = null

    // Encryption key derived from shared secret
    private var encryptionKey: ByteArray? = null

    /**
     * Generates our SPAKE2 message (blinded public key).
     *
     * For client: X = x*G + pw*M
     * For server: Y = y*G + pw*N
     *
     * @return The blinded public key to send to the peer.
     */
    fun generatePublicKey(): ByteArray {
        // Generate random scalar x or y
        secretScalar = BigInteger(256, random).mod(n)

        // Compute pw (password hash as scalar)
        val pwHash = sha256(password)
        val pw = BigInteger(1, pwHash).mod(n)

        // Select the blinding point (M for client, N for server)
        val blindingPoint = if (isClient) pointM else pointN

        // Compute blinded public key: scalar*G + pw*blindingPoint
        val scalarTimesG = g.multiply(secretScalar)
        val pwTimesBlind = blindingPoint.multiply(pw)
        val publicPoint = scalarTimesG.add(pwTimesBlind).normalize()

        ourPublicKey = encodePoint(publicPoint)
        return ourPublicKey!!
    }

    /**
     * Processes the peer's SPAKE2 message and derives the shared secret.
     *
     * For client receiving Y: K = x * (Y - pw*N)
     * For server receiving X: K = y * (X - pw*M)
     *
     * @param peerPublicKey The peer's blinded public key.
     * @return The derived shared secret.
     */
    fun processPublicKey(peerPublicKey: ByteArray): ByteArray {
        val secret = secretScalar ?: throw IllegalStateException("Must call generatePublicKey first")

        // Decode peer's point
        val peerPoint = decodePoint(peerPublicKey)

        // Compute pw
        val pwHash = sha256(password)
        val pw = BigInteger(1, pwHash).mod(n)

        // Select the blinding point (N for client processing server's msg, M for server)
        val blindingPoint = if (isClient) pointN else pointM

        // Remove blinding: peerPoint - pw*blindingPoint
        val pwTimesBlind = blindingPoint.multiply(pw)
        val unblindedPoint = peerPoint.add(pwTimesBlind.negate()).normalize()

        // Compute shared secret: secret * unblindedPoint
        val sharedPoint = unblindedPoint.multiply(secret).normalize()

        // Derive shared secret from the x-coordinate of the shared point (SPAKE2 standard)
        val rawSecret = sharedPoint.affineXCoord.encoded

        // Create the transcript for key confirmation (as per SPAKE2+ spec)
        // For ADB pairing, we derive keys using HKDF with specific info strings
        val keyMaterial = deriveKeyMaterial(rawSecret)
        sharedSecret = keyMaterial

        // Derive encryption key using HKDF
        // AOSP uses "adb pairing_auth aes-128-gcm key" as info
        encryptionKey = deriveKey(keyMaterial, "adb pairing_auth aes-128-gcm key".toByteArray(), 16)

        return sharedSecret!!
    }

    /**
     * Derives key material from the raw ECDH shared secret.
     */
    private fun deriveKeyMaterial(rawSecret: ByteArray): ByteArray {
        val hkdf = HKDFBytesGenerator(SHA256Digest())
        hkdf.init(HKDFParameters(rawSecret, null, "SPAKE2 secret".toByteArray()))

        val keyMaterial = ByteArray(32)
        hkdf.generateBytes(keyMaterial, 0, 32)
        return keyMaterial
    }

    /**
     * Encrypts data using AES-128-GCM with the derived key.
     *
     * @param plaintext Data to encrypt.
     * @return IV (12 bytes) + ciphertext + tag.
     */
    fun encrypt(plaintext: ByteArray): ByteArray {
        val key = encryptionKey ?: throw IllegalStateException("Key exchange not completed")

        val iv = ByteArray(12)
        random.nextBytes(iv)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val keySpec = SecretKeySpec(key, "AES")
        val gcmSpec = GCMParameterSpec(128, iv)

        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec)
        val ciphertext = cipher.doFinal(plaintext)

        return iv + ciphertext
    }

    /**
     * Decrypts data using AES-128-GCM with the derived key.
     *
     * @param ciphertext IV (12 bytes) + ciphertext + tag.
     * @return Decrypted plaintext.
     */
    fun decrypt(ciphertext: ByteArray): ByteArray {
        val key = encryptionKey ?: throw IllegalStateException("Key exchange not completed")

        require(ciphertext.size > 12) { "Ciphertext too short" }

        val iv = ciphertext.copyOfRange(0, 12)
        val encrypted = ciphertext.copyOfRange(12, ciphertext.size)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val keySpec = SecretKeySpec(key, "AES")
        val gcmSpec = GCMParameterSpec(128, iv)

        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec)
        return cipher.doFinal(encrypted)
    }

    /**
     * Checks if key exchange has been completed.
     */
    fun isKeyExchangeComplete(): Boolean = encryptionKey != null

    // --- Private helper functions ---

    private fun sha256(data: ByteArray): ByteArray {
        return MessageDigest.getInstance("SHA-256").digest(data)
    }

    private fun encodePoint(point: ECPoint): ByteArray {
        return point.getEncoded(false) // Uncompressed format
    }

    private fun decodePoint(data: ByteArray): ECPoint {
        return curve.decodePoint(data)
    }

    private fun deriveKey(secret: ByteArray, info: ByteArray, length: Int): ByteArray {
        val hkdf = HKDFBytesGenerator(SHA256Digest())
        hkdf.init(HKDFParameters(secret, null, info))

        val derivedKey = ByteArray(length)
        hkdf.generateBytes(derivedKey, 0, length)
        return derivedKey
    }

    private fun hexToBytes(hex: String): ByteArray {
        val result = ByteArray(hex.length / 2)
        for (i in result.indices) {
            result[i] = hex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
        return result
    }
}
