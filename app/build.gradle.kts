import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}



val signingProps = Properties().apply {
    val f = rootProject.file("app/signing.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

// Release automation supplies these environment variables from the version tag.
// They remain unset locally, so local builds keep the existing 1.0/1 defaults.
val resolvedVersionName = providers.environmentVariable("NOTEPAY_VERSION_NAME")
    .orElse("1.0")
    .get()
    .also { require(it.isNotBlank()) { "NOTEPAY_VERSION_NAME must not be blank" } }
val resolvedVersionCode = providers.environmentVariable("NOTEPAY_VERSION_CODE")
    .map { value ->
        value.toIntOrNull()
            ?: error("NOTEPAY_VERSION_CODE must be a positive integer")
    }
    .orElse(1)
    .get()
    .also {
        require(it in 1..2_100_000_000) {
            "NOTEPAY_VERSION_CODE must be between 1 and 2,100,000,000"
        }
    }

android {
    namespace = "com.notepay"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.notepay"
        // ML Kit Prompt API (Gemini Nano) yêu cầu Android 8.0 / API 26.
        minSdk = 26
        versionCode = resolvedVersionCode
        versionName = resolvedVersionName
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
    }

    signingConfigs {
        create("release") {
            if (signingProps.isNotEmpty()) {
                storeFile = rootProject.file("app/${signingProps.getProperty("storeFile", "release.keystore")}")
                storePassword = signingProps.getProperty("storePassword")
                keyAlias = signingProps.getProperty("keyAlias")
                keyPassword = signingProps.getProperty("keyPassword")
            }
        }
    }

    // Play has Internet access; Local owns offline notification capture.
    // Shared UI and business logic stay in src/main.
    flavorDimensions += "distribution"

    productFlavors {
        create("play") {
            dimension = "distribution"
        }

        create("local") {
            dimension = "distribution"
            applicationIdSuffix = ".local"
            versionNameSuffix = "-local"
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
            // Nếu đã config keystore qua app/signing.properties thì dùng; nếu chưa,
            // build vẫn pass nhưng APK sẽ chưa được ký — cần keystore để đưa lên Play Store.
            if (signingConfigs.findByName("release")?.storeFile?.exists() == true) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }


    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources.excludes += setOf(
            "/META-INF/{AL2.0,LGPL2.1}",
            "/META-INF/LICENSE*",
            "/META-INF/NOTICE*",
        )
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
            all {
                it.jvmArgs("-XX:+EnableDynamicAgentLoading", "-Xmx1024m")
            }
        }
    }

    @Suppress("UnstableApiUsage")
    bundle {
        abi {
            enableSplit = true // Giúp người dùng chỉ tải đúng chip máy họ, giảm hàng chục MB
        }
        density {
            enableSplit = true // Tối ưu ảnh theo độ phân giải màn hình
        }
        language {
            enableSplit = false // Đảm bảo đổi ngôn ngữ trong app không bị lỗi thiếu chữ
        }
    }
}
kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        freeCompilerArgs.add("-opt-in=kotlin.time.ExperimentalTime")
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    // Splash screen
    implementation(libs.androidx.core.splashscreen)

    // Backdrop for Liquid Slider/Toggle
    implementation(libs.backdrop)

    // Image loading (VietQR CDN logos + QR images)
    implementation(libs.coil.compose)
    implementation(libs.coil.network)

    // Navigation
    implementation(libs.androidx.navigation.compose)
    implementation(libs.firebase.crashlytics.buildtools)

    // Hilt
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.compiler)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.datastore.preferences)
    ksp(libs.androidx.room.compiler)

    // Coroutines + DateTime
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.datetime)
    implementation(libs.zxing.core)

    // Gemini Nano qua Android AICore: inference local, không gửi dữ liệu tài chính lên cloud.
    implementation(libs.mlkit.genai.prompt)
    // Cloud AI fallback using official Google Generative AI Client (~300KB)
    implementation(libs.google.ai.client)
    // OCR Latin bundled in the APK: runs fully offline for Vietnamese bank screenshots.
    implementation(libs.mlkit.text.recognition)

    // WorkManager + Hilt-Work
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.hilt.work)
    ksp(libs.hilt.work.compiler)

    // Debug
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Unit test
    testImplementation(libs.kotlin.test)
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.truth)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.room.testing)

    // Instrumented
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
}
