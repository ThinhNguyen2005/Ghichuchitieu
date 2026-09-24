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


