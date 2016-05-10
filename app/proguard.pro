-optimizationpasses 5
-dontpreverify

-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    static void checkParameterIsNotNull(java.lang.Object, java.lang.String);
}
-keepclassmembers class **$WhenMappings {
    <fields>;
}

-assumenosideeffects class timber.log.Timber {
    <methods>;
}
-dontwarn com.yalantis.ucrop.**
-dontwarn android.support.v4.**
