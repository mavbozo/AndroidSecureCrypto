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
