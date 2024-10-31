# !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
# SECURITY WARNING: DO NOT ENABLE AGGRESSIVE OPTIMIZATIONS OR OBFUSCATION
# FOR CRYPTOGRAPHIC CODE. THIS CAN INTRODUCE TIMING VULNERABILITIES AND 
# BREAK SECURITY GUARANTEES.
# !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

# Disable optimizations for cryptographic operations to prevent timing attacks
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*,!code/allocation/variable

# Disable any field or method renaming for crypto classes
-keepnames class com.mavbozo.securecrypto.** { *; }

# Keep annotation information for library users
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions
-keepattributes InnerClasses

# Keep source file names and line numbers for better stack traces
-keepattributes SourceFile,LineNumberTable

# Keep crypto provider information
-keepattributes *Provider*

# Keep all classes in our library
-keep class com.mavbozo.securecrypto.** { *; }
-keepclassmembers class com.mavbozo.securecrypto.** { *; }

# Keep all classes that implement EntropyQuality
-keep interface com.mavbozo.securecrypto.EntropyQuality { *; }
-keep class * implements com.mavbozo.securecrypto.EntropyQuality { *; }

# Keep SecureBytes value class and its companion
-keep class com.mavbozo.securecrypto.SecureBytes {
    private final byte[] bytes;
    public static final com.mavbozo.securecrypto.SecureBytes$Companion Companion;
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
