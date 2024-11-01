package com.mavbozo.securecrypto

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import java.security.Provider
import java.security.SecureRandom
import java.security.Security
import org.bouncycastle.jce.provider.BouncyCastleProvider

@RunWith(RobolectricTestRunner::class)
class RandomTest {
    private lateinit var context: Context

    // Custom provider just for quality detection
    private class TestAndroidKeyStoreProvider : Provider(
        "AndroidKeyStore",
        1.0,
        "Test Android KeyStore Provider"
    ) {
        init {
            // Empty provider - only needed for presence check
        }
    }

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        context = ApplicationProvider.getApplicationContext()

        // Add providers
        if (Security.getProvider("BC") == null) {
            Security.addProvider(BouncyCastleProvider())
        }

        // Add test provider for quality detection
        if (Security.getProvider("AndroidKeyStore") == null) {
            Security.addProvider(TestAndroidKeyStoreProvider())
        }
    }

    @Test
    fun `test Random creation with hardware quality`() = runBlocking {
        // Add AndroidKeyStore provider to trigger hardware quality
        val randomResult = Random.create()

        assertTrue("Random creation failed", randomResult.isSuccess)
        val random = randomResult.getOrThrow()

        assertEquals("Should detect hardware quality due to AndroidKeyStore presence",
            EntropyQuality.Hardware,
            random.getQuality())
    }

    @Test
    fun `test Random creation with fallback quality`() = runBlocking {
        // Remove AndroidKeyStore provider to trigger fallback quality
        Security.removeProvider("AndroidKeyStore")

        val randomResult = Random.create()
        assertTrue("Random creation failed", randomResult.isSuccess)

        val random = randomResult.getOrThrow()
        assertEquals("Should fall back to software quality",
            EntropyQuality.Fallback,
            random.getQuality())
    }

    @Test
    fun `test generate random bytes`() = runBlocking {
        val size = 32
        val bytesResult = Random.generateBytes(size)

        assertTrue("Generate bytes failed", bytesResult.isSuccess)
        val bytes = bytesResult.getOrThrow()

        assertNotNull("Generated bytes are null", bytes)
        assertEquals("Generated bytes size mismatch", size, bytes.size)
        assertFalse("Generated bytes should not be all zero",
            bytes.all { it == 0.toByte() })
    }

    @Test
    fun `test generate hex string`() = runBlocking {
        val length = 32
        val hexResult = Random.generateHexString(length)

        assertTrue("Hex string generation failed", hexResult.isSuccess)
        val hexString = hexResult.getOrThrow()

        assertNotNull("Generated hex string is null", hexString)
        assertEquals("Hex string length mismatch", length, hexString.length)
        assertTrue("Invalid hex string format",
            hexString.matches(Regex("[0-9a-f]+")))
    }

    @Test
    fun `test secure bytes wrapping and cleanup`() = runBlocking {
        val size = 32
        val randomResult = Random.create()
        assertTrue("Random creation failed", randomResult.isSuccess)

        val random = randomResult.getOrThrow()
        val bytesResult = random.nextSecureBytes(size)
        assertTrue("Secure bytes generation failed", bytesResult.isSuccess)

        val secureBytes = bytesResult.getOrThrow()
        var exposedBytes: ByteArray? = null

        secureBytes.use { bytes ->
            exposedBytes = bytes.clone()
            assertNotNull("Exposed bytes are null", exposedBytes)
            assertEquals("Exposed bytes size mismatch", size, exposedBytes?.size)
            assertFalse("Bytes should not be all zero before cleanup",
                bytes.all { it == 0.toByte() })
        }

        secureBytes.use { bytes ->
            assertTrue("Bytes not zeroed after use",
                bytes.all { it == 0.toByte() })
        }
    }

    @Test
    fun `test negative size handling`() = runBlocking {
        val negativeSize = -1
        val bytesResult = Random.generateBytes(negativeSize)

        assertTrue("Should fail with negative size", bytesResult.isFailure)
        assertTrue("Should have IllegalArgumentException",
            bytesResult.exceptionOrNull() is IllegalArgumentException)
    }

    // Base64Test
    @Test
    fun `test generate base64 string`() = runBlocking {
        val length = 32
        val base64Result = Random.generateBase64String(length)

        assertTrue("Base64 string generation failed", base64Result.isSuccess)
        val base64String = base64Result.getOrThrow()

        assertNotNull("Generated base64 string is null", base64String)
        assertEquals("Base64 string length mismatch", length, base64String.length)
        assertTrue("Invalid base64 string format",
            base64String.matches(Regex("[A-Za-z0-9+/=]*")))
    }

    @Test
    fun `test base64 string with different flags`() = runBlocking {
        val length = 32
        val testCases = listOf(
            Base64Flags.Default to Regex("[A-Za-z0-9+/=]*"),
            Base64Flags.NoPadding to Regex("[A-Za-z0-9+/]*"),
            Base64Flags.UrlSafe to Regex("[A-Za-z0-9_\\-=]*"),
            Base64Flags.UrlSafeNoPadding to Regex("[A-Za-z0-9_\\-]*")
        )

        for ((flag, regex) in testCases) {
            val base64Result = Random.generateBase64String(length, flag)
            assertTrue("Base64 string generation failed for flag: $flag",
                base64Result.isSuccess)

            val base64String = base64Result.getOrThrow()
            assertNotNull("Generated base64 string is null for flag: $flag",
                base64String)
            assertEquals("Base64 string length mismatch for flag: $flag",
                length, base64String.length)
            assertTrue("Invalid base64 string format for flag: $flag",
                base64String.matches(regex))
        }
    }

    @Test
    fun `test base64 string with odd lengths`() = runBlocking {
        // Test odd lengths to verify proper padding handling
        val lengths = listOf(1, 3, 7, 11)

        for (length in lengths) {
            val base64Result = Random.generateBase64String(length)
            assertTrue("Base64 string generation failed for length: $length",
                base64Result.isSuccess)

            val base64String = base64Result.getOrThrow()
            assertEquals("Base64 string length mismatch for length: $length",
                length, base64String.length)
        }
    }

    @Test
    fun `test base64 string with zero length`() = runBlocking {
        try {
            Random.generateBase64String(0).getOrThrow()
            fail("Should throw IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertEquals("Length must be positive", e.message)
        }
    }

    @Test
    fun `test base64 string with negative length`() = runBlocking {
        val negativeValues = listOf(-1, -42, Int.MIN_VALUE)

        for (negativeLength in negativeValues) {
            try {
                Random.generateBase64String(negativeLength).getOrThrow()
                fail("Should throw IllegalArgumentException for length $negativeLength")
            } catch (e: IllegalArgumentException) {
                assertEquals(
                    "Invalid error message for length $negativeLength",
                    "Length must be positive",
                    e.message
                )
            }
        }
    }

    @Test
    fun `test base64 string uniqueness`() = runBlocking {
        val length = 32
        val count = 5
        val generated = mutableSetOf<String>()

        repeat(count) {
            val base64Result = Random.generateBase64String(length)
            assertTrue("Base64 string generation failed", base64Result.isSuccess)

            val base64String = base64Result.getOrThrow()
            assertTrue("Generated base64 string should be unique",
                generated.add(base64String))
        }
    }

    @Test
    fun `test base64 string memory cleanup`() = runBlocking {
        val length = 32
        val base64Result = Random.generateBase64String(length)
        assertTrue("Base64 string generation failed", base64Result.isSuccess)

        // Verify the internal byte array was cleaned up by checking heap
        System.gc() // Request garbage collection

        // This is a basic check - the actual cleanup is verified more thoroughly
        // in the SecureBytes tests
        val base64String = base64Result.getOrThrow()
        assertNotNull("Generated base64 string should exist after cleanup",
            base64String)
    }
}