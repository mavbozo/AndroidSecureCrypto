package com.mavbozo.crypto.samples.cipher

import com.mavbozo.crypto.samples.common.SampleActivity
import java.io.File

class CipherActivity : SampleActivity() {
    override suspend fun runSamples() {
        appendLine("String Encryption/Decryption:")
        CipherSamples.stringEncryption().fold(
            onSuccess = { appendLine(it) },
            onFailure = { appendLine("Error: ${it.message}") }
        )

        appendLine("\nByte Array Encryption/Decryption:")
        CipherSamples.byteArrayEncryption().fold(
            onSuccess = { appendLine(it) },
            onFailure = { appendLine("Error: ${it.message}") }
        )

        appendLine("\nFile Encryption/Decryption:")
        val sourceFile = File(filesDir, "sample.txt")
        val encryptedFile = File(filesDir, "sample.enc")
        val decryptedFile = File(filesDir, "sample.dec")

        // Create sample file
        sourceFile.writeText("Secret data for encryption test")

        CipherSamples.fileEncryption(sourceFile, encryptedFile, decryptedFile).fold(
            onSuccess = { appendLine(it) },
            onFailure = { appendLine("Error: ${it.message}") }
        )

        // Cleanup test files
        sourceFile.delete()
        encryptedFile.delete()
        decryptedFile.delete()
    }
}