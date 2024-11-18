package com.mavbozo.androidsecurecrypto

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import android.util.Base64
import java.io.File
import kotlin.random.Random

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class CipherTest {
    private val validKey32 = ByteArray(32) { it.toByte() }
    private val invalidKey = ByteArray(24) { it.toByte() }

    @Test
    fun `create - with default format succeeds`() = runBlocking {
        val result = Cipher.create()
        assertTrue(result.isSuccess)
    }

    @Test
    fun `create - with explicit AesGcmFormat succeeds`() = runBlocking {
        val result = Cipher.create(AesGcmFormat)
        assertTrue(result.isSuccess)
    }

    @Test
    fun `encryptString - with valid key succeeds`(): Unit = runBlocking {
        // Given
        val plaintext = "Hello, World!"

        // When
        val result = Cipher.encryptString(validKey32, plaintext)

        // Then
        assertTrue(result.isSuccess)
        val ciphertext = result.getOrNull()
        assertNotNull(ciphertext)

        // Verify it's valid Base64
        try {
            Base64.decode(ciphertext, Base64.NO_WRAP)
        } catch (e: IllegalArgumentException) {
            fail("Ciphertext is not valid Base64: ${e.message}")
        }
    }


    @Test
    fun `encryptString - with invalid key fails`() = runBlocking {
        // Given
        val plaintext = "Hello, World!"

        // When
        val result = Cipher.encryptString(invalidKey, plaintext)

        // Then
        assertTrue(result.isFailure)
        result.exceptionOrNull()?.let { exception ->
            assertTrue(
                "Exception should be IllegalArgumentException",
                exception is IllegalArgumentException
            )
            assertNotNull(
                "Exception message should not be null",
                exception.message
            )
            assertTrue(
                "Exception message should mention key length",
                exception.message?.contains("Key must be") == true
            )
        } ?: fail("Exception should not be null")
    }

    @Test
    fun `encryptString - produces different ciphertexts for same plaintext`() = runBlocking {
        // Given
        val plaintext = "Hello, World!"

        // When
        val result1 = Cipher.encryptString(validKey32, plaintext)
        val result2 = Cipher.encryptString(validKey32, plaintext)

        // Then
        assertTrue(result1.isSuccess && result2.isSuccess)
        assertNotEquals(
            "Ciphertexts should be different due to random IV",
            result1.getOrNull(),
            result2.getOrNull()
        )
    }

    @Test
    fun `decrypt - succeeds with matching key`() = runBlocking {
        // Given
        val plaintext = "Hello, World!"
        val encrypted = Cipher.encryptString(validKey32, plaintext).getOrThrow()

        // When
        val decrypted = Cipher.decryptString(validKey32, encrypted)

        // Then
        assertTrue(decrypted.isSuccess)
        assertEquals(plaintext, decrypted.getOrThrow())
    }

    @Test
    fun `decrypt - fails with wrong key`() = runBlocking {
        // Given
        val plaintext = "Hello, World!"
        val encrypted = Cipher.encryptString(validKey32, plaintext).getOrThrow()
        val wrongKey = ByteArray(32) { (it + 1).toByte() }

        // When
        val decrypted = Cipher.decryptString(wrongKey, encrypted)

        // Then
        assertTrue(decrypted.isFailure)
        val exception = decrypted.exceptionOrNull()
        assertNotNull(exception)
    }

    @Test
    fun `decrypt - fails with tampered ciphertext`() = runBlocking {
        // Given
        val plaintext = "Hello, World!"
        val encrypted = Cipher.encryptString(validKey32, plaintext).getOrThrow()
        val tamperedBytes = Base64.decode(encrypted, Base64.NO_WRAP)
        // Tamper with the last byte of the ciphertext
        tamperedBytes[tamperedBytes.size - 1] = tamperedBytes[tamperedBytes.size - 1].inc()
        val tampered = Base64.encodeToString(tamperedBytes, Base64.NO_WRAP)

        // When
        val decrypted = Cipher.decryptString(validKey32, tampered)

        // Then
        assertTrue(decrypted.isFailure)
        val exception = decrypted.exceptionOrNull()
        assertNotNull(exception)
    }

    @Test
    fun `encrypt decrypt - handles empty string`() = runBlocking {
        // Given
        val plaintext = ""

        // When
        val encrypted = Cipher.encryptString(validKey32, plaintext).getOrThrow()
        val decrypted = Cipher.decryptString(validKey32, encrypted).getOrThrow()

        // Then
        assertEquals(plaintext, decrypted)
    }

    @Test
    fun `encrypt decrypt - handles special characters`() = runBlocking {
        // Given
        val plaintext = "Hello, 世界! 🌍 \u0000\n\t"

        // When
        val encrypted = Cipher.encryptString(validKey32, plaintext).getOrThrow()
        val decrypted = Cipher.decryptString(validKey32, encrypted).getOrThrow()

        // Then
        assertEquals(plaintext, decrypted)
    }

    @Test
    fun `encrypt decrypt - handles long text`() = runBlocking {
        // Given
        val plaintext = "A".repeat(1000)

        // When
        val encrypted = Cipher.encryptString(validKey32, plaintext).getOrThrow()
        val decrypted = Cipher.decryptString(validKey32, encrypted).getOrThrow()

        // Then
        assertEquals(plaintext, decrypted)
    }


    // Add these test methods to CipherTest class
    @Test
    fun `encryptFile - with valid file succeeds`() = runBlocking {
        // Given
        val plaintext = "Hello, World!"
        val sourceFile = File.createTempFile("test", ".txt").apply {
            writeText(plaintext)
            deleteOnExit()
        }
        val destFile = File.createTempFile("test", ".enc").apply {
            deleteOnExit()
        }

        // When
        val result = Cipher.encryptFile(validKey32, sourceFile, destFile)

        // Then
        assertTrue(result.isSuccess)
        assertTrue(destFile.exists())
        assertTrue(destFile.length() > 0)
    }

    @Test
    fun `decryptFile - recovers original content`() = runBlocking {
        // Given
        val plaintext = "Hello, World!"
        val sourceFile = File.createTempFile("test", ".txt").apply {
            writeText(plaintext)
            deleteOnExit()
        }
        val encryptedFile = File.createTempFile("test", ".enc").apply {
            deleteOnExit()
        }
        val decryptedFile = File.createTempFile("test", ".dec").apply {
            deleteOnExit()
        }

        // When
        Cipher.encryptFile(validKey32, sourceFile, encryptedFile).getOrThrow()
        Cipher.decryptFile(validKey32, encryptedFile, decryptedFile).getOrThrow()

        // Then
        assertEquals(plaintext, decryptedFile.readText())
    }

    @Test
    fun `encryptFile - fails with file larger than 10MB`() = runBlocking {
        // Given
        val largeFile = File.createTempFile("test", ".large").apply {
            // Create 11MB file
            outputStream().use { out ->
                repeat(11 * 1024) { // 11MB in KB chunks
                    out.write(ByteArray(1024) { Random.nextBytes(1)[0] })
                }
            }
            deleteOnExit()
        }
        val destFile = File.createTempFile("test", ".enc").apply {
            deleteOnExit()
        }

        // When
        val result = Cipher.encryptFile(validKey32, largeFile, destFile)

        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is IllegalArgumentException)
        assertTrue(exception?.message?.contains("exceeds maximum allowed size") == true)
    }

    @Test
    fun `decryptFile - fails with wrong key`() = runBlocking {
        // Given
        val plaintext = "Hello, World!"
        val sourceFile = File.createTempFile("test", ".txt").apply {
            writeText(plaintext)
            deleteOnExit()
        }
        val encryptedFile = File.createTempFile("test", ".enc").apply {
            deleteOnExit()
        }
        val decryptedFile = File.createTempFile("test", ".dec").apply {
            deleteOnExit()
        }
        val wrongKey = ByteArray(32) { (it + 1).toByte() }

        // When
        Cipher.encryptFile(validKey32, sourceFile, encryptedFile).getOrThrow()
        val result = Cipher.decryptFile(wrongKey, encryptedFile, decryptedFile)

        // Then
        assertTrue(result.isFailure)
    }

    @Test
    fun `encryptFile - handles empty file`() = runBlocking {
        // Given
        val sourceFile = File.createTempFile("test", ".txt").apply {
            writeText("")
            deleteOnExit()
        }
        val encryptedFile = File.createTempFile("test", ".enc").apply {
            deleteOnExit()
        }
        val decryptedFile = File.createTempFile("test", ".dec").apply {
            deleteOnExit()
        }

        // When
        Cipher.encryptFile(validKey32, sourceFile, encryptedFile).getOrThrow()
        Cipher.decryptFile(validKey32, encryptedFile, decryptedFile).getOrThrow()

        // Then
        assertEquals("", decryptedFile.readText())
    }

    @Test
    fun `encryptFile - handles binary data`() = runBlocking {
        // Given
        val binaryData = ByteArray(1024) { it.toByte() }
        val sourceFile = File.createTempFile("test", ".bin").apply {
            writeBytes(binaryData)
            deleteOnExit()
        }
        val encryptedFile = File.createTempFile("test", ".enc").apply {
            deleteOnExit()
        }
        val decryptedFile = File.createTempFile("test", ".dec").apply {
            deleteOnExit()
        }

        // When
        Cipher.encryptFile(validKey32, sourceFile, encryptedFile).getOrThrow()
        Cipher.decryptFile(validKey32, encryptedFile, decryptedFile).getOrThrow()

        // Then
        assertTrue(binaryData.contentEquals(decryptedFile.readBytes()))
    }

    @Test
    fun `decryptFile - fails with tampered file`() = runBlocking {
        // Given
        val plaintext = "Hello, World!"
        val sourceFile = File.createTempFile("test", ".txt").apply {
            writeText(plaintext)
            deleteOnExit()
        }
        val encryptedFile = File.createTempFile("test", ".enc").apply {
            deleteOnExit()
        }
        val decryptedFile = File.createTempFile("test", ".dec").apply {
            deleteOnExit()
        }

        // When
        Cipher.encryptFile(validKey32, sourceFile, encryptedFile).getOrThrow()

        // Tamper with the encrypted file
        val tamperedBytes = encryptedFile.readBytes()
        tamperedBytes[tamperedBytes.size - 1] = tamperedBytes[tamperedBytes.size - 1].inc()
        encryptedFile.writeBytes(tamperedBytes)

        val result = Cipher.decryptFile(validKey32, encryptedFile, decryptedFile)

        // Then
        assertTrue(result.isFailure)
    }
}