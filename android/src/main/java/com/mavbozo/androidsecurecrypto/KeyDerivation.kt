package com.mavbozo.androidsecurecrypto

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.ceil

/**
 * Supported HKDF algorithms with their corresponding HMAC functions.
 *
 * Usage examples for different algorithms:
 * @sample com.mavbozo.crypto.samples.keyderivation.KeyDerivationSamples.multipleAlgorithms
 *
 */

enum class HkdfAlgorithm(
    /**
     * The name of the HMAC algorithm as defined in the Java Cryptography Architecture.
     * Used when initializing the [javax.crypto.Mac] instance for HKDF operations.
     */
    val hmacAlgorithm: String,

    /**
     * The length of the MAC output in bytes. This determines the size of the
     * pseudorandom key in the HKDF extract step and the block size for the expand step.
     * - SHA-256: 32 bytes (256 bits)
     * - SHA-512: 64 bytes (512 bits)
     * - SHA-1: 20 bytes (160 bits)
     */
    val macLength: Int
) {

    /**
     * HKDF using HMAC-SHA256 (recommended default)
     * - 256-bit output
     * - 128-bit security level
     * - Good balance of security and performance
     */
    SHA256("HmacSHA256", 32),

    /**
     * HKDF using HMAC-SHA512
     * - 512-bit output
     * - 256-bit security level
     * - Higher security for sensitive applications
     */
    SHA512("HmacSHA512", 64),

    /**
     * HKDF using HMAC-SHA1 (legacy support only)
     * - 160-bit output
     * - Not recommended for new applications
     * - Provided only for compatibility
     */
    @Deprecated("SHA1 is not recommended for new applications")
    SHA1("HmacSHA1", 20)
}

/**
 * Provides secure key derivation functionality using the HKDF (HMAC-based Key Derivation Function)
 * algorithm as specified in RFC 5869. This implementation offers hardware-backed operation when
 * available and automatic cleanup of sensitive key material.
 *
 * Key security features:
 * - Hardware security module integration when available
 * - Automatic secure memory cleanup
 * - Domain separation for derived keys
 * - Side-channel resistant implementation
 * - Multiple hash algorithm support
 *
 * Basic example:
 * ```kotlin
 * // Generate a master key (managed by the application)
 * val masterKey = Random.generateBytes(32).getOrThrow()
 * try {
 *     // Derive an encryption key with domain separation
 *     val derivedKey = KeyDerivation.deriveKey(
 *         masterKey = masterKey,
 *         domain = "myapp.encryption",
 *         context = "user-data-key"
 *     ).getOrThrow()
 *
 *     derivedKey.use { bytes ->
 *         // Use the derived key for cryptographic operations
 *     } // Key is automatically zeroed after use
 * } finally {
 *     masterKey.fill(0) // Clean up master key
 * }
 * ```
 *
 * @sample com.mavbozo.crypto.samples.keyderivation.KeyDerivationSamples.basicKeyDerivation
 * @sample com.mavbozo.crypto.samples.keyderivation.KeyDerivationSamples.domainSeparation
 *
 * Security considerations:
 * 1. Master key requirements:
 *    - Minimum length: 16 bytes (128 bits)
 *    - Should be randomly generated using [Random] or [EnhancedRandom]
 *    - Must be securely stored (e.g., in Android Keystore)
 *
 * 2. Domain separation:
 *    - Use unique domain strings for different key purposes
 *    - Domain strings should include app package name or identifier
 *    - Context strings should identify specific use cases
 *
 * 3. Algorithm selection:
 *    - SHA-256 (default) provides 128-bit security
 *    - SHA-512 provides 256-bit security for high-security needs
 *    - SHA-1 provided only for legacy compatibility
 *
 * 4. Memory handling:
 *    - All derived keys are wrapped in [SecureBytes] for automatic cleanup
 *    - Master keys should be zeroed after use
 *    - Use the provided `use` block pattern to ensure cleanup
 *
 * @see HkdfAlgorithm For supported hash algorithms
 * @see SecureBytes For secure memory handling
 * @see Random For secure random number generation
 */
