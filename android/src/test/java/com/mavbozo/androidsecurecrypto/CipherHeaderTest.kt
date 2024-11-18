package com.mavbozo.androidsecurecrypto

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.nio.ByteBuffer

@Config(manifest=Config.NONE)
@RunWith(RobolectricTestRunner::class)
class CipherHeaderTest {
    @Test
    fun `create header produces valid format`() {
        // When
        val header = CipherHeader.create(AesGcmFormat)
        val encoded = header.encode()

        // Then
        // Check magic bytes
        assertEquals(
            "SECB",
            encoded.sliceArray(0..3).toString(Charsets.US_ASCII)
        )

        // Check version
        assertEquals(0x01.toByte(), encoded[4])

        // Check algorithm ID
        assertEquals(AesGcmFormat.id, encoded[5])

        // Check params length
        val paramsLength = ByteBuffer.wrap(encoded.sliceArray(6..7))
            .short.toInt() and 0xFFFF
        assertEquals(AesGcmFormat.paramsLength, paramsLength)
    }

    @Test
    fun `parse header correctly reads valid format`() {
        // Given
        val originalHeader = CipherHeader.create(AesGcmFormat)
        val encoded = originalHeader.encode()

        // When
        val parsed = CipherHeader.parse(ByteBuffer.wrap(encoded))

        // Then
        assertEquals(AesGcmFormat.id, parsed.algorithm.id)
        assertEquals(AesGcmFormat.paramsLength, parsed.params.size)
    }

    @Test
    fun `parse header fails with invalid magic bytes`() {
        // Given
        val buffer = ByteBuffer.allocate(8 + AesGcmFormat.paramsLength)
        buffer.put("INVL".toByteArray(Charsets.US_ASCII))
        buffer.put(0x01.toByte()) // version
        buffer.put(AesGcmFormat.id)
        buffer.putShort(AesGcmFormat.paramsLength.toShort())
        buffer.put(ByteArray(AesGcmFormat.paramsLength))
        buffer.flip() // Important: flip buffer before reading

        // Then
        assertThrows(IllegalArgumentException::class.java) {
            CipherHeader.parse(buffer)
        }
    }

    @Test
    fun `parse header fails with unsupported version`() {
        // Given
        val buffer = ByteBuffer.allocate(8 + AesGcmFormat.paramsLength)
        buffer.put("SECB".toByteArray(Charsets.US_ASCII))
        buffer.put(0xFF.toByte()) // invalid version
        buffer.put(AesGcmFormat.id)
        buffer.putShort(AesGcmFormat.paramsLength.toShort())
        buffer.put(ByteArray(AesGcmFormat.paramsLength))
        buffer.flip()

        // Then
        assertThrows(IllegalArgumentException::class.java) {
            CipherHeader.parse(buffer)
        }
    }

    @Test
    fun `parse header fails with invalid algorithm ID`() {
        // Given
        val buffer = ByteBuffer.allocate(8 + AesGcmFormat.paramsLength)
        buffer.put("SECB".toByteArray(Charsets.US_ASCII))
        buffer.put(0x01.toByte())
        buffer.put(0xFF.toByte()) // invalid algorithm ID
        buffer.putShort(AesGcmFormat.paramsLength.toShort())
        buffer.put(ByteArray(AesGcmFormat.paramsLength))
        buffer.flip()

        // Then
        assertThrows(IllegalArgumentException::class.java) {
            CipherHeader.parse(buffer)
        }
    }

    @Test
    fun `parse header fails with mismatched params length`() {
        // Given
        val buffer = ByteBuffer.allocate(8 + 32)
        buffer.put("SECB".toByteArray(Charsets.US_ASCII))
        buffer.put(0x01.toByte())
        buffer.put(AesGcmFormat.id)
        buffer.putShort(32.toShort()) // wrong length
        buffer.put(ByteArray(32))
        buffer.flip()

        // Then
        assertThrows(IllegalArgumentException::class.java) {
            CipherHeader.parse(buffer)
        }
    }
}