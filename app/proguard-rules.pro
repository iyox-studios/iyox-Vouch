# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Preserve line numbers and source file names for readable crash stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Glance pulls in WorkManager 2.7.1, whose consumer rules keep InputMerger
# classes but omit their reflection-created constructors under R8 full mode.
# Without this constructor, widget rendering jobs fail and leave the loading UI.
# Match the runtime-only WorkManager base class without requiring a compile-time reference.
-keep class * extends androidx.work.*InputMerger {
    public <init>();
}

# Keep model and backup classes to ensure backup serialization/deserialization integrity
-keep class com.iyox.vouch.core.backup.** { *; }
-keep class com.iyox.vouch.core.model.** { *; }
-keepclassmembers enum * { *; }

# KotlinX Serialization rules
-keepattributes *Annotation*,InnerClasses,Signature
-dontnote kotlinx.serialization.SerializationKt
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
