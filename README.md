# SecureCrypto

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![API](https://img.shields.io/badge/API-23%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=23)

A comprehensive, hardware-backed cryptography library for Android applications. The library aims to provide a complete suite of cryptographic operations with strong security guarantees and ergonomic Kotlin-first API design.

## Current Features

- Secure Random Number Generation
  - Hardware-backed entropy generation (when available)
  - Secure memory handling with automatic cleanup
  - Enhanced entropy mixing using Android Keystore
  - Automatic quality detection and fallback mechanisms

## Roadmap

Future releases will include:
- Symmetric Encryption/Decryption
- Asymmetric Encryption/Decryption
- Digital Signatures
- Key Generation and Management
- Message Authentication Codes (MACs)
- Key Derivation Functions (KDFs)

## Installation

Add the dependency to your app's `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.mavbozo.securecrypto:securecrypto:0.1.0")
}
```

## Quick Start

### Secure Random Generation

```kotlin
// Basic random bytes generation
val bytesResult = Random.generateBytes(32)
bytesResult.fold(
    onSuccess = { bytes ->
        try {
            // Use the bytes
        } finally {
            bytes.fill(0) // Clean up
        }
    },
    onFailure = { error ->
        // Handle error
    }
)

// Enhanced random generation with hardware backing
class YourActivity : AppCompatActivity() {
    private suspend fun generateSecureRandom() {
        val bytesResult = EnhancedRandom.generateEnhancedBytes(context, 32)
        bytesResult.fold(
            onSuccess = { bytes ->
                try {
                    // Use the bytes
                } finally {
                    bytes.fill(0) // Clean up
                }
            },
            onFailure = { error ->
                // Handle error
            }
        )
    }
}
```

## Detailed Documentation

### Secure Random Generation

The library provides two main classes for random number generation:

#### Random Class
Basic secure random number generation:

```kotlin
val randomResult = Random.create()
randomResult.fold(
    onSuccess = { random ->
        val bytesResult = random.nextSecureBytes(32)
        bytesResult.fold(
            onSuccess = { secureBytes ->
                secureBytes.use { bytes ->
                    // Use bytes safely here
                } // Automatic cleanup after use
            },
            onFailure = { error ->
                // Handle error
            }
        )
    },
    onFailure = { error ->
        // Handle creation error
    }
)
```

#### EnhancedRandom Class
Advanced random generation with additional hardware entropy:

```kotlin
class YourActivity : AppCompatActivity() {
    private suspend fun useEnhancedRandom() {
        val randomResult = EnhancedRandom.create(context)
        randomResult.fold(
            onSuccess = { random ->
                val bytesResult = random.nextSecureBytes(32)
                bytesResult.fold(
                    onSuccess = { secureBytes ->
                        secureBytes.use { bytes ->
                            // Use bytes safely here
                        } // Automatic cleanup after use
                    },
                    onFailure = { error ->
                        // Handle error
                    }
                )
            },
            onFailure = { error ->
                // Handle creation error
            }
        )
    }
}
```

## Security Architecture

### Design Principles

1. Hardware-First Approach
   - Utilizes hardware security modules when available
   - Graceful fallback to software implementations
   - Security level detection and reporting

2. Memory Safety
   - Automatic zeroing of sensitive data
   - Secure memory handling patterns
   - Resource cleanup guarantees

3. Side-Channel Protection
   - Constant-time operations where possible
   - No branching on secret data
   - Process isolation via Android Keystore

4. Error Handling
   - Explicit Result types
   - No silent fallbacks
   - Detailed error reporting

### Implementation Details

#### Entropy Sources
- Hardware-backed SecureRandom when available
- AndroidKeyStore for additional entropy
- Platform SecureRandom as fallback
- Additional entropy mixing via SHA-512

#### Memory Management
- `SecureBytes` wrapper ensures cleanup
- Uses `finally` blocks for guaranteed cleanup
- Immediate zeroing after operations
- Garbage collector independent

## Requirements

- Android API 23 or higher
- Kotlin Coroutines
- AndroidX Security Crypto library

## Testing

The library includes extensive tests:
```bash
./gradlew test           # Unit tests
./gradlew connectedTest  # Instrumented tests
```

## Best Practices

1. Always use `use` blocks with sensitive data:
```kotlin
// Good
secureBytes.use { bytes ->
    // Use bytes here
} // Automatic cleanup

// Bad
val exposed = secureBytes // No cleanup!
```

2. Handle both success and failure cases:
```kotlin
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

3. Use appropriate coroutine scopes:
```kotlin
lifecycleScope.launch {
    val bytes = withContext(Dispatchers.Default) {
        Random.generateBytes(32).getOrThrow()
    }
    try {
        // Use bytes
    } finally {
        bytes.fill(0)
    }
}
```

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Support

For questions and support:
- Use the GitHub issue tracker for bug reports and feature requests
- Check our [Security Policy](SECURITY.md) for reporting security issues
- Email mavbozo@pm.me for sensitive security reports
