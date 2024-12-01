# !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
# SECURITY WARNING: DO NOT ENABLE AGGRESSIVE OPTIMIZATIONS OR OBFUSCATION
# FOR CRYPTOGRAPHIC CODE. THIS CAN INTRODUCE TIMING VULNERABILITIES AND 
# BREAK SECURITY GUARANTEES.
# !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

# Disable optimizations for cryptographic operations to prevent timing attacks
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*,!code/allocation/variable

# Disable any field or method renaming for com.mavbozo.androidsecurecrypto classes
-keepnames class com.mavbozo.androidsecurecrypto.** { *; }

# Keep annotation information for library users
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions
-keepattributes InnerClasses

# Keep source file names and line numbers for better stack traces
-keepattributes SourceFile,LineNumberTable

# Keep com.mavbozo.androidsecurecrypto provider information
-keepattributes *Provider*

# Keep all classes in our library
-keep class com.mavbozo.androidsecurecrypto.** { *; }
-keepclassmembers class com.mavbozo.androidsecurecrypto.** { *; }

# Keep all classes that implement EntropyQuality
-keep interface com.mavbozo.androidsecurecrypto.EntropyQuality { *; }
-keep class * implements com.mavbozo.androidsecurecrypto.EntropyQuality { *; }

# Keep SecureBytes value class and its companion
-keep class com.mavbozo.androidsecurecrypto.SecureBytes {
    private final byte[] bytes;
    public static final com.mavbozo.androidsecurecrypto.SecureBytes$Companion Companion;
}

# Preserve Kotlin metadata
-keep class kotlin.Metadata { *; }

# Keep cryptographic providers and security classes
-keep class javax.crypto.** { *; }
-keep class javax.security.** { *; }
-keep class java.security.** { *; }
-keep class org.bouncycastle.** { *; }

# Keep all native methods as timing-sensitive
-keepclasseswithmembernames,allowshrinking,allowobfuscation class * {
    native <methods>;
}

# Keep coroutines implementation
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# Keep Result class and its methods
-keepclassmembers class kotlin.Result {
    public static final kotlin.Result$Companion Companion;
}

# Keep enum classes
-keepclassmembers enum * { *; }

# Keep security-sensitive fields
-keepclassmembers class * {
    @javax.crypto.* *;
    @java.security.* *;
}

# Remove logging in release while preserving security-critical logs
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    # Keep warning and error logs for security issues
    # public static *** w(...);
    # public static *** e(...);
}

# Prevent inlining/removal of security-critical code
-keep,allowshrinking class * {
    @androidx.annotation.Keep *;
}

# Handle string concatenation
-dontwarn java.lang.invoke.StringConcatFactory

# Generate detailed output files
-printmapping build/outputs/mapping/mapping.txt
-printseeds build/outputs/mapping/seeds.txt
-printusage build/outputs/mapping/usage.txt
-printconfiguration build/outputs/mapping/configuration.txt

# Keep detailed source information for stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Show which items are being kept and why
-whyareyoukeeping class com.mavbozo.androidsecurecrypto.**

# Keep all KeyDerivation classes and interfaces
-keep public class com.mavbozo.androidsecurecrypto.KeyDerivation { *; }
-keep public enum com.mavbozo.androidsecurecrypto.HkdfAlgorithm { *; }

# Protect cryptographic operations from optimization
-keep,allowoptimization class com.mavbozo.androidsecurecrypto.KeyDerivation {
    private void extractKey(byte[]);
    private void expandKey(byte[], byte[], int);
}

# Keep all method names for security auditing
-keepclassmembernames class com.mavbozo.androidsecurecrypto.KeyDerivation {
    private void extractKey(byte[]);
    private void expandKey(byte[], byte[], int);
}

# Prevent inlining of security-critical code
-keepclassmembers,allowoptimization class com.mavbozo.androidsecurecrypto.KeyDerivation {
    private static final int DEFAULT_KEY_SIZE;
    private final com.mavbozo.androidsecurecrypto.HkdfAlgorithm algorithm;
}

# Keep HkdfAlgorithm enum values and properties
-keepclassmembers enum com.mavbozo.androidsecurecrypto.HkdfAlgorithm {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    public final String hmacAlgorithm;
    public final int macLength;
}

# Keep cryptographic provider classes
-keep class javax.crypto.** { *; }
-keep class javax.crypto.spec.** { *; }
-keep class javax.security.** { *; }
-keepclassmembers class * extends javax.crypto.Mac { *; }

# Preserve stacktraces for security debugging
-keepattributes SourceFile,LineNumberTable,Exceptions,InnerClasses,Signature

# Keep all annotations for security validation
-keepattributes *Annotation*

# Protect KeyDerivation implementation
-keepclassmembers class com.mavbozo.androidsecurecrypto.KeyDerivation {
    private void extractKey(byte[]);
    private void expandKey(byte[], byte[], int);
}

# Prevent optimization of cryptographic operations
-keepclassmembers,allowoptimization class com.mavbozo.androidsecurecrypto.KeyDerivation {
    private final com.mavbozo.androidsecurecrypto.HkdfAlgorithm algorithm;
}

# Keep HkdfAlgorithm properties
-keepclassmembers enum com.mavbozo.androidsecurecrypto.HkdfAlgorithm {
    public final java.lang.String hmacAlgorithm;
    public final int macLength;
}

# Additional security for HMAC operations
-keep class javax.crypto.Mac { *; }
-keepclassmembers class javax.crypto.Mac { *; }
-keep class javax.crypto.spec.SecretKeySpec { *; }

# Keep annotations for security validation
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses

# Keep debugging info for security auditing
-keepattributes SourceFile,LineNumberTable
-keepattributes Exceptions
