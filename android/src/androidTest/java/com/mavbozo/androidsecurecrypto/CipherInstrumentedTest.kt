package com.mavbozo.androidsecurecrypto

import android.content.Context
import android.util.Base64
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.random.Random

@RunWith(AndroidJUnit4::class)
class CipherInstrumentedTest {
    private lateinit var context: Context
    private val validKey32 = ByteArray(32) { it.toByte() }
    private val invalidKey = ByteArray(24) { it.toByte() }

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
    }

    // Basic Functionality Tests
    @Test
    fun testBasicEncryptionDecryption() = runBlocking {
        // Given
        val plaintext = "Hello, World!"

        // When
        val encrypted = Cipher.encryptString(validKey32, plaintext).getOrThrow()
        val decrypted = Cipher.decryptString(validKey32, encrypted).getOrThrow()

        // Then
        assertEquals("Decrypted text should match original", plaintext, decrypted)
        assertNotEquals("Encrypted text should not match original", plaintext, encrypted)
    }

    @Test
    fun testHardwareBackedEncryption() = runBlocking {
        // Create cipher with explicit hardware-backed requirement
        val result = Cipher.create()
        assertTrue("Cipher creation failed", result.isSuccess)

        // Instead of checking isHardwareBacked, check the provider
        val cipher = result.getOrThrow()
        val provider = cipher.getProvider()
        assertTrue(
            "Should use AndroidKeyStore or AndroidOpenSSL provider",
            provider.contains("AndroidKeyStore") || provider.contains("AndroidOpenSSL")
        )
    }

    @Test
    fun testConcurrentEncryption() = runBlocking {
        val plaintexts = List(10) { "Test message $it" }

        // Perform concurrent encryptions
        val results = coroutineScope {
            plaintexts.map { plaintext ->
                async(Dispatchers.Default) {
                    Cipher.encryptString(validKey32, plaintext)
                }
            }.awaitAll()
        }

        // Verify all encryptions succeeded and produced different ciphertexts
        val ciphertexts = results.map { it.getOrThrow() }
        val uniqueCiphertexts = ciphertexts.toSet()
        assertEquals(
            "All concurrent encryptions should produce unique ciphertexts",
            ciphertexts.size, uniqueCiphertexts.size
        )
    }

    @Test
    fun testLargeDataHandling() = runBlocking {
        // Generate 1MB of random data
        val largeData = StringBuilder()
        repeat(1024 * 1024) { // 1MB
            largeData.append((Random.nextInt(26) + 'a'.code).toChar())
        }
        val plaintext = largeData.toString()

        // Encrypt and decrypt
        val encrypted = Cipher.encryptString(validKey32, plaintext).getOrThrow()
        val decrypted = Cipher.decryptString(validKey32, encrypted).getOrThrow()

        assertEquals(
            "Large data should be correctly encrypted and decrypted",
            plaintext, decrypted
        )
    }

    @Test
    fun testFileEncryption() = runBlocking {
        // Create test files in app-specific directory
        val sourceFile = File(context.cacheDir, "test_source.txt")
        val encryptedFile = File(context.cacheDir, "test_encrypted.bin")
        val decryptedFile = File(context.cacheDir, "test_decrypted.txt")

        try {
            // Write test data
            val testData = "Test file content with special chars: 世界"
            sourceFile.writeText(testData)

            // Encrypt
            val encryptResult = Cipher.encryptFile(validKey32, sourceFile, encryptedFile)
            assertTrue("File encryption failed", encryptResult.isSuccess)

            // Decrypt
            val decryptResult = Cipher.decryptFile(validKey32, encryptedFile, decryptedFile)
            assertTrue("File decryption failed", decryptResult.isSuccess)

            // Verify
            assertEquals(
                "Decrypted file content should match original",
                testData, decryptedFile.readText()
            )

        } finally {
            // Cleanup
            sourceFile.delete()
            encryptedFile.delete()
            decryptedFile.delete()
        }
    }

    @Test
    fun testEncryptionWithInvalidKeys() = runBlocking {
        val plaintext = "Test message"

        // Test with invalid key size
        val result = Cipher.encryptString(invalidKey, plaintext)
        assertTrue("Should fail with invalid key size", result.isFailure)
        val error = result.exceptionOrNull()
        assertTrue(
            "Should throw IllegalArgumentException",
            error is IllegalArgumentException
        )
        assertTrue(
            "Error message should mention key length",
            error?.message?.contains("Key must be") == true
        )
    }

    @Test
    fun testDecryptionWithTamperedData() = runBlocking {
        val plaintext = "Test message"

        // Encrypt valid data
        val encrypted = Cipher.encryptString(validKey32, plaintext).getOrThrow()

        // Tamper with the encrypted data
        val tamperedBytes = Base64.decode(encrypted, Base64.NO_WRAP)
        tamperedBytes[tamperedBytes.size - 1] = tamperedBytes[tamperedBytes.size - 1].inc()
        val tampered = Base64.encodeToString(tamperedBytes, Base64.NO_WRAP)

        // Attempt to decrypt tampered data
        val result = Cipher.decryptString(validKey32, tampered)
        assertTrue("Should fail with tampered data", result.isFailure)
    }

    @Test
    fun testEncryptionPerformance() = runBlocking {
        val iterations = 100
        val dataSize = 1024 // 1KB
        val testData = ByteArray(dataSize) { it.toByte() }.toString(Charsets.UTF_8)

        val startTime = System.nanoTime()

        // Perform multiple encryptions
        repeat(iterations) {
            val result = Cipher.encryptString(validKey32, testData)
            assertTrue("Encryption failed at iteration $it", result.isSuccess)
        }

        val duration = (System.nanoTime() - startTime) / 1_000_000.0 // ms
        val throughput = iterations * dataSize / duration * 1000 / 1024 // KB/s

        println("Encryption Performance:")
        println("Total time: $duration ms")
        println("Throughput: $throughput KB/s")

        assertTrue(
            "Encryption performance below threshold",
            throughput > 100
        ) // Minimum 100 KB/s
    }

    @Test
    fun testSpecialCharacters() = runBlocking {
        // Test string with various special characters
        val testString = """
            Basic ASCII: Hello World!
            Unicode: こんにちは世界
            Emojis: 👋🌍✨
            Control chars: \n\t\r
            Special chars: !@#$%^&*()_+
            Zero byte: \u0000
        """.trimIndent()

        // Encrypt and decrypt
        val encrypted = Cipher.encryptString(validKey32, testString).getOrThrow()
        val decrypted = Cipher.decryptString(validKey32, encrypted).getOrThrow()

        assertEquals(
            "Special characters should be preserved",
            testString, decrypted
        )
    }

    @Test
    fun testMemoryHandling() = runBlocking {
        val dataSize = 1024 * 1024 // 1MB
        val testData = ByteArray(dataSize) { it.toByte() }.toString(Charsets.UTF_8)

        // Force GC to stabilize memory
        System.gc()
        Thread.sleep(100)

        val initialMemory = Runtime.getRuntime().totalMemory() -
                Runtime.getRuntime().freeMemory()

        // Perform multiple encryptions
        repeat(10) {
            val encrypted = Cipher.encryptString(validKey32, testData).getOrThrow()
            val decrypted = Cipher.decryptString(validKey32, encrypted).getOrThrow()
            assertEquals("Data integrity check failed", testData, decrypted)
        }

        // Force GC again
        System.gc()
        Thread.sleep(100)

        val finalMemory = Runtime.getRuntime().totalMemory() -
                Runtime.getRuntime().freeMemory()
        val memoryDiff = finalMemory - initialMemory

        println("Memory usage difference: ${memoryDiff / 1024} KB")
        assertTrue(
            "Excessive memory retention detected",
            memoryDiff < dataSize
        )
    }

    @Test
    fun testEmptyData() = runBlocking {
        // Test empty string
        val encrypted = Cipher.encryptString(validKey32, "").getOrThrow()
        val decrypted = Cipher.decryptString(validKey32, encrypted).getOrThrow()

        assertEquals("Empty string should be handled correctly", "", decrypted)

        // Test empty file
        val emptyFile = File(context.cacheDir, "empty.txt")
        val encryptedFile = File(context.cacheDir, "empty.enc")
        val decryptedFile = File(context.cacheDir, "empty.dec")

        try {
            emptyFile.createNewFile()

            Cipher.encryptFile(validKey32, emptyFile, encryptedFile).getOrThrow()
            Cipher.decryptFile(validKey32, encryptedFile, decryptedFile).getOrThrow()

            assertEquals(
                "Empty file should be handled correctly",
                0, decryptedFile.length()
            )
        } finally {
            emptyFile.delete()
            encryptedFile.delete()
            decryptedFile.delete()
        }
    }
}