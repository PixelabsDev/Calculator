# =========================================================================
# Pixelabs Calculator - ProGuard & R8 Obfuscation & Shrinking Rules
# =========================================================================

# 1. Preserve SourceFile and LineNumberTable for Play Console deobfuscation
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# 2. General Kotlin & Coroutines reflection metadata
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# 3. AndroidX Room Persistence Library
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * {
    <fields>;
    <init>(...);
}
-keep @androidx.room.Dao interface * {
    <methods>;
}
-dontwarn androidx.room.paging.**

# 4. App Data Models (History entries & User feedback payload)
-keepclassmembers class com.example.data.** {
    <fields>;
    <init>(...);
}
-keepclassmembers class com.example.report.UserReport {
    <fields>;
    <init>(...);
}

# 5. Jetpack Compose
-keepclassmembers class androidx.compose.ui.platform.AndroidComposeView {
    <fields>;
    <init>(...);
}
-dontwarn androidx.compose.**

# 6. Google Play Integrity & Services
-keep class com.google.android.play.core.integrity.** { *; }
-dontwarn com.google.android.play.core.**

# 7. Zero-Logcat Production Privacy (Strip Log calls in release builds)
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int println(...);
}

# 8. R8 DEX Code Optimization & Aggressive Obfuscation
# Allow R8 to widen access modifiers and aggressively optimize class hierarchies
-allowaccessmodification
-repackageclasses ''

# 9. Android 15 Edge-to-Edge Compliance & Play Console Verification
# Preserve explicit EdgeToEdge entry points
-keep class androidx.activity.EdgeToEdge** { *; }
-keepclassmembers class androidx.activity.EdgeToEdge** { *; }
