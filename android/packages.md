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

### Key Derivation
The KeyDerivation class implements HKDF (HMAC-based Key Derivation Function) with domain separation:

```kotlin
val derivedKey = KeyDerivation.deriveKey(
    masterKey = masterKey,
    domain = "myapp.encryption",
    context = "user-data-key"
)
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

For implementation details, see [Security Architecture](https://github.com/mavbozo/AndroidSecureCrypto/blob/main/docs/security.md).

# Package com.mavbozo.androidsecurecrypto.internal

Internal implementation details. Not part of the public API.
