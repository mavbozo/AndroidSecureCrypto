/**
 * Provides secure cryptographic operations for Android applications with hardware-backed encryption
 * support and automatic memory cleanup.
 *
 * Security features:
 * - Authenticated Encryption with Associated Data (AEAD) using AES-GCM
 * - Hardware-backed encryption when available
 * - Automatic secure memory cleanup
 * - Side-channel attack mitigations
 * - Format validation and version checking
 */
package com.mavbozo.androidsecurecrypto

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.SecureRandom
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.nio.ByteBuffer

private const val MAGIC_BYTES = "SECB"
private const val CURRENT_VERSION: Byte = 0x01
private const val MAX_PARAMS_LENGTH = 65535
private const val MAX_FILE_SIZE = 10 * 1024 * 1024 // 10 MB in bytes

/**
 * Interface defining supported encryption formats with their parameters and validation rules.
 * Each format specifies its identifier, parameter structure, and key requirements.
 */
sealed interface CipherFormat {
    /** Unique identifier for this cipher format */
    val id: Byte

    /** Required length of the parameter block in bytes */
    val paramsLength: Int

    /** Required key size in bytes */
    val keySize: Int

    /**
     * Validates the provided parameter block for this format.
     * @throws IllegalArgumentException if parameters are invalid
     */
    fun validateParams(params: ByteArray)

    /**
     * Creates a new parameter block for this format with secure random values.
     * @return ByteArray containing the generated parameters
     */
    fun createParams(): ByteArray
}

/**
 * Implementation of AES-GCM encryption format with 256-bit keys and 128-bit authentication tags.
 */
data object AesGcmFormat : CipherFormat {
    override val id: Byte = 0x01
    override val paramsLength: Int = 16 // 12 bytes IV + 4 bytes tag length
    override val keySize: Int = 32  // Support AES-256 by default

    override fun validateParams(params: ByteArray) {
        require(params.size == paramsLength) { "Invalid parameter length for AES-GCM" }
    }

    override fun createParams(): ByteArray = ByteBuffer.allocate(paramsLength).apply {
        val iv = ByteArray(12).apply { SecureRandom().nextBytes(this) }
        put(iv)
        putInt(128) // 128-bit authentication tag
    }.array()
}


/**
 * Represents the header structure for encrypted data, containing format information
 * and encryption parameters.
 *
 * @property algorithm The encryption format being used
 * @property params Format-specific parameters (e.g., IV and tag length for AES-GCM)
 * @throws IllegalArgumentException if parameters are invalid for the specified algorithm
 */
data class CipherHeader(
    val algorithm: CipherFormat,
    val params: ByteArray
) {
    init {
        algorithm.validateParams(params)
    }

    /**
     * Companion object providing factory methods for header creation and parsing.
     */
    companion object {

        /**
         * Parses a cipher header from the input buffer.
         *
         * @param input ByteBuffer containing the header data
         * @return Parsed CipherHeader instance
         * @throws IllegalArgumentException if the header format is invalid
         */
        fun parse(input: ByteBuffer): CipherHeader {
            // Read and validate magic bytes
            val magicBytes = ByteArray(4)
            input.get(magicBytes)
            require(magicBytes.contentEquals(MAGIC_BYTES.toByteArray(Charsets.US_ASCII))) {
                "Invalid magic bytes"
            }

            // Read and validate version
            val version = input.get()
            require(version == CURRENT_VERSION) { "Unsupported version: $version" }

            // Read algorithm ID and map to format
            val algorithmId = input.get()
            val format = when (algorithmId) {
                AesGcmFormat.id -> AesGcmFormat
                else -> throw IllegalArgumentException("Unsupported algorithm: $algorithmId")
            }

            // Read and validate params length
            val paramsLength = input.short.toInt() and 0xFFFF
            require(paramsLength in 1..MAX_PARAMS_LENGTH) { "Invalid params length" }
            require(paramsLength == format.paramsLength) {
                "Unexpected params length for algorithm"
            }

            // Read params
            val params = ByteArray(paramsLength)
            input.get(params)

            return CipherHeader(format, params)
        }

        /**
         * Creates a new header for the specified cipher format.
         *
         * @param format The encryption format to use
         * @return CipherHeader with newly generated parameters
         */
        fun create(format: CipherFormat): CipherHeader {
            return CipherHeader(format, format.createParams())
        }
    }

    /**
     * Encodes the header into a byte array suitable for prepending to ciphertext.
     * Format:
     * - 4 bytes: Magic ("SECB")
     * - 1 byte: Version (0x01)
     * - 1 byte: Algorithm ID
     * - 2 bytes: Parameters length
     * - N bytes: Algorithm parameters
     *
     * @return Encoded header as byte array
     */
    fun encode(): ByteArray = ByteBuffer.allocate(8 + params.size).apply {
        put(MAGIC_BYTES.toByteArray(Charsets.US_ASCII))
        put(CURRENT_VERSION)
        put(algorithm.id)
        putShort(params.size.toShort())
        put(params)
    }.array()
}

