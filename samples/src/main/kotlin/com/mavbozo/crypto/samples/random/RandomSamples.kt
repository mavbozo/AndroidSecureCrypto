package com.mavbozo.crypto.samples.random

import android.content.Context
import com.mavbozo.androidsecurecrypto.Random
import com.mavbozo.androidsecurecrypto.EnhancedRandom
import com.mavbozo.androidsecurecrypto.Base64Flags

/**
 * Collection of samples demonstrating secure random number generation.
 */
object RandomSamples {
    /**
     * Demonstrates basic random byte generation.
     *
     * Example usage in documentation:
     * ```kotlin
     * @sample com.mavbozo.crypto.samples.random.RandomSamples.basicRandomBytes
     * ```
     */
    suspend fun basicRandomBytes(): Result<String> =
        Random.generateBytes(32).map { bytes ->
            try {
                bytes.joinToString("") { "%02x".format(it) }
            } finally {
                bytes.fill(0)
            }
        }

    /**
     * Demonstrates random hex string generation.
     *
     * Example usage in documentation:
     * ```kotlin
     * @sample com.mavbozo.crypto.samples.random.RandomSamples.randomHexString
     * ```
     */
    suspend fun randomHexString(): Result<String> =
        Random.generateBytesAsHex(32)

    /**
     * Demonstrates Base64 encoding options.
     *
     * Example usage in documentation:
     * ```kotlin
     * @sample com.mavbozo.crypto.samples.random.RandomSamples.base64Examples
     * ```
     */
    suspend fun base64Examples(): Map<String, Result<String>> {
        val examples = listOf(
            "Default" to Base64Flags.Default,
            "No Padding" to Base64Flags.NoPadding,
            "URL Safe" to Base64Flags.UrlSafe,
            "URL Safe No Padding" to Base64Flags.UrlSafeNoPadding
        )

        return examples.associate { (name, flags) ->
            name to Random.generateBytesAsBase64(32, flags)
        }
    }

    /**
     * Demonstrates hardware-backed random generation.
     *
     * Example usage in documentation:
     * ```kotlin
     * @sample com.mavbozo.crypto.samples.random.RandomSamples.enhancedRandom
     * ```
     */
    suspend fun enhancedRandom(context: Context): Result<String> =
        EnhancedRandom.create(context).fold(
            onSuccess = { random ->
                random.nextSecureBytes(32).fold(
                    onSuccess = { secureBytes ->
                        runCatching {
                            secureBytes.use { bytes ->
                                bytes.joinToString("") { "%02x".format(it) }
                            }
                        }
                    },
                    onFailure = { Result.failure(it) }
                )
            },
            onFailure = { Result.failure(it) }
        )
}