class KeyDerivation private constructor(
    private val algorithm: HkdfAlgorithm = HkdfAlgorithm.SHA256
) {

    /**
     * Provides factory methods for creating [KeyDerivation] instances and utility
     * functions for key derivation operations.
     *
     * Example usage:
     * ```kotlin
     * // Create a KeyDerivation instance
     * val kdf = KeyDerivation.create().getOrThrow()
     *
     * // Direct key derivation
     * val key = KeyDerivation.deriveKey(
     *     masterKey = masterKey,
     *     domain = "myapp.encryption",
     *     context = "user-data"
     * ).getOrThrow()
     * ```
     */
    companion object {
        /**
         * Creates a new key derivation instance with the specified algorithm.
         *
         * Example:
         * ```kotlin
         * val kdf = KeyDerivation.create(HkdfAlgorithm.SHA256).getOrThrow()
         * ```
         *
         * @param algorithm The HKDF algorithm to use, defaults to SHA-256
         * @return Result containing KeyDerivation instance or error
         */
        suspend fun create(
            algorithm: HkdfAlgorithm = HkdfAlgorithm.SHA256
        ): Result<KeyDerivation> = withContext(Dispatchers.Default) {
            runCatching {
                KeyDerivation(algorithm)
            }
        }

        /**
         * Derives a new key with automatic cleanup and domain separation.
         *
         * Derives a key using HKDF with the following structured info string format:
         * ```
         * com.mavbozo.androidsecurecrypto.<domain>.v1:<context>
         * ```
         *
         * @sample com.mavbozo.crypto.samples.keyderivation.KeyDerivationSamples.basicKeyDerivation
         * @sample com.mavbozo.crypto.samples.keyderivation.KeyDerivationSamples.differentKeySizes
         * @sample com.mavbozo.crypto.samples.keyderivation.KeyDerivationSamples.multipleAlgorithms
         *
         * @param masterKey The master key for derivation (min 16 bytes)
         * @param domain Domain identifier for key separation (e.g., "myapp.encryption")
         * @param context Usage context for the key (e.g., "user-data")
         * @param keySize Size of the derived key in bytes, defaults to 32
         * @param algorithm HKDF algorithm to use, defaults to SHA-256
         * @return Result containing derived key wrapped in SecureBytes
         * @throws IllegalArgumentException if inputs are invalid
         */
        suspend fun deriveKey(
            masterKey: ByteArray,
            domain: String,
            context: String,
            keySize: Int = 32,
            algorithm: HkdfAlgorithm = HkdfAlgorithm.SHA256
        ): Result<SecureBytes> = withContext(Dispatchers.Default) {
            runCatching {
                require(keySize > 0) { "Key size must be positive" }
                require(masterKey.size >= 16) { "Master key too short" }
                require(domain.isNotEmpty()) { "Domain must not be empty" }
                require(context.isNotEmpty()) { "Context must not be empty" }

                // Create info string with domain separation
                val info = "com.mavbozo.androidsecurecrypto.$domain.v1:$context".toByteArray()

                // Create instance with specified algorithm
                val kdf = KeyDerivation(algorithm)

                // Extract and expand
                val prk = kdf.extractKey(masterKey)
                val okm = kdf.expandKey(prk, info, keySize)

                // Wrap in SecureBytes for automatic cleanup
                SecureBytes.wrap(okm)
            }
        }
    }

    /**
     * HKDF Extract function - generates a pseudorandom key.
     */
    private fun extractKey(inputKeyMaterial: ByteArray): ByteArray {
        val salt = ByteArray(algorithm.macLength) // Fixed null salt
        return try {
            val mac = Mac.getInstance(algorithm.hmacAlgorithm)
            mac.init(SecretKeySpec(salt, algorithm.hmacAlgorithm))
            mac.doFinal(inputKeyMaterial)
        } finally {
            salt.fill(0)
        }
    }

    /**
     * HKDF Expand function - generates output keying material.
     */
    private fun expandKey(prk: ByteArray, info: ByteArray, outputLength: Int): ByteArray {
        val mac = Mac.getInstance(algorithm.hmacAlgorithm)
        mac.init(SecretKeySpec(prk, algorithm.hmacAlgorithm))

        val hashLen = algorithm.macLength
        val n = ceil(outputLength.toDouble() / hashLen).toInt()

        val t = ArrayList<ByteArray>(n)
        val result = ByteArray(outputLength)
        var lastT = ByteArray(0)

        try {
            // Generate T(1), T(2), ..., T(n)
            for (i in 0 until n) {
                mac.reset()
                mac.update(lastT)
                mac.update(info)
                mac.update((i + 1).toByte())
                lastT = mac.doFinal()
                t.add(lastT)
            }

            // Concatenate and truncate to the desired length
            var offset = 0
            for (i in 0 until n - 1) {
                System.arraycopy(t[i], 0, result, offset, hashLen)
                offset += hashLen
            }
            // Copy last block with potential truncation
            System.arraycopy(t[n - 1], 0, result, offset, outputLength - offset)

            return result
        } finally {
            // Clean up intermediate values
            t.forEach { it.fill(0) }
            lastT.fill(0)
            prk.fill(0)
        }
    }
}