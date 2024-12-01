// KeyDerivationSamples.kt
package com.mavbozo.crypto.samples.keyderivation

import com.mavbozo.androidsecurecrypto.Random
import com.mavbozo.androidsecurecrypto.KeyDerivation
import com.mavbozo.androidsecurecrypto.HkdfAlgorithm

/**
 * Collection of samples demonstrating secure key derivation operations.
 */
object KeyDerivationSamples {
    /**
     * Demonstrates basic key derivation with default parameters.
     *
     * Example usage in documentation:
     * ```kotlin
     * @sample com.mavbozo.crypto.samples.keyderivation.KeyDerivationSamples.basicKeyDerivation
     * ```
     */
    suspend fun basicKeyDerivation(): Result<String> = runCatching {
        val masterKey = Random.generateBytes(32).getOrThrow()
        try {
            // Derive a key using SHA-256 (default)
            val derivedKey = KeyDerivation.deriveKey(
                masterKey = masterKey,
                domain = "myapp.encryption",
                context = "user-data-key"
            ).getOrThrow()

            derivedKey.use { bytes ->
                buildString {
                    appendLine("Master Key (hex): ${masterKey.joinToString("") { "%02x".format(it) }}")
                    appendLine("Derived Key (hex): ${bytes.joinToString("") { "%02x".format(it) }}")
                    appendLine("Domain: myapp.encryption")
                    appendLine("Context: user-data-key")
                }
            }
        } finally {
            masterKey.fill(0) // Secure cleanup
        }
    }

    /**
     * Demonstrates key derivation with different hash algorithms.
     *
     * Example usage in documentation:
     * ```kotlin
     * @sample com.mavbozo.crypto.samples.keyderivation.KeyDerivationSamples.multipleAlgorithms
     * ```
     */
    suspend fun multipleAlgorithms(): Result<String> = runCatching {
        val masterKey = Random.generateBytes(32).getOrThrow()
        try {
            val results = buildString {
                HkdfAlgorithm.entries.forEach { algorithm ->
                    val derivedKey = KeyDerivation.deriveKey(
                        masterKey = masterKey,
                        domain = "myapp.encryption",
                        context = "test-key",
                        algorithm = algorithm
                    ).getOrThrow()

                    derivedKey.use { bytes ->
                        appendLine("${algorithm.name}:")
                        appendLine("  Key (hex): ${bytes.joinToString("") { "%02x".format(it) }}")
                        appendLine("  Output size: ${bytes.size} bytes")
                        appendLine()
                    }
                }
            }
            results
        } finally {
            masterKey.fill(0)
        }
    }

    /**
     * Demonstrates deriving keys of different sizes.
     *
     * Example usage in documentation:
     * ```kotlin
     * @sample com.mavbozo.crypto.samples.keyderivation.KeyDerivationSamples.differentKeySizes
     * ```
     */
    suspend fun differentKeySizes(): Result<String> = runCatching {
        val masterKey = Random.generateBytes(32).getOrThrow()
        try {
            val keySizes = listOf(16, 24, 32, 64) // Different key sizes in bytes
            val results = buildString {
                keySizes.forEach { size ->
                    val derivedKey = KeyDerivation.deriveKey(
                        masterKey = masterKey,
                        domain = "myapp.encryption",
                        context = "test-key",
                        keySize = size
                    ).getOrThrow()

                    derivedKey.use { bytes ->
                        appendLine("$size-byte key:")
                        appendLine("  Key (hex): ${bytes.joinToString("") { "%02x".format(it) }}")
                        appendLine()
                    }
                }
            }
            results
        } finally {
            masterKey.fill(0)
        }
    }

    /**
     * Demonstrates domain separation in key derivation.
     *
     * Example usage in documentation:
     * ```kotlin
     * @sample com.mavbozo.crypto.samples.keyderivation.KeyDerivationSamples.domainSeparation
     * ```
     */
    suspend fun domainSeparation(): Result<String> = runCatching {
        val masterKey = Random.generateBytes(32).getOrThrow()
        try {
            val domains = listOf(
                "myapp.encryption" to "data-key",
                "myapp.encryption" to "metadata-key",
                "myapp.signing" to "data-key",
                "myapp.signing" to "metadata-key"
            )

            val results = buildString {
                domains.forEach { (domain, context) ->
                    val derivedKey = KeyDerivation.deriveKey(
                        masterKey = masterKey,
                        domain = domain,
                        context = context
                    ).getOrThrow()

                    derivedKey.use { bytes ->
                        appendLine("Domain: $domain")
                        appendLine("Context: $context")
                        appendLine("Key (hex): ${bytes.joinToString("") { "%02x".format(it) }}")
                        appendLine()
                    }
                }
            }
            results
        } finally {
            masterKey.fill(0)
        }
    }
}
