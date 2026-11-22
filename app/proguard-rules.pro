# Hilt
-keepattributes *Annotation*
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.HiltAndroidApp
-keepclasseswithmembernames class * { @dagger.hilt.android.* <methods>; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# kotlinx.coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Keep ViewModel constructors
-keep class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
# Giữ nguyên vẹn dịch vụ đọc thông báo của hệ thống
-keep class * extends android.service.notification.NotificationListenerService { *; }
# Keep domain models for serialization safety (though we use Room types)
-keep class com.notepay.domain.model.** { *; }

# Compose
-keep class androidx.compose.runtime.** { *; }
-keep class com.notepay.service.NotePayNotificationListenerService { *; }
-keep class com.notepay.** { *; }
-keepattributes InnerClasses,EnclosingMethod,Signature

# ML Kit & Firebase ComponentRegistrars (Required for reflection-based discovery)
-keep class * implements com.google.firebase.components.ComponentRegistrar {
    public <init>();
    public *;
}
-keep class com.google.mlkit.** { *; }
-keep interface com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_text_common.** { *; }
-keep class com.google.android.gms.vision.** { *; }
-dontwarn com.google.mlkit.**

# Credential Manager & Google ID (Tránh R8 làm mất cấu trúc data khi minifyEnabled)
-keep class androidx.credentials.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }

# kotlinx.serialization & Supabase DTOs
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.SerializationKt
-keep,includedescriptorclasses class com.notepay.data.remote.supabase.dto.** { *; }
-keep,includedescriptorclasses class com.notepay.data.sync.** { *; }
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}
-if @kotlinx.serialization.Serializable class ** {
    static **$Companion Companion;
}
-keepclasseswithmembers class <1>$Companion {
    kotlinx.serialization.KSerializer serializer(...);
}
