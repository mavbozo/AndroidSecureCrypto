# Package com.mavbozo.androidsecurecrypto

Core cryptographic operations with hardware-backed security features.

## Components

### Random Generation
The `Random` and `EnhancedRandom` classes provide secure random number generation:
```kotlin
val random = Random.create()
random.nextSecureBytes(32)
```

### Encryption
The `Cipher` class provides encryption and decryption operations:
```kotlin
val cipher = Cipher.create()
cipher.encryptString(key, plaintext)
```

## Security Architecture

### Hardware Security
- Hardware-backed key generation
- Secure element integration
- TEE utilization when available

### Memory Safety
- Automatic cleanup
- Secure memory handling
- Resource management

### Error Handling
- Explicit Result types
- No silent failures
- Comprehensive error reporting

For implementation details, see [Security Architecture](../README.md#security-architecture).

# Package com.mavbozo.androidsecurecrypto.internal

Internal implementation details. Not part of the public API.
