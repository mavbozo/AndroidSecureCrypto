package com.mavbozo.securecrypto

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

@RunWith(AndroidJUnit4::class)
class EnhancedRandomInstrumentedTest {
    private lateinit var context: Context

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
    }

    // Basic Functionality Tests
    @Test
    fun testBasicRandomGeneration() = runBlocking {
        // First, test creation
        val randomResult = EnhancedRandom.create(context)
        assertTrue("Random creation failed: ${randomResult.exceptionOrNull()}",
            randomResult.isSuccess)

        val random = randomResult.getOrThrow()
        val size = 32

        // Then test generation with error logging
        val result = random.nextBytes(size)
        assertTrue("Random generation failed: ${result.exceptionOrNull()}",
            result.isSuccess)

        // Only proceed if we succeeded
        val bytes = result.getOrThrow()
        assertEquals("Generated bytes size mismatch", size, bytes.size)

        // Basic entropy check
        val allZeros = bytes.all { it == 0.toByte() }
        val allOnes = bytes.all { it == (-1).toByte() }
        assertFalse("Generated bytes should not be all zeros", allZeros)
        assertFalse("Generated bytes should not be all ones", allOnes)
    }

    @Test
    fun testEnhancedHexStringGeneration() = runBlocking {
        val length = 32 // We want a 32-character hex string

        val result = EnhancedRandom.generateEnhancedHexString(context, length)
        assertTrue("Hex string generation failed: ${result.exceptionOrNull()}",
            result.isSuccess)

        val hexString = result.getOrThrow()
        assertEquals("Generated hex string length mismatch", length, hexString.length)
        assertTrue("Invalid hex string format",
            hexString.matches(Regex("[0-9a-f]+")))
    }

    @Test
    fun testEntropyQualityReporting() = runBlocking {
        val random = EnhancedRandom.create(context).getOrThrow()

        // Since we're using MasterKey and platform SecureRandom,
        // we should always get Hardware quality on modern Android devices
        val quality = random.getQuality()
        assertEquals("Should report hardware-backed entropy",
            EntropyQuality.Hardware, quality)
    }

    @Test
    fun testDistinctSequences() = runBlocking {
        val random = EnhancedRandom.create(context).getOrThrow()
        val size = 32

        // Generate two sequences of the same size
        val sequence1 = random.nextBytes(size).getOrThrow()
        val sequence2 = random.nextBytes(size).getOrThrow()

        // They should be different
        assertFalse("Consecutive generations should produce different values",
            sequence1.contentEquals(sequence2))
    }

    @Test
    fun testNegativeSizeHandling() = runBlocking {
        val random = EnhancedRandom.create(context).getOrThrow()

        val result = random.nextBytes(-1)
        assertTrue("Should fail with negative size", result.isFailure)
        assertTrue("Should have IllegalArgumentException",
            result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun testSecureBytesZeroization() = runBlocking {
        val random = EnhancedRandom.create(context).getOrThrow()
        val size = 32

        val bytesResult = random.nextSecureBytes(size)
        assertTrue("Secure bytes generation failed", bytesResult.isSuccess)

        val secureBytes = bytesResult.getOrThrow()

        // First use should give us non-zero data
        var allZerosFirst = true
        secureBytes.use { bytes ->
            allZerosFirst = bytes.all { it == 0.toByte() }
            assertFalse("Initial bytes should not be all zeros", allZerosFirst)
        }

        // Second use should give us zeroed data due to cleanup
        secureBytes.use { bytes ->
            assertTrue("Bytes should be zeroed after first use",
                bytes.all { it == 0.toByte() })
        }
    }

    @Test
    fun testDifferentSizes() = runBlocking {
        val random = EnhancedRandom.create(context).getOrThrow()

        // Test various sizes
        val sizes = listOf(1, 16, 64, 1024)

        for (size in sizes) {
            val result = random.nextBytes(size)
            assertTrue("Failed to generate $size bytes", result.isSuccess)

            val bytes = result.getOrThrow()
            assertEquals("Generated size mismatch for request of $size bytes",
                size, bytes.size)

            // Basic non-zero check
            assertFalse("Generated bytes should not be all zeros for size $size",
                bytes.all { it == 0.toByte() })
        }
    }

    @Test
    fun testSequentialHexStrings() = runBlocking {
        val length = 32
        // Generate multiple hex strings and verify they're all different
        val hexStrings = List(3) {
            EnhancedRandom.generateEnhancedHexString(context, length).getOrThrow()
        }

        // Verify all strings are different
        val uniqueStrings = hexStrings.toSet()
        assertEquals("Sequential hex strings should all be different",
            hexStrings.size, uniqueStrings.size)

        // Verify they're all valid hex strings of correct length
        for (hexString in hexStrings) {
            assertEquals("Hex string length incorrect", length, hexString.length)
            assertTrue("Invalid hex string format",
                hexString.matches(Regex("[0-9a-f]+")))
        }
    }

    @Test
    fun testZeroLengthHandling() = runBlocking {
        val random = EnhancedRandom.create(context).getOrThrow()

        // Test both bytes and hex string generation with zero length
        val bytesResult = random.nextBytes(0)
        assertTrue("Should fail with zero size", bytesResult.isFailure)
        assertTrue("Should have IllegalArgumentException for bytes",
            bytesResult.exceptionOrNull() is IllegalArgumentException)

        val hexResult = EnhancedRandom.generateEnhancedHexString(context, 0)
        assertTrue("Should fail with zero length", hexResult.isFailure)
        assertTrue("Should have IllegalArgumentException for hex",
            hexResult.exceptionOrNull() is IllegalArgumentException)
    }

    // Statistical Quality Tests
    @Test
    fun testByteDistribution() = runBlocking {
        val random = EnhancedRandom.create(context).getOrThrow()
        val size = 25600

        val bytes = random.nextBytes(size).getOrThrow()
        val frequencies = IntArray(256) { 0 }

        bytes.forEach { byte ->
            frequencies[byte.toInt() and 0xFF]++
        }

        // More detailed statistics
        val mean = frequencies.average()
        val stdDev = sqrt(frequencies.map { (it - mean) * (it - mean) }.average())
        val min = frequencies.minOrNull() ?: 0
        val max = frequencies.maxOrNull() ?: 0
        val expectedFreq = size / 256.0

        println("""
        Byte Distribution Statistics:
        ---------------------------
        Sample size: $size
        Expected frequency per byte: $expectedFreq
        Actual frequencies:
        - Mean: $mean
        - Std Dev: $stdDev
        - Min: $min
        - Max: $max
        - Max/Min ratio: ${if (min > 0) max.toDouble() / min else "infinity"}
    """.trimIndent())

        val chiSquare = frequencies.sumOf { freq ->
            val diff = freq - expectedFreq
            (diff * diff) / expectedFreq
        }

        println("Chi-square value: $chiSquare")

        // More reasonable thresholds for practical randomness
        assertTrue("Distribution fails uniformity test (chi-square = $chiSquare)",
            chiSquare < 336.0)
        assertTrue("Some byte values appear too infrequently",
            min > expectedFreq * 0.5)
        assertTrue("Some byte values appear too frequently",
            max < expectedFreq * 1.5)
    }

    private fun sqrt(x: Double): Double = kotlin.math.sqrt(x)

    @Test
    fun testBitCorrelation() = runBlocking {
        val random = EnhancedRandom.create(context).getOrThrow()
        val size = 1000

        // Generate two consecutive sequences
        val bytes1 = random.nextBytes(size).getOrThrow()
        val bytes2 = random.nextBytes(size).getOrThrow()

        // Calculate bit correlations
        var correlationCount = 0
        val totalBits = size * 8

        for (i in 0 until size) {
            for (bit in 0 until 8) {
                val bit1 = (bytes1[i].toInt() shr bit) and 1
                val bit2 = (bytes2[i].toInt() shr bit) and 1
                if (bit1 == bit2) correlationCount++
            }
        }

        val correlationRatio = correlationCount.toDouble() / totalBits
        println("""
        Bit Correlation Analysis:
        ------------------------
        Total bits compared: $totalBits
        Matching bits: $correlationCount
        Correlation ratio: $correlationRatio
        (Expected ~0.5 for random data)
    """.trimIndent())

        // For truly random bits, correlation should be close to 0.5
        assertTrue("Bit correlation too high: $correlationRatio",
            correlationRatio in 0.45..0.55)
    }

    // Performance Tests
    @Test
    fun testHighVolumeGeneration() = runBlocking {
        val random = EnhancedRandom.create(context).getOrThrow()
        val iterations = 100
        val size = 1024 // 1KB each

        val startTime = System.nanoTime()

        // Generate multiple blocks in parallel
        val results = (1..iterations).map {
            random.nextBytes(size)
        }

        val duration = (System.nanoTime() - startTime) / 1_000_000.0 // Convert to ms
        val totalBytes = iterations * size

        println("""
        High Volume Generation Test:
        --------------------------
        Total bytes: $totalBytes
        Iterations: $iterations
        Duration: $duration ms
        Throughput: ${totalBytes / duration * 1000 / 1024} KB/s
    """.trimIndent())

        // Verify all generations succeeded
        assertTrue("All generations should succeed",
            results.all { it.isSuccess })

        // Check that we maintain good performance
        assertTrue("Generation too slow: $duration ms",
            duration < 5000) // Should complete within 5 seconds
    }

    @Test
    fun testMemoryCleanup() = runBlocking {
        val random = EnhancedRandom.create(context).getOrThrow()
        val size = 1024

        // Get initial memory state
        Runtime.getRuntime().gc()
        val initialMemory = Runtime.getRuntime().totalMemory() -
                Runtime.getRuntime().freeMemory()

        // Generate and immediately clear many secure bytes
        repeat(1000) {
            random.nextSecureBytes(size).getOrThrow().use { bytes ->
                // Use bytes and verify they're cleared
                assertFalse("Bytes should not be all zero initially",
                    bytes.all { it == 0.toByte() })
            }
        }

        // Check memory usage
        Runtime.getRuntime().gc()
        val finalMemory = Runtime.getRuntime().totalMemory() -
                Runtime.getRuntime().freeMemory()

        // Memory usage shouldn't grow significantly
        val memoryDiff = finalMemory - initialMemory
        println("Memory usage difference: ${memoryDiff / 1024} KB")

        assertTrue("Memory leak detected: $memoryDiff bytes",
            memoryDiff < size * 2) // Allow for some overhead
    }

    @Test
    fun testConcurrentAccess() = runBlocking {
        val random = EnhancedRandom.create(context).getOrThrow()
        val size = 32
        val iterations = 100

        // Generate random bytes concurrently
        val results = coroutineScope {
            List(iterations) {
                async(Dispatchers.Default) {
                    random.nextBytes(size)
                }
            }.awaitAll()
        }

        // Verify all operations succeeded
        assertTrue("All concurrent operations should succeed",
            results.all { it.isSuccess })

        // Verify all results are different
        val uniqueResults = results.map {
            it.getOrThrow().toList()
        }.toSet()

        assertTrue("Concurrent generations should produce unique results",
            uniqueResults.size == iterations)
    }

    // Base64 Generation Test
    @Test
    fun testEnhancedBase64Generation() = runBlocking {
        val length = 24 // Standard Base64 length for 18 bytes

        val result = EnhancedRandom.generateEnhancedBase64String(context, length)
        assertTrue("Base64 generation failed: ${result.exceptionOrNull()}",
            result.isSuccess)

        val base64String = result.getOrThrow()
        assertEquals("Generated Base64 string length mismatch",
            length, base64String.length)

        // Verify it's valid Base64
        assertTrue("Invalid Base64 format",
            isValidBase64(base64String))
    }

    @Test
    fun testBase64Variants() = runBlocking {
        val length = 24
        val variants = listOf(
            Base64Flags.Default,
            Base64Flags.NoPadding,
            Base64Flags.UrlSafe,
            Base64Flags.UrlSafeNoPadding
        )

        for (flags in variants) {
            val result = EnhancedRandom.generateEnhancedBase64String(
                context, length, flags
            )
            assertTrue("Generation failed for $flags: ${result.exceptionOrNull()}",
                result.isSuccess)

            val base64String = result.getOrThrow()
            assertEquals("Length mismatch for $flags",
                length, base64String.length)

            when (flags) {
                is Base64Flags.Default -> {
                    assertTrue("Invalid standard Base64 format",
                        base64String.matches(Regex("[A-Za-z0-9+/=]*")))
                }
                is Base64Flags.NoPadding -> {
                    assertTrue("Invalid unpadded Base64 format",
                        base64String.matches(Regex("[A-Za-z0-9+/]*")))
                    assertFalse("Found padding in NoPadding variant",
                        base64String.contains("="))
                }
                is Base64Flags.UrlSafe -> {
                    assertTrue("Invalid URL-safe Base64 format",
                        base64String.matches(Regex("[A-Za-z0-9_\\-=]*")))
                }
                is Base64Flags.UrlSafeNoPadding -> {
                    assertTrue("Invalid URL-safe unpadded Base64 format",
                        base64String.matches(Regex("[A-Za-z0-9_\\-]*")))
                    assertFalse("Found padding in UrlSafeNoPadding variant",
                        base64String.contains("="))
                }
            }
        }
    }

    @Test
    fun testBase64VariableLengths() = runBlocking {
        // Test various lengths including non-standard ones
        val lengths = listOf(8, 16, 24, 32, 40, 44, 64)

        for (length in lengths) {
            val result = EnhancedRandom.generateEnhancedBase64String(context, length)
            assertTrue("Failed to generate $length chars: ${result.exceptionOrNull()}",
                result.isSuccess)

            val base64String = result.getOrThrow()
            assertEquals("Length mismatch for requested $length chars",
                length, base64String.length)
        }
    }

    @Test
    fun testDistinctBase64Sequences() = runBlocking {
        val length = 32
        // Generate multiple Base64 strings and verify they're different
        val base64Strings = List(5) {
            EnhancedRandom.generateEnhancedBase64String(context, length).getOrThrow()
        }

        val uniqueStrings = base64Strings.toSet()
        assertEquals("Sequential Base64 generations should produce unique strings",
            base64Strings.size, uniqueStrings.size)
    }

    @Test
    fun testUrlSafeBase64Characters() = runBlocking {
        val length = 32
        val result = EnhancedRandom.generateEnhancedBase64String(
            context,
            length,
            Base64Flags.UrlSafe
        )

        val base64String = result.getOrThrow()
        assertFalse("URL-safe Base64 should not contain '+'",
            base64String.contains("+"))
        assertFalse("URL-safe Base64 should not contain '/'",
            base64String.contains("/"))
        assertTrue("URL-safe Base64 should use '-' and '_'",
            base64String.matches(Regex("[A-Za-z0-9_\\-=]*")))
    }

    @Test
    fun testBase64ErrorCases() = runBlocking {
        // Test negative length
        try {
            EnhancedRandom.generateEnhancedBase64String(context, -1)
            fail("Should throw IllegalArgumentException for negative length")
        } catch (e: IllegalArgumentException) {
            assertEquals("Length must be positive", e.message)
        }

        // Test zero length
        try {
            EnhancedRandom.generateEnhancedBase64String(context, 0)
            fail("Should throw IllegalArgumentException for zero length")
        } catch (e: IllegalArgumentException) {
            assertEquals("Length must be positive", e.message)
        }
    }


    @Test
    fun testBase64PaddingBehavior() = runBlocking {
        // Test lengths that would normally require padding
        val lengthsRequiringPadding = listOf(10, 17, 22) // Non-multiple-of-4 lengths

        for (length in lengthsRequiringPadding) {
            // Test with padding
            val paddedResult = EnhancedRandom.generateEnhancedBase64String(
                context, length, Base64Flags.Default
            ).getOrThrow()

            // Test without padding
            val unpaddedResult = EnhancedRandom.generateEnhancedBase64String(
                context, length, Base64Flags.NoPadding
            ).getOrThrow()

            assertEquals("Length mismatch with padding", length, paddedResult.length)
            assertEquals("Length mismatch without padding", length, unpaddedResult.length)
            assertFalse("Unpadded version should not contain padding",
                unpaddedResult.contains("="))
        }
    }

    private fun isValidBase64(str: String): Boolean {
        return try {
            Base64.decode(str, Base64.DEFAULT)
            true
        } catch (e: IllegalArgumentException) {
            false
        }
    }
}