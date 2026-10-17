-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
-dontwarn org.jetbrains.annotations.**

# Reflexão para DexClassLoader / entrypoint da 2ª fase
-keep class * extends android.app.Activity
-keepclassmembers class * {
    public <init>(...);
    public static void entry(...);
    public static void start(android.content.Context);
}
-keep interface com.serv.app.** { *; }
-keep class com.serv.app.core.Dropper { *; }

# Não otimizar nada do kotlin stdlib que use reflexão interna
-keep class kotlin.Metadata { *; }