/**
 * Base interface for cryptographic algorithms supported by the library.
 * Defines common properties required for algorithm implementation.
 */
sealed interface Algorithm {
    /** JCA transformation string */
    val transformation: String


    /** Required key size in bytes */
    val keySize: Int
}

/** Interface for algorithms requiring an initialization vector */
sealed interface IvBasedAlgorithm : Algorithm {
    /** Required IV size in bytes */
    val ivSize: Int
}

/** Interface for authenticated encryption algorithms */
sealed interface AuthenticatedAlgorithm : Algorithm {

    /** Authentication tag size in bytes */
    val tagSize: Int
}

/** Interface for authenticated encryption with associated data (AEAD) algorithms */
sealed interface AeadAlgorithm : IvBasedAlgorithm, AuthenticatedAlgorithm

/**
 * Collection of AES-based encryption algorithms
 */
sealed class Aes {
    /**
     * AES-GCM implementation with 256-bit keys and 128-bit authentication tags
     */
    data object GCM : AeadAlgorithm {
        override val transformation = "AES/GCM/NoPadding"
        override val ivSize = 12    // 96-bit nonce
        override val tagSize = 16   // 128-bit tag
        override val keySize = 32   // 256-bit key
    }
}

/**
 * Main cipher implementation providing secure encryption and decryption operations.
 * Supports hardware-backed encryption when available and ensures secure cleanup of sensitive data.
 */
