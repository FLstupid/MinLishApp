# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Preserve line number information for debugging stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Room entities
-keep class com.example.minlish.data.model.** { *; }

# Firebase
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# Apache POI and its dependencies
-dontwarn org.apache.poi.**
-dontwarn org.apache.xmlbeans.**
-dontwarn org.apache.commons.compress.**
-dontwarn com.github.luben.zstd.**
-dontwarn com.graphbuilder.**
-dontwarn net.sf.saxon.**
-dontwarn javax.xml.stream.**
-dontwarn java.awt.**
-dontwarn org.apache.poi.xslf.draw.**

-keep class org.apache.poi.** { *; }
-keep class org.apache.xmlbeans.** { *; }
-keep class org.apache.commons.compress.** { *; }

# Apache POI transitive deps (log4j references bnd annotations not needed on Android)
-dontwarn org.apache.logging.log4j.**
-dontwarn aQute.bnd.annotation.**

# Google Credentials / Auth
-keep class com.google.android.libraries.identity.googleid.** { *; }

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
