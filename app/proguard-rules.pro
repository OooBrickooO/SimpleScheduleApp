# Preserve annotations & generic signatures for WebView JS bridge, Room, and SMBJ
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,RuntimeVisibleAnnotations

# WebView @JavascriptInterface bridge
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Room entities and DAOs
-keep class com.example.simpleschedule.data.local.room.** { *; }

# SMBJ + MBassador + BouncyCastle + SLF4J
-keep class com.hierynomus.smbj.** { *; }
-keep class com.hierynomus.mssmb2.** { *; }
-keep class com.hierynomus.protocol.** { *; }
-keepclassmembers class * {
    @net.engio.mbassy.listener.Handler <methods>;
}
-keep class net.engio.mbassy.** { *; }
-dontwarn net.engio.mbassy.**

-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**
-dontwarn org.bouncycastle.jsse.**
-dontwarn org.bouncycastle.jcajce.**

-keep class org.slf4j.** { *; }
-dontwarn org.slf4j.**
-keep class org.slf4j.impl.StaticLoggerBinder { *; }
-keep class org.slf4j.impl.StaticMDCBinder { *; }
-keep class org.slf4j.impl.StaticMarkerBinder { *; }

# JXL / Jsoup / OkHttp
-keep class jxl.** { *; }
-dontwarn jxl.**
-keep class org.jsoup.** { *; }
-dontwarn org.jsoup.**
-dontwarn okhttp3.**
-dontwarn okio.**