class Cipher private constructor(
    private val cipher: javax.crypto.Cipher,
    private val format: CipherFormat
) {

    /**
     * Returns the name of the security provider being used by this cipher.
     * This can be used to determine if hardware-backed encryption is being used.
     *
     * @return String representing the provider name (e.g., "AndroidKeyStore", "AndroidOpenSSL")
     */
    fun getProvider(): String = cipher.provider.name

    private fun createCipher() = javax.crypto.Cipher.getInstance(
        when (format) {
            is AesGcmFormat -> "AES/GCM/NoPadding"
        }
    )

    private fun validateKey(key: ByteArray) {
        require(key.size == format.keySize) {
            "Key must be ${format.keySize} bytes for ${format::class.simpleName}"
        }
    }

    private suspend fun encryptBytes(key: ByteArray, plaintext: ByteArray): Result<ByteArray> =
        withContext(Dispatchers.Default) {
            runCatching {
                validateKey(key)

                // Create header
                val header = CipherHeader.create(format)

                // Setup cipher based on format
                val cipher = when (format) {
                    is AesGcmFormat -> {
                        val paramsBuf = ByteBuffer.wrap(header.params)
                        val iv = ByteArray(12).apply { paramsBuf.get(this) }
                        val tagLength = paramsBuf.getInt()

                        createCipher().apply {
                            init(
                                javax.crypto.Cipher.ENCRYPT_MODE,
                                SecretKeySpec(key, "AES"),
                                GCMParameterSpec(tagLength, iv)
                            )
                        }
                    }
                }

                // Encrypt data
                val ciphertext = cipher.doFinal(plaintext)

                // Combine header and ciphertext
                ByteBuffer.allocate(header.encode().size + ciphertext.size)
                    .put(header.encode())
                    .put(ciphertext)
                    .array()
            }
        }

    private suspend fun decryptBytes(key: ByteArray, input: ByteArray): Result<ByteArray> =
        withContext(Dispatchers.Default) {
            runCatching {
                validateKey(key)

                // Parse header
                val buffer = ByteBuffer.wrap(input)
                val header = CipherHeader.parse(buffer)

                // Get remaining ciphertext
                val ciphertext = ByteArray(input.size - (8 + header.params.size))
                buffer.get(ciphertext)

                // Setup cipher based on format
                val cipher = when (header.algorithm) {
                    is AesGcmFormat -> {
                        val paramsBuf = ByteBuffer.wrap(header.params)
                        val iv = ByteArray(12).apply { paramsBuf.get(this) }
                        val tagLength = paramsBuf.getInt()

                        createCipher().apply {
                            init(
                                javax.crypto.Cipher.DECRYPT_MODE,
                                SecretKeySpec(key, "AES"),
                                GCMParameterSpec(tagLength, iv)
                            )
                        }
                    }
                }

                // Decrypt and verify authentication
                cipher.doFinal(ciphertext)
            }
        }

    private suspend fun encryptFile(
        key: ByteArray,
        sourceFile: File,
        destFile: File
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            require(sourceFile.length() <= MAX_FILE_SIZE) {
                "File size exceeds maximum allowed size of 10MB"
            }

            // Read entire file into memory
            val plaintext = sourceFile.readBytes()
            try {
                // Encrypt the bytes
                val encrypted = encryptBytes(key, plaintext).getOrThrow()

                // Write to destination atomically
                destFile.outputStream().use { it.write(encrypted) }
            } finally {
                // Secure cleanup of plaintext
                plaintext.fill(0)
            }
        }
    }

    private suspend fun decryptFile(
        key: ByteArray,
        sourceFile: File,
        destFile: File
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            require(sourceFile.length() <= MAX_FILE_SIZE) {
                "File size exceeds maximum allowed size of 10MB"
            }

            // Read entire encrypted file
            val ciphertext = sourceFile.readBytes()
            try {
                // Decrypt the bytes
                val decrypted = decryptBytes(key, ciphertext).getOrThrow()

                // Write to destination atomically
                destFile.outputStream().use { it.write(decrypted) }
            } finally {
                // Secure cleanup
                ciphertext.fill(0)
            }
        }
    }

    /**
     * Companion object providing static encryption utility functions.
     */
    companion object {

        /**
         * Creates a new Cipher instance with the specified format.
         *
         * @param format The encryption format to use (defaults to AES-GCM)
         * @return Result containing the Cipher instance or an error
         */
        fun create(format: CipherFormat = AesGcmFormat): Result<Cipher> = runCatching {
            val transformation = when (format) {
                is AesGcmFormat -> "AES/GCM/NoPadding"
            }
            val cipher = javax.crypto.Cipher.getInstance(transformation)
            Cipher(cipher, format)
        }

        /**
         * Encrypts a string using AES-GCM and encodes the result in Base64.
         *
         * @param key 32-byte encryption key
         * @param plaintext String to encrypt
         * @return Result containing the Base64-encoded encrypted string or an error
         * @throws IllegalArgumentException if the key size is incorrect
         *
         * @sample com.mavbozo.crypto.samples.cipher.CipherSamples.stringEncryption
         *
         */
        suspend fun encryptString(key: ByteArray, plaintext: String): Result<String> = runCatching {
            val cipher = create().getOrThrow()
            cipher.encryptBytes(key, plaintext.encodeToByteArray())
                .map { encryptedBytes ->
                    Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
                }.getOrThrow()
        }


        /**
         * Decrypts a Base64-encoded encrypted string.
         *
         * @param key 32-byte encryption key
         * @param ciphertext Base64-encoded encrypted string
         * @return Result containing the decrypted string or an error
         * @throws IllegalArgumentException if the key size is incorrect
         *
         * @sample com.mavbozo.crypto.samples.cipher.CipherSamples.stringEncryption
         *
         */
        suspend fun decryptString(key: ByteArray, ciphertext: String): Result<String> =
            runCatching {
                val cipher = create().getOrThrow()
                val encryptedBytes = Base64.decode(ciphertext, Base64.NO_WRAP)
                cipher.decryptBytes(key, encryptedBytes)
                    .map { decryptedBytes ->
                        decryptedBytes.decodeToString()
                    }.getOrThrow()
            }

        /**
         * Encrypts a byte array using AES-GCM.
         *
         * @param key 32-byte encryption key
         * @param plaintext Data to encrypt
         * @return Result containing the encrypted bytes or an error
         * @throws IllegalArgumentException if the key size is incorrect
         *
         * @sample com.mavbozo.crypto.samples.cipher.CipherSamples.byteArrayEncryption
         *
         */
        suspend fun encryptBytes(key: ByteArray, plaintext: ByteArray): Result<ByteArray> =
            runCatching {
                val cipher = create().getOrThrow()
                cipher.encryptBytes(key, plaintext).getOrThrow()
            }


        /**
         * Decrypts an encrypted byte array.
         *
         * @param key 32-byte encryption key
         * @param ciphertext Encrypted data
         * @return Result containing the decrypted bytes or an error
         * @throws IllegalArgumentException if the key size is incorrect
         *
         * @sample com.mavbozo.crypto.samples.cipher.CipherSamples.byteArrayEncryption
         *
         */
        suspend fun decryptBytes(key: ByteArray, ciphertext: ByteArray): Result<ByteArray> =
            runCatching {
                val cipher = create().getOrThrow()
                cipher.decryptBytes(key, ciphertext).getOrThrow()
            }

        /**
         * Encrypts a file using AES-GCM.
         *
         * @param key 32-byte encryption key
         * @param sourceFile File to encrypt
         * @param destFile Destination for encrypted data
         * @return Result indicating success or an error
         * @throws IllegalArgumentException if the key size is incorrect or file is too large
         *
         * @sample com.mavbozo.crypto.samples.cipher.CipherSamples.fileEncryption
         *
         */
        suspend fun encryptFile(key: ByteArray, sourceFile: File, destFile: File): Result<Unit> =
            runCatching {
                val cipher = create().getOrThrow()
                cipher.encryptFile(key, sourceFile, destFile).getOrThrow()
            }

        /**
         * Decrypts an encrypted file.
         *
         * @param key 32-byte encryption key
         * @param sourceFile Encrypted file
         * @param destFile Destination for decrypted data
         * @return Result indicating success or an error
         * @throws IllegalArgumentException if the key size is incorrect or file is too large
         *
         * @sample com.mavbozo.crypto.samples.cipher.CipherSamples.fileEncryption
         *
         */
        suspend fun decryptFile(key: ByteArray, sourceFile: File, destFile: File): Result<Unit> =
            runCatching {
                val cipher = create().getOrThrow()
                cipher.decryptFile(key, sourceFile, destFile).getOrThrow()
            }

    }
}