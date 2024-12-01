package com.mavbozo.androidsecurecrypto

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class KeyDerivationTest {

    @Test
    fun `create - succeeds with default algorithm`() = runBlocking {
        val result = KeyDerivation.create()
        assertTrue("KeyDerivation creation failed", result.isSuccess)
    }

    @Test
    fun `create - succeeds with each algorithm`() = runBlocking {
        HkdfAlgorithm.values().forEach { algorithm ->
            val result = KeyDerivation.create(algorithm)
            assertTrue("KeyDerivation creation failed for $algorithm", result.isSuccess)
        }
    }

    @Test
    fun `deriveKey - each algorithm generates correct size key`() = runBlocking {
        // Given
        val masterKey = ByteArray(32) { it.toByte() }
        val keySizes = listOf(16, 32, 64)

        HkdfAlgorithm.values().forEach { algorithm ->
            keySizes.forEach { keySize ->
                // When
                val result = KeyDerivation.deriveKey(
                    masterKey = masterKey,
                    domain = "test",
                    context = "context1",
                    keySize = keySize,
                    algorithm = algorithm
                )

                // Then
                assertTrue(
                    "Key derivation failed for ${algorithm.name} with size $keySize",
                    result.isSuccess
                )
                result.getOrNull()?.use { bytes ->
                    assertEquals(
                        "Derived key has wrong size for ${algorithm.name}",
                        keySize,
                        bytes.size
                    )
                }
            }
        }
    }

    @Test
    fun `deriveKey - same input produces same key for each algorithm`() = runBlocking {
        // Given
        val masterKey = ByteArray(32) { it.toByte() }

        HkdfAlgorithm.values().forEach { algorithm ->
            // When
            val result1 = KeyDerivation.deriveKey(
                masterKey = masterKey,
                domain = "test",
                context = "context1",
                algorithm = algorithm
            )
            val result2 = KeyDerivation.deriveKey(
                masterKey = masterKey,
                domain = "test",
                context = "context1",
                algorithm = algorithm
            )

            // Then
            assertTrue("First key derivation failed for ${algorithm.name}", result1.isSuccess)
            assertTrue("Second key derivation failed for ${algorithm.name}", result2.isSuccess)

            var key1Bytes: ByteArray? = null
            var key2Bytes: ByteArray? = null

            result1.getOrNull()?.use { bytes1 ->
                key1Bytes = bytes1.clone()
            }

            result2.getOrNull()?.use { bytes2 ->
                key2Bytes = bytes2.clone()
            }

            assertNotNull("First derived key is null for ${algorithm.name}", key1Bytes)
            assertNotNull("Second derived key is null for ${algorithm.name}", key2Bytes)
            assertTrue(
                "Derived keys should be identical for ${algorithm.name}",
                key1Bytes!!.contentEquals(key2Bytes!!)
            )
        }
    }

    @Test
    fun `deriveKey - different algorithms produce different keys`() = runBlocking {
        // Given
        val masterKey = ByteArray(32) { it.toByte() }
        val keys = mutableMapOf<HkdfAlgorithm, ByteArray>()

        // When - derive keys for each algorithm
        HkdfAlgorithm.values().forEach { algorithm ->
            val result = KeyDerivation.deriveKey(
                masterKey = masterKey,
                domain = "test",
                context = "context1",
                algorithm = algorithm
            )

            assertTrue("Key derivation failed for ${algorithm.name}", result.isSuccess)
            result.getOrNull()?.use { bytes ->
                keys[algorithm] = bytes.clone()
            }
        }

        // Then - compare each pair of keys
        for (algo1 in HkdfAlgorithm.values()) {
            for (algo2 in HkdfAlgorithm.values()) {
                if (algo1 != algo2) {
                    assertFalse(
                        "Keys from ${algo1.name} and ${algo2.name} should be different",
                        keys[algo1]!!.contentEquals(keys[algo2]!!)
                    )
                }
            }
        }

        // Cleanup
        keys.values.forEach { it.fill(0) }
    }

    @Test
    fun `deriveKey - different domains produce different keys for each algorithm`() = runBlocking {
        // Given
        val masterKey = ByteArray(32) { it.toByte() }

        HkdfAlgorithm.values().forEach { algorithm ->
            // When
            val result1 = KeyDerivation.deriveKey(
                masterKey = masterKey,
                domain = "domain1",
                context = "context",
                algorithm = algorithm
            )
            val result2 = KeyDerivation.deriveKey(
                masterKey = masterKey,
                domain = "domain2",
                context = "context",
                algorithm = algorithm
            )

            // Then
            assertTrue("First key derivation failed for ${algorithm.name}", result1.isSuccess)
            assertTrue("Second key derivation failed for ${algorithm.name}", result2.isSuccess)

            var key1Bytes: ByteArray? = null
            var key2Bytes: ByteArray? = null

            result1.getOrNull()?.use { bytes1 ->
                key1Bytes = bytes1.clone()
            }

            result2.getOrNull()?.use { bytes2 ->
                key2Bytes = bytes2.clone()
            }

            assertNotNull("First derived key is null for ${algorithm.name}", key1Bytes)
            assertNotNull("Second derived key is null for ${algorithm.name}", key2Bytes)
            assertFalse(
                "Keys from different domains should be different for ${algorithm.name}",
                key1Bytes!!.contentEquals(key2Bytes!!)
            )
        }
    }

    // Original validation tests adapted for all algorithms
    @Test
    fun `deriveKey - fails with empty domain for all algorithms`() = runBlocking {
        val masterKey = ByteArray(32) { it.toByte() }

        HkdfAlgorithm.values().forEach { algorithm ->
            val result = KeyDerivation.deriveKey(
                masterKey = masterKey,
                domain = "",
                context = "context",
                algorithm = algorithm
            )

            assertTrue("Should fail with empty domain for ${algorithm.name}", result.isFailure)
            val exception = result.exceptionOrNull()
            assertTrue("Should throw IllegalArgumentException",
                exception is IllegalArgumentException)
            assertEquals("Wrong error message",
                "Domain must not be empty", exception?.message)
        }
    }

    @Test
    fun `deriveKey - fails with short master key for all algorithms`() = runBlocking {
        val shortMasterKey = ByteArray(8) { it.toByte() }

        HkdfAlgorithm.values().forEach { algorithm ->
            val result = KeyDerivation.deriveKey(
                masterKey = shortMasterKey,
                domain = "test",
                context = "context",
                algorithm = algorithm
            )

            assertTrue("Should fail with short master key for ${algorithm.name}", result.isFailure)
            val exception = result.exceptionOrNull()
            assertTrue("Should throw IllegalArgumentException",
                exception is IllegalArgumentException)
            assertEquals("Wrong error message",
                "Master key too short", exception?.message)
        }
    }

    @Test
    fun `deriveKey - cleanup after use for all algorithms`() = runBlocking {
        val masterKey = ByteArray(32) { it.toByte() }

        HkdfAlgorithm.values().forEach { algorithm ->
            val result = KeyDerivation.deriveKey(
                masterKey = masterKey,
                domain = "test",
                context = "context",
                algorithm = algorithm
            )

            assertTrue("Key derivation failed for ${algorithm.name}", result.isSuccess)

            result.getOrNull()?.use { bytes ->
                assertFalse("Bytes should not be zeroed before use for ${algorithm.name}",
                    bytes.all { it == 0.toByte() })
            }

            result.getOrNull()?.use { bytes ->
                assertTrue("Bytes should be zeroed after use for ${algorithm.name}",
                    bytes.all { it == 0.toByte() })
            }
        }
    }
}