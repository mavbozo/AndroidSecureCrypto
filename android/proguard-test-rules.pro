# Additional ProGuard rules for testing

# Generate debugging information
-printusage usage.txt
-printseeds seeds.txt
-printmapping mapping.txt

# Generate a report of removed code
-dump class_files.txt

# Verify all optimizations
-whyareyoukeeping class com.mavbozo.androidsecurecrypto.**

# Additional testing-specific rules
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keepattributes Signature

# Keep test classes
-keep class com.mavbozo.androidsecurecrypto.** { *; }
-keep class androidx.test.** { *; }
