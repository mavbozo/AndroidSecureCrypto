package com.mavbozo.crypto.samples.cipher

import com.mavbozo.androidsecurecrypto.Cipher
import com.mavbozo.androidsecurecrypto.Random
import java.io.File

/**
 * Collection of samples demonstrating secure encryption/decryption operations.
 */
object CipherSamples {
    /**
     * Demonstrates string encryption and decryption with automatic key cleanup.
     *
     * Example usage in documentation:
     * ```kotlin
     * @sample com.mavbozo.crypto.samples.cipher.CipherSamples.stringEncryption
     * ```
     */
    suspend fun stringEncryption(): Result<String> = runCatching {
        val key = Random.generateBytes(32).getOrThrow()
        try {
            // Encrypt string data
            val plaintext = "Sensitive information to encrypt"
            val encrypted = Cipher.encryptString(key, plaintext).getOrThrow()

            // Decrypt data
            val decrypted = Cipher.decryptString(key, encrypted).getOrThrow()

            buildString {
                appendLine("Original : $plaintext")
                appendLine("Encrypted: $encrypted")
                appendLine("Decrypted: $decrypted")
            }
        } finally {
            key.fill(0) // Secure cleanup
        }
    }

    /**
     * Demonstrates byte array encryption and decryption with secure memory handling.
     *
     * Example usage in documentation:
     * ```kotlin
     * @sample com.mavbozo.crypto.samples.cipher.CipherSamples.byteArrayEncryption
     * ```
     */
    suspend fun byteArrayEncryption(): Result<String> = runCatching {
        val key = Random.generateBytes(32).getOrThrow()
        try {
            // Sample data
            val data = "Binary data for encryption".encodeToByteArray()

            // Encrypt bytes
            val encrypted = Cipher.encryptBytes(key, data).getOrThrow()

            // Decrypt bytes
            val decrypted = Cipher.decryptBytes(key, encrypted).getOrThrow()

            buildString {
                appendLine("Original : ${data.decodeToString()}")
                appendLine("Encrypted (hex): ${encrypted.joinToString("") { "%02x".format(it) }}")
                appendLine("Decrypted: ${decrypted.decodeToString()}")
            }
        } finally {
            key.fill(0) // Secure cleanup
        }
    }

    /**
     * Demonstrates file encryption and decryption with atomic operations.
     *
     * Example usage in documentation:
     * ```kotlin
     * @sample com.mavbozo.crypto.samples.cipher.CipherSamples.fileEncryption
     * ```
     */
    suspend fun fileEncryption(
        sourceFile: File,
        encryptedFile: File,
        decryptedFile: File
    ): Result<String> = runCatching {
        val key = Random.generateBytes(32).getOrThrow()
        try {
            // Encrypt file
            Cipher.encryptFile(key, sourceFile, encryptedFile).getOrThrow()

            // Decrypt file
            Cipher.decryptFile(key, encryptedFile, decryptedFile).getOrThrow()

            buildString {
                appendLine("Source file content    : ${sourceFile.readText()}")
                appendLine("Encrypted file size    : ${encryptedFile.length()} bytes")
                appendLine("Decrypted file content : ${decryptedFile.readText()}")
            }
        } finally {
            key.fill(0) // Secure cleanup
        }
    }
}