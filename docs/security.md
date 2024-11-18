# Security Architecture

## Design Principles

### 1. Hardware-First Security
- Hardware-backed cryptographic operations when available
- Automatic security level detection
- Graceful fallback to software implementations
- Hardware security attestation support

### 2. Memory Safety
- Automatic zeroing of sensitive data
- Secure memory handling patterns
- Guaranteed cleanup via structured APIs
- Process isolation enforcement
- Protection against memory dumps

### 3. Side-Channel Protection
- Constant-time operations where possible
- No branching on secret data
- Process isolation via Android Keystore
- Power analysis resistance
- Cache timing attack mitigations

### 4. Error Handling
- Explicit Result types
- No silent fallbacks
- Detailed error reporting
- Secure logging practices

## Implementation Details

### Entropy Sources
1. Primary Sources:
   - Hardware-backed SecureRandom when available
   - AndroidKeyStore for additional entropy
   - Platform SecureRandom as fallback

2. Mixing Function:
   - SHA-512 based entropy combining
   - Multiple independent sources
   - Continuous health monitoring

### Encryption Implementation
1. AES-GCM Details:
   - 256-bit keys
   - 96-bit random IVs
   - 128-bit authentication tags
   - Hardware acceleration when available

2. Ciphertext Format:
   ```
   [MAGIC_BYTES][VERSION][ALGORITHM_ID][PARAMS_LENGTH][PARAMS][CIPHERTEXT]
   ```
   - MAGIC_BYTES: "SECB" (4 bytes)
   - VERSION: 0x01 (1 byte)
   - ALGORITHM_ID: Algorithm identifier (1 byte)
   - PARAMS_LENGTH: Parameter block length (2 bytes)
   - PARAMS: Algorithm parameters (variable)
   - CIPHERTEXT: Encrypted data with authentication tag

### Memory Management
1. SecureBytes Wrapper:
   - Automatic memory zeroing
   - Structured cleanup guarantees
   - Protection from accidental exposure
   - GC-independent operation

2. Resource Handling:
   - try/finally blocks for cleanup
   - Immediate zeroing after use
   - Safe exception handling
   - Resource tracking

## Security Guarantees

### Random Number Generation
- Cryptographically secure output
- Hardware entropy when available
- Multiple entropy sources
- Continuous health monitoring
- Format validation

### Encryption
- Authenticated encryption (AEAD)
- Perfect forward secrecy
- Key separation
- IV uniqueness
- Tag validation

### General Properties
- No key material exposure
- Side-channel resistance
- Memory safety
- Process isolation
- Error handling safety

## Security Level Detection

The library provides runtime detection of security capabilities:

```kotlin
val cipher = Cipher.create().getOrThrow()
when (cipher.getProvider()) {
    // Hardware-backed provider
    "AndroidKeyStore" -> // ...
    "AndroidOpenSSL" -> // ...
    // Software fallback
    else -> // ...
}
```

## Best Practices

1. Key Management:
   ```kotlin
   // Generate key
   val keyResult = Random.generateBytes(32)
   keyResult.fold(
       onSuccess = { key ->
           try {
               // Use key
           } finally {
               key.fill(0) // Always clean up
           }
       },
       onFailure = { error ->
           // Handle error
       }
   )
   ```

2. Encryption:
   ```kotlin
   // Never reuse IVs - library handles this automatically
   val ciphertext = Cipher.encryptString(key, "data")
   
   // Always verify decryption succeeded
   val plaintext = Cipher.decryptString(key, ciphertext)
       .getOrNull() ?: handleError()
   ```

3. Error Handling:
   ```kotlin
   // Always handle both success and failure
   Random.generateBytes(32).fold(
       onSuccess = { bytes -> /* ... */ },
       onFailure = { error -> /* ... */ }
   )
   ```

## Security Reporting

If you discover a security vulnerability:

1. You can open an issue, but it would be better if you do the next steps instead. 
2. Email mavbozo@pm.me with details
3. Please provide:
   - Clear description of the issue
   - Steps to reproduce
   - Affected versions
   - Potential impact assessment
