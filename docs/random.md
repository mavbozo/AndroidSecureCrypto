# Random Number Generation Guide

## Overview

AndroidSecureCrypto provides two levels of random number generation:

1. Basic (`Random` class)
   - Platform SecureRandom with quality detection
   - Automatic entropy assessment
   - Memory-safe output handling

2. Enhanced (`EnhancedRandom` class)
   - Hardware-backed entropy generation
   - Multiple entropy sources
   - Android Keystore integration
   - Additional entropy mixing

## Basic Usage

### Generate Random Bytes

```kotlin
// Generate 32 random bytes
val bytesResult = Random.generateBytes(32)
bytesResult.fold(
    onSuccess = { bytes ->
        try {
            // Use bytes
        } finally {
            bytes.fill(0) // Clean up
        }
    },
    onFailure = { error ->
        // Handle error
    }
)
```

### Generate Hex String

```kotlin
// Generate 32 random bytes as hex
val hexResult = Random.generateBytesAsHex(32)
hexResult.fold(
    onSuccess = { hexString ->
        // Use hex string (64 characters)
    },
    onFailure = { error ->
        // Handle error
    }
)
```

### Generate Base64 String

```kotlin
// Generate random bytes as Base64
val base64Result = Random.generateBytesAsBase64(
    size = 32,
    flags = Base64Flags.UrlSafe
)
base64Result.fold(
    onSuccess = { base64String ->
        // Use Base64 string
    },
    onFailure = { error ->
        // Handle error
    }
)
```

## Enhanced Usage

### Hardware-Backed Generation

```kotlin
class YourActivity : AppCompatActivity() {
    private suspend fun generateSecureRandom() {
        // Generate enhanced random bytes
        val bytesResult = EnhancedRandom.generateEnhancedBytes(
            context = context,
            size = 32
        )
        bytesResult.fold(
            onSuccess = { bytes ->
                try {
                    // Use bytes
                } finally {
                    bytes.fill(0)
                }
            },
            onFailure = { error ->
                // Handle error
            }
        )
    }
}
```

### Quality Detection

```kotlin
// Check entropy source quality
val randomResult = Random.create()
randomResult.fold(
    onSuccess = { random ->
        when (random.getQuality()) {
            is EntropyQuality.Hardware ->
                // Hardware-backed entropy
            is EntropyQuality.Fallback ->
                // Software-based entropy
        }
    },
    onFailure = { error ->
        // Handle creation error
    }
)
```

## Advanced Features

### Base64 Encoding Options

```kotlin
// Standard Base64 (default)
Base64Flags.Default

// No padding
Base64Flags.NoPadding

// URL-safe encoding
Base64Flags.UrlSafe

// URL-safe, no padding
Base64Flags.UrlSafeNoPadding
```

### Custom Random Instance

```kotlin
// Create custom instance
val random = Random.create().getOrThrow()

// Generate bytes
val bytes = random.nextSecureBytes(32)
    .getOrThrow()
try {
    // Use bytes
} finally {
    bytes.fill(0)
}
```

## Security Considerations

1. Entropy Quality
   - Hardware sources preferred
   - Multiple entropy sources
   - Quality detection available
   - Automatic mixing

2. Memory Safety
   - Automatic cleanup
   - Structured use blocks
   - Exception safety
   - Resource tracking

3. Thread Safety
   - Thread-safe implementation
   - No shared state
   - Concurrent access safe
   - Coroutine support

4. Error Handling
   - Explicit Result types
   - Quality reporting
   - Detailed errors
   - No silent failures

## Best Practices

1. Entropy Verification
   ```kotlin
   // Verify entropy source quality
   when (random.getQuality()) {
       is EntropyQuality.Hardware -> // Proceed
       is EntropyQuality.Fallback -> // Consider risks
   }
   ```

2. Proper Cleanup
   ```kotlin
   // Always use structured cleanup
   secureBytes.use { bytes ->
       // Use bytes here
   } // Automatic cleanup
   ```

3. Error Handling
   ```kotlin
   // Handle all potential errors
   Random.generateBytes(32).fold(
       onSuccess = { bytes -> /* ... */ },
       onFailure = { error -> /* ... */ }
   )
   ```

4. Coroutine Context
   ```kotlin
   // Use appropriate dispatcher
   withContext(Dispatchers.Default) {
       Random.generateBytes(32)
   }
   ```

## Common Patterns

### Key Generation

```kotlin
// Generate encryption key
val key = Random.generateBytes(32).getOrThrow()
try {
    // Use key for encryption
} finally {
    key.fill(0)
}
```

### Random IDs

```kotlin
// Generate random identifier
val id = Random.generateBytesAsBase64(
    size = 16,
    flags = Base64Flags.UrlSafeNoPadding
).getOrThrow()
```

### Salt Generation

```kotlin
// Generate cryptographic salt
val salt = Random.generateBytes(16).getOrThrow()
try {
    // Use salt for hashing
} finally {
    salt.fill(0)
}
```
