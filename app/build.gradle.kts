plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)

    id("com.google.gms.google-services")

}

android {
    namespace = "com.example.uth_socials"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.uth_socials"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "19.11.2025"

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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}


dependencies {
    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:34.5.0"))
    implementation("com.google.firebase:firebase-auth:24.0.1") // Nếu cần xác thực
    implementation("com.google.firebase:firebase-firestore") // Database chính
    implementation("com.google.firebase:firebase-storage") // Lưu trữ ảnh
    implementation("com.google.firebase:firebase-analytics")

// Jetpack Compose
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")
    implementation("androidx.navigation:navigation-compose:2.9.5") // Để điều hướng
    implementation("io.coil-kt:coil-compose:2.7.0")

    // gửi thông báo
    implementation("com.google.firebase:firebase-messaging")
    implementation("com.google.firebase:firebase-messaging-ktx:24.0.0")

    // material
    implementation("androidx.compose.material:material:1.6.8")

// Coil (để tải ảnh từ URL)
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4") // Kiểm tra phiên bản mới nhất
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.foundation:foundation:1.9.3")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.compose.foundation.layout)
    implementation(libs.androidx.compose.ui.text)
    implementation(libs.androidx.compose.ui)
    implementation(libs.firebase.crashlytics.buildtools)
    implementation(libs.firebase.ai)
    implementation(libs.androidx.compose.animation)
    implementation(libs.volley)
    implementation(libs.litert.support.api)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.room.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:34.4.0"))
    implementation("com.google.firebase:firebase-auth") // Nếu cần xác thực
    implementation("com.google.firebase:firebase-firestore") // Database chính
    implementation("com.google.firebase:firebase-storage") // Lưu trữ ảnh
    implementation("com.google.firebase:firebase-analytics")

    // Jetpack Compose
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.3")
    implementation("androidx.navigation:navigation-compose:2.9.5") // Để điều hướng

    // gửi thông báo
    implementation("com.google.firebase:firebase-messaging-ktx:24.0.0")

    // Coil (để tải ảnh từ URL)
    implementation("io.coil-kt:coil-compose:2.7.0") // Dùng cho AsyncImage
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.foundation:foundation:1.9.3")
    implementation("com.google.android.gms:play-services-auth:21.2.0")//Auth API GG

    // Coroutines (để quản lý luồng)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")
}