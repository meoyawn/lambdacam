-optimizationpasses 5
-dontpreverify

## TODO remove this
-dontobfuscate

-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    static void checkParameterIsNotNull(java.lang.Object, java.lang.String);
}
-keepclassmembers class **$WhenMappings {
    <fields>;
}

-dontwarn com.yalantis.ucrop.**
-dontwarn android.support.v4.**
