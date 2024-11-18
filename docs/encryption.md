# Encryption Guide

## Overview

AndroidSecureCrypto provides authenticated encryption using AES-GCM with the following features:
- 256-bit keys
- 128-bit authentication tags
- Automatic IV management
- Hardware acceleration (when available)
- Memory-safe implementation

## Basic Usage

### String Encryption

```kotlin
// Generate a random 256-bit key
val keyResult = Random.generateBytes(32)
keyResult.fold(
    onSuccess = { key ->
        try {
            // Encrypt string
            val encrypted = Cipher.encryptString(key, "sensitive data")
                .getOrThrow()
            
            // Decrypt string
            val decrypted = Cipher.decryptString(key, encrypted)
                .getOrThrow()
            
            // Use decrypted data
            println(decrypted) // "sensitive data"
        } finally {
            key.fill(0) // Clean up key
        }
    },
    onFailure = { error ->
        // Handle key generation error
    }
)
```

### Byte Array Encryption

```kotlin
// Encrypt raw bytes
val bytesResult = Cipher.encryptBytes(key, dataBytes)
bytesResult.fold(
    onSuccess = { encryptedBytes ->
        // Use encrypted bytes
    },
    onFailure = { error ->
        // Handle encryption error
    }
)
```

### File Encryption

```kotlin
val sourceFile = File("sensitive.txt")
val encryptedFile = File("encrypted.bin")
val decryptedFile = File("decrypted.txt")

// Encrypt file
Cipher.encryptFile(key, sourceFile, encryptedFile)
    .onSuccess {
        // File encrypted successfully
    }
    .onFailure { error ->
        // Handle encryption error
    }

// Decrypt file
Cipher.decryptFile(key, encryptedFile, decryptedFile)
    .onSuccess {
        // File decrypted successfully
    }
    .onFailure { error ->
        // Handle decryption error
    }
```

## Advanced Usage

### Custom Cipher Format

```kotlin
// Create cipher with specific format
val cipher = Cipher.create(AesGcmFormat)
    .getOrThrow()

// Check provider details
val provider = cipher.getProvider()
println("Using provider: $provider")
```

### Base64 Configuration

```kotlin
// URL-safe Base64 without padding
val encrypted = Cipher.encryptString(
    key = key,
    plaintext = "data",
    base64Flags = Base64Flags.UrlSafeNoPadding
).getOrThrow()
```

## Security Considerations

1. Key Management
   - Generate new random keys
   - Never reuse keys
   - Clean up keys when done
   - Consider key rotation

2. IV Handling
   - Library handles automatically
   - Never reuse IVs
   - Verify IV uniqueness

3. Error Handling
   - Check Result objects
   - Handle all error cases
   - Verify decryption success

4. Memory Safety
   - Use structured cleanup
   - Clear sensitive data
   - Handle exceptions properly

## Performance Considerations

1. Hardware Acceleration
   - Automatically used when available
   - Significant performance boost
   - Battery efficient

2. File Encryption
   - 10MB file size limit
   - Use streaming for larger files
   - Consider chunked processing

3. String Encryption
   - Base64 adds ~33% overhead
   - Consider raw bytes for large data
   - URL-safe encoding available

## Error Handling Examples

```kotlin
// Comprehensive error handling
Cipher.encryptString(key, "data").fold(
    onSuccess = { encrypted ->
        // Successful encryption
        Cipher.decryptString(key, encrypted).fold(
            onSuccess = { decrypted ->
                // Successful decryption
            },
            onFailure = { error ->
                when (error) {
                    is IllegalArgumentException -> // Invalid input
                    is SecurityException -> // Security error
                    else -> // Unknown error
                }
            }
        )
    },
    onFailure = { error ->
        // Handle encryption error
    }
)
```

## Best Practices

1. Key Generation
   ```kotlin
   // Always use library's random generator
   val key = Random.generateBytes(32).getOrThrow()
   ```

2. Cleanup
   ```kotlin
   try {
       // Use key
   } finally {
       key.fill(0)
   }
   ```

3. Error Checking
   ```kotlin
   // Always verify operations
   val result = operation().getOrNull()
       ?: handleError()
   ```

4. Provider Selection
   ```kotlin
   // Check for hardware acceleration
   when (cipher.getProvider()) {
       "AndroidKeyStore" -> // Hardware
       else -> // Software
   }
   ```
