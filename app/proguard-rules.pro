# ── App data models ────────────────────────────────────────────────────────────
-keep class com.mathcore.app.data.** { *; }
-keep class com.mathcore.app.data.model.** { *; }
-keepattributes Signature
-keepattributes *Annotation*

# ── kotlinx.serialization ──────────────────────────────────────────────────────
# Keep @Serializable classes and their serializers so reflection-based
# deserialization works after minification.
-keepclassmembers class * {
    @kotlinx.serialization.SerialName *;
}
-keep @kotlinx.serialization.Serializable class * { *; }
-keepclassmembers class * extends kotlinx.serialization.KSerializer {
    *** serializer(...);
    *** descriptor;
}
# Companion objects that hold generated serializers
-keepclassmembers class **$$serializer { *; }

# ── Ktor ───────────────────────────────────────────────────────────────────────
# Removed stale io.github.jan.supabase.** rule — that library is not in use.
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# ── Gson (used by QuestionRepository + PreferencesManager) ────────────────────
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
