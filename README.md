# AndroidSecureCrypto

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![API](https://img.shields.io/badge/API-23%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=23)

A comprehensive, hardware-backed cryptography library for Android applications. The library aims to provide a complete suite of cryptographic operations with strong security guarantees and ergonomic Kotlin-first API design.

## Features

- Secure Random Number Generation
  - Hardware-backed entropy generation (when available)
  - Secure memory handling with automatic cleanup
  - Enhanced entropy mixing using Android Keystore
  - Automatic quality detection and fallback mechanisms
  
- Symmetric Encryption (AES-GCM)
  - Authenticated encryption with associated data (AEAD)
  - Hardware acceleration when available
  - Automatic IV management and tag validation
  - String, byte array, and file encryption support

## Installation

Add the dependency to your app's `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.mavbozo.crypto:android:0.2.0")
}
```

## Dokka Docs

- [API Reference](https://mavbozo.github.io/AndroidSecureCrypto)

## Quick Start

### Random Number Generation

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
```

### Encryption

```kotlin
// String encryption
val key = Random.generateBytes(32).getOrThrow()
try {
    val encrypted = Cipher.encryptString(key, "sensitive data")
        .getOrThrow()
    val decrypted = Cipher.decryptString(key, encrypted)
        .getOrThrow()
} finally {
    key.fill(0)
}
```

For more examples and detailed usage, see:
- [Random Number Generation Guide](docs/random.md)
- [Encryption Guide](docs/encryption.md)
- [Security Architecture](docs/security.md)

## Documentation

- [API Reference](https://mavbozo.github.io/AndroidSecureCrypto)
- [Security Model](docs/security.md)
- [Release Notes](docs/releases)

## Support

- File bug reports and feature requests via [GitHub Issues](https://github.com/mavbozo/androidsecurecrypto/issues)
- For security issues, see our [Security Policy](SECURITY.md)
- Questions? open github Issues or email mavbozo@pm.me

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
