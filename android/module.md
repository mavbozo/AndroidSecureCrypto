# Module AndroidSecureCrypto

A comprehensive random number generation library for Android providing secure operations with strong guarantees and an idiomatic Kotlin API.

## Security Architecture

### Random Generation
- Secure provider-based entropy sources
- Additional entropy mixing via Android Keystore
- Automatic entropy quality detection
- Secure memory handling

### Memory Protection
- Automatic cleanup of sensitive data
- Secure allocation patterns
- Zero-on-free guarantees

### Side-Channel Resistance
- Constant-time implementations where possible
- Secret-independent branching
- Secure error handling

## Platform Requirements

### Minimum Requirements
- Android API 23 (Android 6.0)
- Kotlin 1.9+
- AndroidX Security Crypto library

## Installation

### Gradle Setup
```kotlin
dependencies {
    implementation("com.mavbozo.crypto:android:0.1.0")
}
```

## Current Features

### Random Number Generation
- Secure provider-based entropy
- Enhanced entropy mixing
- Quality detection
- Multiple output formats
  - Raw bytes
  - Hex encoding
  - Base64 encoding (standard/URL-safe)

## Best Practices

### Memory Management
```kotlin
// Always use .use() for secure cleanup
secureBytes.use { bytes ->
    // Use bytes here
} // Automatic cleanup

// Handle both success and failure
Random.generateBytes(32).fold(
    onSuccess = { bytes ->
        try {
            // Use bytes
        } finally {
            bytes.fill(0)
        }
    },
    onFailure = { error ->
        Log.e(TAG, "Generation failed", error)
    }
)
```

### Coroutine Usage
```kotlin
lifecycleScope.launch {
    withContext(Dispatchers.Default) {
        EnhancedRandom.create(context).fold(
            onSuccess = { random ->
                // Use random generator
            },
            onFailure = { /* Handle error */ }
        )
    }
}
```

## Roadmap
- Symmetric encryption/decryption
- Asymmetric encryption/decryption
- Digital signatures
- Key management
- Message authentication
- Key derivation functions
