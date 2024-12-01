# Consumer rules for AndroidSecureCrypto library
# These rules will be applied to apps that use this library

# Keep all public API classes and methods
-keep public class com.mavbozo.androidsecurecrypto.Random { *; }
-keep public class com.mavbozo.androidsecurecrypto.EnhancedRandom { *; }
-keep public class com.mavbozo.androidsecurecrypto.Base64Flags { *; }

# Keep all security-sensitive classes
-keep class com.mavbozo.androidsecurecrypto.SecureBytes { *; }
-keep class com.mavbozo.androidsecurecrypto.EntropyQuality { *; }

# Preserve Result class for error handling
-keepclassmembers class kotlin.Result {
    public static final kotlin.Result$Companion Companion;
}

# Keep coroutines implementation for async operations
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# Prevent renaming of security-critical methods
-keepclassmembernames class * {
    @androidx.annotation.Keep *;
}

# Keep cryptographic providers
-keep class javax.crypto.** { *; }
-keep class javax.security.** { *; }
-keep class java.security.** { *; }

# Preserve source file names and line numbers for better debugging
-keepattributes SourceFile,LineNumberTable

# Keep security annotations
-keepattributes *Annotation*

# Handle Java 8+ string concat factory
-dontwarn java.lang.invoke.StringConcatFactory
-keep class java.lang.invoke.StringConcatFactory { *; }

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

# Keep KeyDerivation public API and implementation
-keep public class com.mavbozo.androidsecurecrypto.KeyDerivation { *; }
-keep public enum com.mavbozo.androidsecurecrypto.HkdfAlgorithm { 
    *;
}

# Keep companion object and its methods
-keepclassmembers class com.mavbozo.androidsecurecrypto.KeyDerivation$Companion {
    public static <methods>;
}

# Keep HMAC implementations
-keep class javax.crypto.Mac { *; }
-keep class javax.crypto.spec.SecretKeySpec { *; }
