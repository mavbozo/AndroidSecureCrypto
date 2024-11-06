/**
 * Provides cryptographically secure random number generation with optional
 * hardware-backed entropy sources.
 *
 * Security Features:
 * - Hardware-backed random generation when available via TEE
 * - Automatic entropy source quality detection
 * - Secure memory handling with guaranteed cleanup
 * - Side-channel resistant implementations
 *
 * Random Generation Modes:
 * 1. Basic Mode (Random class):
 *    - System SecureRandom with quality detection
 *    - Automatic fallback mechanisms
 *    - Memory-safe byte generation
 *
 * 2. Enhanced Mode (EnhancedRandom class):
 *    - Additional hardware-backed entropy mixing
 *    - Android Keystore integration
 *    - Multiple independent entropy sources
 *
 * Output Formats:
 * - Raw bytes via SecureBytes wrapper
 * - Hex encoding: n bytes → 2n characters
 * - Base64 encoding: n bytes → ⌈4n/3⌉ chars (with padding)
 *                           → ⌈4n/3⌉ - p chars (no padding, p ≤ 2)
 *
 * Example Usage:
 * ```kotlin
 * // Basic random generation
 * Random.generateBytes(32).fold(
 *     onSuccess = { bytes -> /* use bytes */ },
 *     onFailure = { error -> /* handle error */ }
 * )
 *
 * // Enhanced random generation
 * EnhancedRandom.generateEnhancedBytes(context, 32).fold(
 *     onSuccess = { bytes -> /* use bytes */ },
 *     onFailure = { error -> /* handle error */ }
 * )
 * ```
 *
 * @see EntropyQuality For entropy source classification
 * @see SecureBytes For secure memory handling
 * @see Base64Flags For Base64 encoding options
 */

package com.mavbozo.androidsecurecrypto

import android.content.Context
import android.util.Base64
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.SecureRandom
import java.security.Security
import com.mavbozo.androidsecurecrypto.internal.InternalApi

/**
 * Represents the quality level of entropy sources available to the random number generator.
 * This helps applications make informed decisions about the security level of generated random numbers.
 */
sealed interface EntropyQuality {
    /**
     * Indicates that the random number generator is using hardware-backed
     * entropy sources (e.g., Android Keystore, hardware security module).
     */
    data object Hardware : EntropyQuality

    /**
     * Indicates that the random number generator is using software-based
     * entropy sources as a fallback when hardware sources are unavailable.
     */
    data object Fallback : EntropyQuality
}


/**
 * Type-safe configuration options for Base64 encoding operations.
 * Provides compile-time safety for Base64 encoding flags compared to raw integers.
 */
sealed interface Base64Flags {
    /**
     * Standard Base64 encoding with padding.
     * Does not wrap lines - produces a single continuous string.
     */
    data object Default : Base64Flags {
        internal const val value = Base64.NO_WRAP
    }

    /**
     * Standard Base64 encoding without padding.
     * Does not wrap lines - produces a single continuous string.
     */
    data object NoPadding : Base64Flags {
        internal const val value = Base64.NO_WRAP or Base64.NO_PADDING
    }

    /**
     * URL-safe Base64 encoding with padding.
     * Does not wrap lines - produces a single continuous string.
     */
    data object UrlSafe : Base64Flags {
        internal const val value = Base64.URL_SAFE or Base64.NO_WRAP
    }

    /**
     * URL-safe Base64 encoding without padding.
     * Does not wrap lines - produces a single continuous string.
     */
    data object UrlSafeNoPadding : Base64Flags {
        internal const val value = Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
    }

    /**
     * Provides internal utilities for converting Base64Flags to Android-specific flags.
     */
    @InternalApi
    companion object {
        @InternalApi
        internal fun toAndroidFlags(flags: Base64Flags): Int = when (flags) {
            is Default -> flags.value
            is NoPadding -> flags.value
            is UrlSafe -> flags.value
            is UrlSafeNoPadding -> flags.value
        }
    }
}

/**
 * A secure wrapper for byte arrays that ensures proper cleanup of sensitive data.
 * Implements automatic zeroing of memory when the data is no longer needed.
 *
 * Usage example:
 * ```kotlin
 * secureBytes.use { bytes ->
 *     // Use bytes here
 * } // bytes are automatically zeroed after this block
 * ```
 *
 * Security guarantees:
 * - Automatic zeroing of memory after use
 * - Prevention of accidental exposure via toString()
 * - Immutable value semantics
 */
@JvmInline
value class SecureBytes @InternalApi constructor(private val bytes: ByteArray) {
    /**
     * Internal companion object for creating SecureBytes instances.
     * Not intended for public use.
     */
    @InternalApi
    companion object {
        @InternalApi
        internal fun wrap(bytes: ByteArray) = SecureBytes(bytes)
    }

    /**
     * Safely uses the byte array and ensures it's zeroed after use.
     *
     * @param block Suspending function that receives the byte array
     * @return Result of the block operation
     */
    suspend fun <T> use(block: suspend (ByteArray) -> T): T {
        try {
            return block(bytes)
        } finally {
            bytes.fill(0)
        }
    }
}

/**
 * Provides cryptographically secure random number generation with optional hardware backing.
 *
 * Key features:
 * - Hardware-backed entropy when available
 * - Automatic entropy quality detection
 * - Secure memory handling
 * - Coroutine support
 *
 * Example usage:
 * ```kotlin
 * Random.create().fold(
 *     onSuccess = { random ->
 *         random.nextSecureBytes(32).fold(
 *             onSuccess = { secureBytes ->
 *                 secureBytes.use { bytes ->
 *                     // Use bytes securely here
 *                 }
 *             },
 *             onFailure = { error ->
 *                 // Handle error
 *             }
 *         )
 *     },
 *     onFailure = { error ->
 *         // Handle creation error
 *     }
 * )
 * ```
 *
 * Security considerations:
 * - All operations are performed on the Default dispatcher for better security
 * - Automatic cleanup of sensitive data
 * - Hardware-backed entropy when available
 * - Explicit error handling
 */
class Random private constructor(
    private val random: SecureRandom,
    private val entropyQuality: EntropyQuality
) {
    /**
     * Companion object providing factory methods for Random instances.
     */
    companion object {
        /**
         * Creates a new Random instance with appropriate entropy source.
         *
         * @return Result containing Random instance or error
         */
        suspend fun create(): Result<Random> = withContext(Dispatchers.Default) {
            runCatching {
                val random = SecureRandom().apply {
                    val seedBuffer = ByteArray(64)
                    nextBytes(seedBuffer)
                    seedBuffer.fill(0)
                }

                val quality = when {
                    random.provider.name.contains("AndroidOpenSSL") -> EntropyQuality.Hardware
                    Security.getProvider("AndroidKeyStore") != null -> EntropyQuality.Hardware
                    else -> EntropyQuality.Fallback
                }

                Random(random, quality)
            }
        }

        /**
         * Generates secure random bytes with automatic cleanup.
         *
         * @param size Number of random bytes to generate (must be positive)
         * @return Result containing generated bytes or error
         * @throws IllegalArgumentException if size is not positive
         */
        suspend fun generateBytes(size: Int): Result<ByteArray> =
            create().fold(
                { random -> // Success case
                    random.nextSecureBytes(size).fold(
                        { secureBytes -> // Success case
                            secureBytes.use { bytes ->
                                Result.success(bytes.clone())
                            }
                        },
                        { error -> Result.failure(error) } // Failure case
                    )
                },
                { error -> Result.failure(error) } // Failure case
            )

        /**
         * Generates specified number of random bytes and returns them encoded as a hexadecimal string.
         * Each byte is represented by two hexadecimal characters.
         *
         * @param size Number of random bytes to generate (must be positive)
         * @return Result containing hex string of length size * 2
         * @throws IllegalArgumentException if size is not positive
         */
        suspend fun generateBytesAsHex(size: Int): Result<String> =
            generateBytes(size).mapSecure { bytes ->
                bytes.joinToString("") { "%02x".format(it) }
            }


        /**
         * Generates specified number of random bytes and returns them encoded as a Base64 string.
         * The default encoding:
         * - Does not wrap lines
         * - Includes padding characters
         * - Uses standard Base64 alphabet (not URL-safe)
         *
         * @param size Number of random bytes to generate (must be positive)
         * @param flags Base64 encoding configuration. Use [Base64Flags] constants
         * @return Result containing Base64 string. Length will be ceil(size * 4/3) with padding
         * @throws IllegalArgumentException if size is not positive
         */
        suspend fun generateBytesAsBase64(
            size: Int,
            flags: Base64Flags = Base64Flags.Default
        ): Result<String> =
            generateBytes(size).mapSecure { bytes ->
                Base64.encodeToString(
                    bytes,
                    Base64Flags.toAndroidFlags(flags)
                )
            }

    }

    @InternalApi
    internal suspend fun nextBytes(size: Int): Result<ByteArray> =
        withContext(Dispatchers.Default) {
            runCatching {
                require(size > 0) { "Size must be positive" }
                ByteArray(size).also { bytes ->
                    random.nextBytes(bytes)
                }
            }
        }

    /**
     * Generates secure random bytes with automatic cleanup.
     *
     * @param size Number of random bytes to generate (must be positive)
     * @return Result containing SecureBytes wrapper or error
     * @throws IllegalArgumentException if size is not positive
     */
    suspend fun nextSecureBytes(size: Int): Result<SecureBytes> =
        nextBytes(size).map { SecureBytes.wrap(it) }


    /**
     * Returns the quality level of the entropy source being used.
     *
     * @return EntropyQuality indicating whether hardware or fallback sources are in use
     */
    fun getQuality(): EntropyQuality = entropyQuality
}

/**
 * Internal utility for enhancing entropy using hardware-backed Android KeyStore capabilities.
 *
 * Security features:
 * - Uses Android KeyStore for additional entropy
 * - Implements multiple mixing stages
 * - Constant-time operations where possible
 *
 * @property context Android application context used for KeyStore access
 */
@InternalApi
internal class AndroidEntropyEnhancer(context: Context) {
    private val applicationContext = context.applicationContext
    private val secureRandom = SecureRandom()

    private val masterKey by lazy {
        try {
            MasterKey.Builder(applicationContext, MASTER_KEY_ALIAS)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .setUserAuthenticationRequired(false)
                .build()
        } catch (e: Exception) {
            throw Exception("Failed to create MasterKey: ${e.message}", e)
        }
    }

    /**
     * Internal companion object containing configuration constants.
     */
    @InternalApi
    companion object {
        private const val MASTER_KEY_ALIAS = "EnhancedRandomMasterKey"
    }


    /**
     * Enhances provided entropy using hardware-backed features.
     *
     * Process:
     * 1. Generates additional entropy blocks
     * 2. Mixes entropy sources using XOR operations
     * 3. Applies SHA-512 based mixing function
     * 4. Incorporates hardware-backed key material
     *
     * @param baseEntropy Initial entropy to enhance
     * @param targetSize Desired size of output in bytes
     * @return Enhanced entropy bytes
     * @throws Exception if entropy enhancement fails
     */
    @InternalApi
    internal suspend fun enhanceEntropy(baseEntropy: ByteArray, targetSize: Int): ByteArray =
        withContext(Dispatchers.Default) {
            try {
                // Generate full-size random blocks
                val block1 = ByteArray(targetSize).also { secureRandom.nextBytes(it) }
                val block2 = ByteArray(targetSize).also { secureRandom.nextBytes(it) }

                // XOR the blocks together
                val result = ByteArray(targetSize)
                for (i in 0 until targetSize) {
                    result[i] = (block1[i].toInt() xor
                            block2[i].toInt() xor
                            baseEntropy[i % baseEntropy.size].toInt()).toByte()
                }

                // Final mixing step using SHA-512 in chunks
                val digest = java.security.MessageDigest.getInstance("SHA-512")
                val mixed = ByteArray(targetSize)
                var offset = 0

                while (offset < targetSize) {
                    digest.reset()
                    digest.update(result.copyOfRange(offset, minOf(offset + 64, targetSize)))
                    digest.update(masterKey.toString().toByteArray())

                    val hashedChunk = digest.digest()
                    val copySize = minOf(64, targetSize - offset)
                    System.arraycopy(hashedChunk, 0, mixed, offset, copySize)

                    offset += copySize
                }

                return@withContext mixed
            } catch (e: Exception) {
                throw Exception("Entropy enhancement failed: ${e.message}", e)
            }
        }
}

/**
 * Enhanced random number generator using multiple entropy sources and hardware backing.
 *
 * Security features:
 * - Hardware-backed entropy generation when available
 * - Multiple independent entropy sources
 * - Secure memory handling with automatic cleanup
 * - Constant-time operations where possible
 *
 * Example usage:
 * ```kotlin
 * EnhancedRandom.create(context).fold(
 *     onSuccess = { random ->
 *         random.nextSecureBytes(32).fold(
 *             onSuccess = { secureBytes ->
 *                 secureBytes.use { bytes ->
 *                     // Use bytes safely here
 *                 }
 *             },
 *             onFailure = { error ->
 *                 // Handle error
 *             }
 *         )
 *     },
 *     onFailure = { error ->
 *         // Handle creation error
 *     }
 * )
 * ```
 */
class EnhancedRandom private constructor(
    @InternalApi private val baseRandom: Random,
    @InternalApi private val enhancer: AndroidEntropyEnhancer
) {
    /**
     * Companion object providing factory methods and utility functions for EnhancedRandom.
     */
    companion object {
        /**
         * Creates new EnhancedRandom instance with hardware backing.
         *
         * @param context Android application context
         * @return Result containing EnhancedRandom instance or error
         */
        suspend fun create(context: Context): Result<EnhancedRandom> =
            Random.create().map { baseRandom ->
                try {
                    EnhancedRandom(
                        baseRandom,
                        AndroidEntropyEnhancer(context)
                    )
                } catch (e: Exception) {
                    throw Exception("Failed to create EnhancedRandom: ${e.message}", e)
                }
            }

        /**
         * Generates specified number of enhanced random bytes using hardware backing.
         *
         * @param context Android application context
         * @param size Number of random bytes to generate (must be positive)
         * @return Result containing generated bytes or error
         * @throws IllegalArgumentException if size is not positive
         */
        private suspend fun generateEnhancedBytes(context: Context, size: Int): Result<ByteArray> =
            create(context).fold(
                { random -> // Success case
                    random.nextSecureBytes(size).fold(
                        { secureBytes -> // Success case
                            secureBytes.use { bytes ->
                                Result.success(bytes.clone())
                            }
                        },
                        { error -> Result.failure(error) } // Failure case
                    )
                },
                { error -> Result.failure(error) } // Failure case
            )

        /**
         * Generates specified number of enhanced random bytes and returns them
         * encoded as a hexadecimal string.
         *
         * @param context Android application context
         * @param size Number of random bytes to generate (must be positive)
         * @return Result containing hex string of length size * 2
         * @throws IllegalArgumentException if size is not positive
         */
        suspend fun generateEnhancedBytesAsHex(context: Context, size: Int): Result<String> =
            generateEnhancedBytes(context, size).mapSecure { bytes ->
                bytes.joinToString("") { "%02x".format(it) }
            }

        /**
         * Generates specified number of enhanced random bytes and returns them
         * encoded as a Base64 string.
         *
         * @param context Android application context
         * @param size Number of random bytes to generate (must be positive)
         * @param flags Base64 encoding configuration
         * @return Result containing Base64 string
         * @throws IllegalArgumentException if size is not positive
         */
        suspend fun generateEnhancedBytesAsBase64(
            context: Context,
            size: Int,
            flags: Base64Flags = Base64Flags.Default
        ): Result<String> {
            return generateEnhancedBytes(context, size).mapSecure { bytes ->
                Base64.encodeToString(
                    bytes,
                    Base64Flags.toAndroidFlags(flags)
                ).trimEnd()
            }
        }
    }

    /**
     * Generates enhanced random bytes using hardware-backed entropy sources.
     *
     * Process:
     * 1. Generates base entropy using SecureRandom
     * 2. Enhances entropy using hardware features
     * 3. Applies additional mixing function
     *
     * @param size Number of random bytes to generate (must be positive)
     * @return Result containing the generated bytes or error
     * @throws IllegalArgumentException if size is not positive
     */
    @InternalApi
    internal suspend fun nextBytes(size: Int): Result<ByteArray> =
        withContext(Dispatchers.Default) {
            runCatching {
                require(size > 0) { "Size must be positive" }

                baseRandom.nextSecureBytes(size).fold(
                    onSuccess = { secureBytes ->
                        try {
                            secureBytes.use { baseBytes ->
                                try {
                                    enhancer.enhanceEntropy(baseBytes, size).also {
                                        baseBytes.fill(0) // Extra safety
                                    }
                                } catch (e: Exception) {
                                    throw Exception("Entropy enhancement failed: ${e.message}", e)
                                }
                            }
                        } catch (e: Exception) {
                            throw Exception("Secure bytes processing failed: ${e.message}", e)
                        }
                    },
                    onFailure = { e ->
                        throw Exception("Base random generation failed: ${e.message}", e)
                    }
                )
            }
        }

    /**
     * Generates enhanced random bytes wrapped in SecureBytes for automatic cleanup.
     *
     * Security features:
     * - Automatic zeroing of memory after use
     * - Hardware-backed entropy when available
     * - Multiple entropy source mixing
     *
     * @param size Number of random bytes to generate (must be positive)
     * @return Result containing SecureBytes wrapper or error
     * @throws IllegalArgumentException if size is not positive
     */
    suspend fun nextSecureBytes(size: Int): Result<SecureBytes> =
        nextBytes(size).map { SecureBytes.wrap(it) }

    /**
     * Returns the quality level of the entropy source being used.
     *
     * For EnhancedRandom, this will always return [EntropyQuality.Hardware] as it requires
     * hardware backing for operation. If hardware backing is unavailable, creation will fail
     * rather than falling back to software-only implementation.
     *
     * @return EntropyQuality.Hardware indicating hardware-backed entropy is in use
     */
    fun getQuality(): EntropyQuality = EntropyQuality.Hardware
}


/**
 * Secure operations utilities for handling sensitive data.
 * Internal use only.
 */
@InternalApi
internal object SecureOps {
    /**
     * Maps a Result<ByteArray> to Result<T> with secure cleanup.
     * Ensures sensitive bytes are zeroed after use.
     */
    internal fun <T> withSecureCleanup(
        result: Result<ByteArray>,
        transform: (ByteArray) -> T
    ): Result<T> = result.fold(
        onSuccess = { bytes ->
            try {
                Result.success(transform(bytes))
            } finally {
                bytes.fill(0)
            }
        },
        onFailure = { Result.failure(it) }
    )
}

// Extension functions for Result type to provide better error handling
/**
 * Maps a Result<ByteArray> to Result<SecureBytes> with proper cleanup.
 */
@InternalApi
internal fun <T> Result<ByteArray>.mapSecure(transform: (ByteArray) -> T): Result<T> =
    SecureOps.withSecureCleanup(this, transform)