# Proguard rules for Anisuge KMP
# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /opt/android-sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.kts.

-keep class to.kuudere.anisuge.data.models.** { *; }
-keepattributes Signature,Annotation,InnerClasses,EnclosingMethod

# RxFFmpeg rules
-keep class io.microshow.rxffmpeg.** { *; }
-dontwarn io.microshow.rxffmpeg.**
-keepclasseswithmembernames class io.microshow.rxffmpeg.RxFFmpegInvoke {
    native <methods>;
}
