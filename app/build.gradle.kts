plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.gfpgan_android"
    compileSdk = 36
    ndkVersion = "27.1.12297006"

    defaultConfig {
        applicationId = "com.gfpgan_android"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    
    // ONNX model dosyalarının sıkıştırılmasını engelle
    aaptOptions {
        noCompress("onnx")
    }

    externalNativeBuild {
        cmake {
            path ("src/main/jni/CMakeLists.txt")
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // --- Jetpack Compose Navigation ---
    implementation("androidx.navigation:navigation-compose:2.7.6")
    
    // --- Koin for Compose ---
    implementation("io.insert-koin:koin-android:3.5.3")
    implementation("io.insert-koin:koin-androidx-compose:3.5.3")
    
    // --- Coil for Compose (Image Loading) ---
    implementation("io.coil-kt:coil:2.5.0")
    implementation("io.coil-kt:coil-compose:2.5.0")
    
    // --- Coroutines (Asynchronous Operations) ---
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    
    // --- OpenCV (For image processing effects) ---
    implementation("com.quickbirdstudios:opencv:4.5.3.0")

    // ONNX Runtime (GFPGAN için) - v1.19.2 supports IR version 10
    implementation("com.microsoft.onnxruntime:onnxruntime-android:1.19.2")
    // MediaPipe (Segmentasyon için)
    implementation("com.google.mediapipe:tasks-vision:0.10.14")
    implementation("com.google.mlkit:face-detection:16.1.7")
    // For coroutine support with ML Kit
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")
}