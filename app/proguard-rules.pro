# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Canvas LMS / Cyber Campus SDK (accessed directly and via reflection in CyberSessionCleaner)
-keep class io.github.chlwhdtn03.** { *; }

# Ktor cookie classes accessed via reflection in CyberSessionCleaner
-keep class io.ktor.client.HttpClient { *; }
-keep class io.ktor.client.HttpClientConfig { *; }
-keep class io.ktor.client.plugins.cookies.** { *; }

# Data module & kotlinx.serialization models
-keep class com.yourssu.data.** { *; }
-keep class com.yourssu.ssutime.v2.**$Companion { *; }
-keepattributes *Annotation*, InnerClasses, EnclosingMethod, Signature
-keepclassmembers class * {
    @kotlinx.serialization.Serializable *;
    public static ** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
