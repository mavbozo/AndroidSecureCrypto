# Security Policy

## Supported Versions

Currently, we only release security updates for the latest major version of SecureCrypto.

| Version | Supported          |
| ------- | ------------------ |
| 0.1.x   | :white_check_mark: |
| < 0.1.0 | :x:                |

## Security Update Policy

- Security patches are released as soon as possible after a vulnerability is confirmed
- Minor version updates include security patches without breaking changes
- All security updates are documented in our release notes
- Critical vulnerabilities trigger an immediate patch release
- Users are notified of security updates through our GitHub Security Advisories

## Reporting a Vulnerability

I take security vulnerabilities seriously. You can report security vulnerabilities through public GitHub issues, but it would be better to 

Instead:

1. Email me at mavbozo@pm.me with:
   - Description of the vulnerability
   - Steps to reproduce
   - Potential impact
   - Any supporting materials (PoC code, logs)

2. I will follow up with:
   - Confirmation of the vulnerability
   - Our plans for a fix
   - Any questions I have about your report

3. Once a fix is ready, I will:
   - Notify you for review
   - Release the fix
   - Credit you in our security advisory (unless you prefer to remain anonymous)

## Security Requirements

### Hardware Security Module (HSM) Usage

SecureCrypto attempts to use hardware security features when available:
- StrongBox Keymaster
- Trusted Execution Environment (TEE)
- Hardware-backed Android Keystore

When hardware security is unavailable, the library falls back to software implementations with clear security level indicators.

### Memory Security

The library implements strict memory handling practices:
- All sensitive data is zeroed after use
- Secure memory wrappers ensure cleanup
- No sensitive data in logs or exceptions
- Protection against memory dumps

### Side-Channel Attack Prevention

We implement protections against:
- Timing attacks
- Power analysis
- Cache attacks
- Memory access patterns

## Known Security Limitations

1. Random Number Generation
   - Hardware entropy quality varies by device
   - Software fallback has lower security guarantees
   - Entropy assessment is best-effort

## Verification

### Build Verification

We provide SHA-256 checksums for all releases. Verify downloaded artifacts with:

```bash
sha256sum securecrypto-0.1.0.aar
```

Compare with the checksums in our release notes.

### Security Assessments

The library undergoes:
- Regular security audits
- Static analysis
- Dynamic analysis
- Fuzzing tests
- Memory leak detection
- Side-channel analysis

## Best Practices

### Integration Requirements

When integrating SecureCrypto:

1. Version Requirements
   - Target API level 23 or higher
   - Keep library updated to latest version
   - Monitor security advisories

2. Runtime Environment
   - Verify hardware security feature availability
   - Handle security level downgrades gracefully
   - Implement proper error handling

3. Memory Management
   - Use `SecureBytes` for sensitive data
   - Implement proper cleanup in `finally` blocks
   - Avoid logging sensitive information

## Release Signing

All releases are signed with our release key. The public key fingerprint is:

```
[To be added after key generation]
```

## Threat Model

SecureCrypto is designed to protect against:

1. Local Threats
   - Memory dumps
   - Process inspection
   - Debugger attachment
   - Side-channel attacks

2. Implementation Threats
   - Timing attacks
   - Memory leaks
   - Improper cleanup
   - Error information leaks

3. Known Limitations
   - Cannot protect against compromised OS
   - Limited protection against physical attacks
   - Dependent on platform security features

## Code Verification

We encourage users to:
1. Review our source code
2. Run our test suite
3. Conduct security assessments
4. Report any findings

## Incident Response

In case of a security incident:

1. We will:
   - Investigate promptly
   - Issue fixes quickly
   - Notify affected users
   - Publish post-mortem analysis

2. Users should:
   - Update immediately
   - Monitor our security advisories
   - Follow mitigation instructions
   - Report any issues encountered

## Contact

- Security issues: mavbozo@pm.me
- PGP Key: [To be added]

For non-security issues, use GitHub issues.
