package com.mavbozo.androidsecurecrypto

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.nio.ByteBuffer

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class CipherFormatTest {
    // AesGcmFormat Tests
    @Test
    fun `AesGcmFormat parameters are correct`() {
        assertEquals(0x01.toByte(), AesGcmFormat.id)
        assertEquals(16, AesGcmFormat.paramsLength) // 12 bytes IV + 4 bytes tag length
        assertEquals(32, AesGcmFormat.keySize)
    }

    @Test
    fun `AesGcmFormat creates valid parameters`() {
        // When
        val params = AesGcmFormat.createParams()

        // Then
        assertEquals(AesGcmFormat.paramsLength, params.size)

        // Extract IV and tag length
        val buffer = ByteBuffer.wrap(params)
        val iv = ByteArray(12)
        buffer.get(iv)
        val tagLength = buffer.getInt()

        assertEquals(128, tagLength) // Verify tag length is 128 bits
    }

    @Test
    fun `AesGcmFormat generates unique IVs`() {
        // When
        val params1 = AesGcmFormat.createParams()
        val params2 = AesGcmFormat.createParams()

        // Then
        assertNotEquals(
            params1.sliceArray(0..11).toList(),
            params2.sliceArray(0..11).toList()
        )
    }

    @Test
    fun `AesGcmFormat validates parameter length`() {
        // Valid case
        AesGcmFormat.validateParams(ByteArray(16))

        // Invalid case
        assertThrows(IllegalArgumentException::class.java) {
            AesGcmFormat.validateParams(ByteArray(15))
        }
    }
}