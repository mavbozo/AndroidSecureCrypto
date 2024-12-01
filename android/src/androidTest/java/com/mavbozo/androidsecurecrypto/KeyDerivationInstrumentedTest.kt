package com.mavbozo.androidsecurecrypto

import android.content.Context
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
class KeyDerivationInstrumentedTest {
    private lateinit var context: Context

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
    }

    @Test
    fun testBasicKeyDerivation() = runBlocking {
        val masterKey = ByteArray(32) { it.toByte() }
        val result = KeyDerivation.deriveKey(
            masterKey = masterKey,
            domain = "test",
            context = "context1",
            keySize = 32,
            algorithm = HkdfAlgorithm.SHA256
        )

        assertTrue("Key derivation failed", result.isSuccess)
        result.getOrThrow().use { bytes ->
            assertEquals("Derived key has wrong size", 32, bytes.size)
            assertFalse("Derived key should not be all zeros",
                bytes.all { it == 0.toByte() })
        }
    }

    @Test
    fun testConcurrentDerivation() = runBlocking {
        val masterKey = ByteArray(32) { it.toByte() }
        val iterations = 10

        val results = coroutineScope {
            List(iterations) {
                async(Dispatchers.Default) {
                    KeyDerivation.deriveKey(
                        masterKey = masterKey,
                        domain = "test",
                        context = "context$it",
                        algorithm = HkdfAlgorithm.SHA256
                    )
                }
            }.awaitAll()
        }

        // Verify all operations succeeded
        assertTrue("All derivations should succeed",
            results.all { it.isSuccess })

        // Verify all derived keys are different
        val derivedKeys = results.map { result ->
            var keyBytes: ByteArray? = null
            result.getOrThrow().use { bytes ->
                keyBytes = bytes.clone()
            }
            keyBytes!!
        }

        val uniqueKeys = derivedKeys.map { it.toList() }.toSet()
        assertEquals("All derived keys should be unique",
            iterations, uniqueKeys.size)

        // Clean up
        derivedKeys.forEach { it.fill(0) }
    }

    @Test
    fun testMemoryCleanup() = runBlocking {
        val masterKey = ByteArray(32) { it.toByte() }

        // Pre-warm and stabilize memory
        System.gc()
        Thread.sleep(100)

        val initialMemory = Runtime.getRuntime().totalMemory() -
                Runtime.getRuntime().freeMemory()

        // Perform multiple derivations
        repeat(100) {
            val result = KeyDerivation.deriveKey(
                masterKey = masterKey,
                domain = "test",
                context = "context$it"
            )

            result.getOrThrow().use { bytes ->
                assertFalse("Derived key should not be all zeros",
                    bytes.all { it == 0.toByte() })
            }
        }

        // Force cleanup
        System.gc()
        Thread.sleep(100)

        val finalMemory = Runtime.getRuntime().totalMemory() -
                Runtime.getRuntime().freeMemory()
        val memoryDiff = finalMemory - initialMemory

        // Memory usage shouldn't grow significantly
        println("Memory usage difference: ${memoryDiff / 1024} KB")
        assertTrue("Excessive memory retention detected",
            memoryDiff < 1024 * 1024) // Allow 1MB variance
    }

    @Test
    fun testDifferentAlgorithms() = runBlocking {
        val masterKey = ByteArray(32) { it.toByte() }
        val keys = mutableMapOf<HkdfAlgorithm, ByteArray>()

        // Generate keys with each algorithm
        for (algorithm in HkdfAlgorithm.entries) {
            val result = KeyDerivation.deriveKey(
                masterKey = masterKey,
                domain = "test",
                context = "context",
                algorithm = algorithm
            )

            assertTrue("Key derivation failed for $algorithm",
                result.isSuccess)

            result.getOrThrow().use { bytes ->
                keys[algorithm] = bytes.clone()
            }

            // Verify key properties
            assertFalse("Generated key should not be all zeros",
                keys[algorithm]!!.all { it == 0.toByte() })
        }

        // Verify each algorithm produces different output
        for (algo1 in HkdfAlgorithm.entries) {
            for (algo2 in HkdfAlgorithm.entries) {
                if (algo1 != algo2) {
                    assertFalse(
                        "Different algorithms should produce different keys",
                        keys[algo1]!!.contentEquals(keys[algo2]!!)
                    )
                }
            }
        }

        // Clean up
        keys.values.forEach { it.fill(0) }
    }

    @Test
    fun testPerformance() = runBlocking {
        val masterKey = ByteArray(32) { it.toByte() }
        val iterations = 100
        val keySizes = listOf(16, 32, 64)

        for (algorithm in HkdfAlgorithm.values()) {
            for (keySize in keySizes) {
                val startTime = System.nanoTime()

                // Perform multiple derivations
                repeat(iterations) {
                    val result = KeyDerivation.deriveKey(
                        masterKey = masterKey,
                        domain = "test",
                        context = "context$it",
                        keySize = keySize,
                        algorithm = algorithm
                    )

                    result.getOrThrow().use { bytes ->
                        assertEquals("Wrong key size", keySize, bytes.size)
                    }
                }

                val duration = (System.nanoTime() - startTime) / 1_000_000.0 // ms
                val throughput = iterations * keySize / duration * 1000 / 1024 // KB/s

                println("""
                    Performance ($algorithm, $keySize bytes):
                    Total time: $duration ms
                    Throughput: $throughput KB/s
                """.trimIndent())

                assertTrue("Performance below threshold",
                    duration / iterations < 10) // Max 10ms per derivation
            }
        }
    }

    @Test
    fun testErrorCases() = runBlocking {
        val validKey = ByteArray(32) { it.toByte() }

        // Test too short master key
        val shortKey = ByteArray(8) { it.toByte() }
        val shortKeyResult = KeyDerivation.deriveKey(
            masterKey = shortKey,
            domain = "test",
            context = "context"
        )
        assertTrue("Should fail with short key", shortKeyResult.isFailure)
        val shortKeyError = shortKeyResult.exceptionOrNull()
        assertTrue("Should be IllegalArgumentException",
            shortKeyError is IllegalArgumentException)
        assertTrue("Error should mention key length",
            shortKeyError?.message?.contains("too short") == true)

        // Test empty domain
        val emptyDomainResult = KeyDerivation.deriveKey(
            masterKey = validKey,
            domain = "",
            context = "context"
        )
        assertTrue("Should fail with empty domain",
            emptyDomainResult.isFailure)
        val emptyDomainError = emptyDomainResult.exceptionOrNull()
        assertTrue("Should be IllegalArgumentException",
            emptyDomainError is IllegalArgumentException)
        assertTrue("Error should mention domain",
            emptyDomainError?.message?.contains("Domain") == true)

        // Test empty context
        val emptyContextResult = KeyDerivation.deriveKey(
            masterKey = validKey,
            domain = "test",
            context = ""
        )
        assertTrue("Should fail with empty context",
            emptyContextResult.isFailure)
        val emptyContextError = emptyContextResult.exceptionOrNull()
        assertTrue("Should be IllegalArgumentException",
            emptyContextError is IllegalArgumentException)
        assertTrue("Error should mention context",
            emptyContextError?.message?.contains("Context") == true)

        // Test zero key size
        val zeroSizeResult = KeyDerivation.deriveKey(
            masterKey = validKey,
            domain = "test",
            context = "context",
            keySize = 0
        )
        assertTrue("Should fail with zero size", zeroSizeResult.isFailure)
        val zeroSizeError = zeroSizeResult.exceptionOrNull()
        assertTrue("Should be IllegalArgumentException",
            zeroSizeError is IllegalArgumentException)
        assertTrue("Error should mention key size",
            zeroSizeError?.message?.contains("size") == true)
    }
}