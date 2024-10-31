// /library/src/main/java/com/mavbozo/securecrypto/Random.kt

package com.mavbozo.securecrypto

import android.content.Context
import android.os.Process
import android.os.SystemClock
import android.util.Base64
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.SecureRandom
import java.security.Security

sealed interface EntropyQuality {
    data object Hardware : EntropyQuality
    data object Fallback : EntropyQuality
}

/**
 * Type-safe wrapper for Base64 encoding flags
 */
sealed interface Base64Flags {
    /** Standard Base64 encoding with padding */
    data object Default : Base64Flags {
        internal val value = Base64.DEFAULT
    }

    /** Standard Base64 encoding without padding */
    data object NoPadding : Base64Flags {
        internal val value = Base64.NO_PADDING
    }

    /** URL-safe Base64 encoding with padding */
    data object UrlSafe : Base64Flags {
        internal val value = Base64.URL_SAFE
    }

    /** URL-safe Base64 encoding without padding */
    data object UrlSafeNoPadding : Base64Flags {
        internal val value = Base64.URL_SAFE or Base64.NO_PADDING
    }

    companion object {
        internal fun toAndroidFlags(flags: Base64Flags): Int = when (flags) {
            is Default -> flags.value
            is NoPadding -> flags.value
            is UrlSafe -> flags.value
            is UrlSafeNoPadding -> flags.value
        }
    }
}


@JvmInline
value class SecureBytes private constructor(private val bytes: ByteArray) {
    companion object {
        internal fun wrap(bytes: ByteArray) = SecureBytes(bytes)
    }

    suspend fun <T> use(block: suspend (ByteArray) -> T): T {
        try {
            return block(bytes)
        } finally {
            bytes.fill(0)
        }
    }
}

class Random private constructor(
    private val random: SecureRandom,
    private val entropyQuality: EntropyQuality
) {
    companion object {
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

        suspend fun generateBytes(size: Int): Result<ByteArray> =
            create().fold (
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

        suspend fun generateHexString(length: Int): Result<String> =
            generateBytes((length + 1) / 2).fold(
                { bytes -> // Success case
                    try {
                        Result.success(
                            bytes.joinToString("") { "%02x".format(it) }
                                .substring(0, length)
                        )
                    } finally {
                        bytes.fill(0)
                    }
                },
                { error -> Result.failure(error) } // Failure case
            )

        suspend fun generateBase64String(
            length: Int,
            flags: Base64Flags = Base64Flags.Default
        ): Result<String> {
            require(length > 0) { "Length must be positive" }

            // Calculate required bytes for desired Base64 length
            val bytesNeeded = (length * 3 + 3) / 4

            return generateBytes(bytesNeeded).fold(
                onSuccess = { bytes ->
                    try {
                        val base64String = Base64.encodeToString(
                            bytes,
                            Base64Flags.toAndroidFlags(flags)
                        )

                        // Trim to exact requested length
                        Result.success(base64String.substring(0, length))
                    } finally {
                        bytes.fill(0)
                    }
                },
                onFailure = { error ->
                    Result.failure(error)
                }
            )
        }
    }

    suspend fun nextBytes(size: Int): Result<ByteArray> = withContext(Dispatchers.Default) {
        runCatching {
            require(size > 0) { "Size must be positive" }
            ByteArray(size).also { bytes ->
                random.nextBytes(bytes)
            }
        }
    }

    suspend fun nextSecureBytes(size: Int): Result<SecureBytes> =
        nextBytes(size).map { SecureBytes.wrap(it) }

    fun getQuality(): EntropyQuality = entropyQuality
}


class AndroidEntropyEnhancer(context: Context) {
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

    companion object {
        private const val MASTER_KEY_ALIAS = "EnhancedRandomMasterKey"
    }

    suspend fun enhanceEntropy(baseEntropy: ByteArray, targetSize: Int): ByteArray = withContext(Dispatchers.Default) {
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

class EnhancedRandom private constructor(
    private val baseRandom: Random,
    private val enhancer: AndroidEntropyEnhancer
) {
    companion object {
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

        suspend fun generateEnhancedBytes(context: Context, size: Int): Result<ByteArray> =
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

        suspend fun generateEnhancedHexString(context: Context, length: Int): Result<String> =
            generateEnhancedBytes(context, (length + 1) / 2).fold(
                { bytes -> // Success case
                    try {
                        Result.success(
                            bytes.joinToString("") { "%02x".format(it) }
                                .substring(0, length)
                        )
                    } finally {
                        bytes.fill(0)
                    }
                },
                { error -> Result.failure(error) } // Failure case
            )

        suspend fun generateEnhancedBase64String(
            context: Context,
            length: Int,
            flags: Base64Flags = Base64Flags.Default
        ): Result<String> {
            require(length > 0) { "Length must be positive" }

            return generateEnhancedBytes(context, (length * 3 + 3) / 4).fold(
                { bytes ->
                    try {
                        Result.success(
                            Base64.encodeToString(
                                bytes,
                                Base64Flags.toAndroidFlags(flags)
                            ).substring(0, length)
                        )
                    } finally {
                        bytes.fill(0)
                    }
                },
                { error -> Result.failure(error) }
            )
        }
    }

    suspend fun nextBytes(size: Int): Result<ByteArray> = withContext(Dispatchers.Default) {
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

    suspend fun nextSecureBytes(size: Int): Result<SecureBytes> =
        nextBytes(size).map { SecureBytes.wrap(it) }

    fun getQuality(): EntropyQuality = EntropyQuality.Hardware
}