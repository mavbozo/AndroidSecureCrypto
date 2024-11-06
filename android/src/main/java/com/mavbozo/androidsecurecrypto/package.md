# Package com.mavbozo.androidsecurecrypto

A security-first cryptographic package for Android applications, providing secure random number generation with strong security guarantees.

## Components

### Random Number Generation
- `Random` - Base secure RNG with automatic quality detection
  - Uses Android security provider infrastructure
  - Automatic memory cleanup
  - Quality detection for entropy sources
  
- `EnhancedRandom` - Enhanced RNG with additional entropy mixing
  - Android Keystore integration for additional entropy
  - Multi-source entropy mixing
  - SHA-512 based mixing function

- `SecureBytes` - Zero-on-free byte array wrapper
  - Guaranteed memory cleanup
  - Protection against toString() exposure
  - Immutable value semantics

### Encoding Configuration
- `Base64Flags` - Type-safe Base64 encoding options
  - Standard and URL-safe alphabets
  - Optional padding control
  - Constant-time operations

### Quality Assessment
- `EntropyQuality` - Entropy source classification
  - Hardware - When using Android OpenSSL provider
  - Fallback - Other secure providers

## Security Guarantees

### Memory Safety
- Zero-on-free for all sensitive data
- Cleanup on all exit paths (including exceptions)
- No sensitive data in error messages or logs

### Side-Channel Protection
- Constant-time operations where possible
- No branching on secret data
- Process isolation via Android Keystore

## Usage Examples

```kotlin
// Basic secure random generation
Random.create().fold(
    onSuccess = { random ->
        random.nextSecureBytes(32).fold(
            onSuccess = { bytes ->
                bytes.use { data ->
                    // Use data here - automatically cleaned up after
                }
            },
            onFailure = { /* Handle error */ }
        )
    },
    onFailure = { /* Handle error */ }
)

// Enhanced generation with additional entropy mixing
context?.let { ctx ->
    EnhancedRandom.generateEnhancedBytesAsBase64(
        context = ctx,
        size = 32,
        flags = Base64Flags.UrlSafe
    ).fold(
        onSuccess = { encoded -> /* Use encoded data */ },
        onFailure = { /* Handle error */ }
    )
}
```
