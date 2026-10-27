// Top-level build file (AGP 9.0+: built-in Kotlin, không cần plugin org.jetbrains.kotlin.android)
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.google.services) apply false
}

// `gradlew clean` mặc định chỉ chạy :app:clean, nên thư mục build/ ở root (reports,
// audit artifacts, ảnh preview sinh tự động) bị bỏ lại mãi. Task này dọn nốt.
tasks.register<Delete>("clean") {
    delete(layout.buildDirectory)
}
