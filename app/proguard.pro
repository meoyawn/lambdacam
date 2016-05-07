-optimizationpasses 5
-dontpreverify

-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    static void checkParameterIsNotNull(java.lang.Object, java.lang.String);
}

-dontwarn com.yalantis.ucrop.**
-dontwarn android.support.v4.**
