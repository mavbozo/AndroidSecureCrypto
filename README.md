# AndroidSecureCrypto

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![API](https://img.shields.io/badge/API-23%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=23)

A comprehensive, hardware-backed cryptography library for Android applications. The library aims to provide a complete suite of cryptographic operations with strong security guarantees and ergonomic Kotlin-first API design.

## Current Features

- Secure Random Number Generation
  - Hardware-backed entropy generation (when available)
  - Secure memory handling with automatic cleanup
  - Enhanced entropy mixing using Android Keystore
  - Automatic quality detection and fallback mechanisms
  - Hex and Base64 string generation with URL-safe options
  
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
    implementation("com.mavbozo.crypto:android:0.1.0")
}
```

## Quick Start

### Basic Random Generation

```kotlin
// Generate random bytes with automatic cleanup
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

// Generate hex string
val hexResult = Random.generateBytesAsHex(32)
hexResult.fold(
    onSuccess = { hexString ->
        // Use hex string (e.g., "a1b2c3d4...")
    },
    onFailure = { error ->
        // Handle error
    }
)

// Generate Base64 string
val base64Result = Random.generateBytesAsBase64(
    size = 32,
    flags = Base64Flags.Default // or UrlSafe, NoPadding, UrlSafeNoPadding
)
base64Result.fold(
    onSuccess = { base64String ->
        // Use base64 string
    },
    onFailure = { error ->
        // Handle error
    }
)
```

### Enhanced Hardware-Backed Random Generation

```kotlin
class YourActivity : AppCompatActivity() {
    private suspend fun generateSecureRandom() {
        // Generate random bytes
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

        // Generate hex string
        val hexResult = EnhancedRandom.generateEnhancedBytesAsHex(context, 32)
        hexResult.fold(
            onSuccess = { hexString ->
                // Use hex string
            },
            onFailure = { error ->
                // Handle error
            }
        )

        // Generate Base64 string
        val base64Result = EnhancedRandom.generateEnhancedBytesAsBase64(
            context = context,
            size = 32,
            flags = Base64Flags.UrlSafe
        )
        base64Result.fold(
            onSuccess = { base64String ->
                // Use base64 string
            },
            onFailure = { error ->
                // Handle error
            }
        )
    }
}
```

## Detailed Documentation

### Random Number Generation

The library provides two main classes for random number generation:

#### Random Class

Basic secure random number generation:

```kotlin
// Create Random instance
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

// Generate random hex string
val hexResult = Random.generateBytesAsHex(32)
hexResult.fold(
    onSuccess = { hexString ->
        // Use hex string (e.g., "a1b2c3d4...")
    },
    onFailure = { error ->
        // Handle error
    }
)

// Generate Base64 with options
val base64Result = Random.generateBytesAsBase64(
    size = 32,
    flags = Base64Flags.UrlSafeNoPadding // URL-safe, no padding
)
base64Result.fold(
    onSuccess = { base64String ->
        // Use URL-safe string (e.g., "a1b2c3d4-_")
    },
    onFailure = { error ->
        // Handle error
    }
)
```

#### EnhancedRandom Class

Advanced random generation with additional hardware entropy:

```kotlin
class YourActivity : AppCompatActivity() {
    private suspend fun useEnhancedRandom() {
        // Create EnhancedRandom instance
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

        // Generate enhanced hex string
        val hexResult = EnhancedRandom.generateEnhancedBytesAsHex(context, 32)
        hexResult.fold(
            onSuccess = { hexString ->
                // Use hex string
            },
            onFailure = { error ->
                // Handle error
            }
        )

        // Generate enhanced Base64
        val base64Result = EnhancedRandom.generateEnhancedBytesAsBase64(
            context = context,
            size = 32,
            flags = Base64Flags.UrlSafe
        )
        base64Result.fold(
            onSuccess = { base64String ->
                // Use base64 string
            },
            onFailure = { error ->
                // Handle error
            }
        )
    }
}
```

## Base64 Encoding Options

The library provides four Base64 encoding configurations via the `Base64Flags` sealed interface:

1. `Base64Flags.Default`: Standard Base64 with padding
2. `Base64Flags.NoPadding`: Standard Base64 without padding
3. `Base64Flags.UrlSafe`: URL-safe Base64 with padding (uses `-` and `_` instead of `+` and `/`)
4. `Base64Flags.UrlSafeNoPadding`: URL-safe Base64 without padding

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
val exposed = secureBytes.bytes // No cleanup!
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

## Releases

- [v0.1.0](docs/releases/v0.1.0.md)

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Support

For questions and support:
- Use the GitHub issue tracker for bug reports and feature requests
- Check our [Security Policy](SECURITY.md) for reporting security issues
- Email mavbozo@pm.me for sensitive security reports
