# Hilt, Room, Compose and kotlinx.serialization ship their own consumer rules.

# Keep @Serializable classes' generated serializers (program seed, backups).
-keepclassmembers @kotlinx.serialization.Serializable class com.hyperathlete.app.** {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
-keepattributes *Annotation*, InnerClasses, Signature

# Type-safe navigation looks routes up by class at runtime (navigate(Dest.Main)); keep them intact.
-keep class com.hyperathlete.app.ui.Dest { *; }
-keep class com.hyperathlete.app.ui.Dest$* { *; }
