# --- Flutter Play Store Split Install ---
-keep class com.google.android.play.core.** { *; }

# --- Zebra RFID SDK (reflection-heavy) ---
-keep class com.zebra.** { *; }

# --- JSch (SFTP) ---
-keep class com.jcraft.jsch.** { *; }

# --- Xerces XML Parser ---
-keep class org.apache.xerces.** { *; }
-keep class org.w3c.dom.** { *; }

# --- BouncyCastle Crypto ---
-keep class org.bouncycastle.** { *; }

# --- LLRP Toolkit (used by Zebra) ---
-keep class org.llrp.** { *; }

# Prevent warnings
-dontwarn com.google.android.play.core.**
-dontwarn com.jcraft.jsch.**
-dontwarn org.apache.xerces.**
-dontwarn org.bouncycastle.**
-dontwarn org.llrp.**

# ============================================
# ✅ FIX: javax.lang.model (Google Error Prone)
# ============================================
-dontwarn javax.lang.model.**
-dontwarn com.google.errorprone.annotations.**
-keep class javax.lang.model.** { *; }
-keep class com.google.errorprone.annotations.** { *; }

# ============================================
# Additional javax warnings
# ============================================
-dontwarn javax.annotation.**
-dontwarn javax.tools.**
-keep class javax.annotation.** { *; }

# ============================================
# Keep Flutter classes
# ============================================
-keep class io.flutter.app.** { *; }
-keep class io.flutter.plugin.** { *; }
-keep class io.flutter.util.** { *; }
-keep class io.flutter.view.** { *; }
-keep class io.flutter.** { *; }
-keep class io.flutter.plugins.** { *; }

# ============================================
# Keep native methods
# ============================================
-keepclasseswithmembernames class * {
    native <methods>;
}

# ============================================
# Keep attributes for debugging
# ============================================
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ============================================
# Keep Parcelable implementations
# ============================================
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# ============================================
# Keep Serializable classes
# ============================================
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ============================================
# Suppress warnings for common missing classes
# ============================================
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**
-dontwarn sun.security.**
-dontwarn com.sun.**