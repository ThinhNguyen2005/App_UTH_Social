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
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.firebase.firestore)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // --- FIREBASE (Google) ---
    // Thêm Firebase Bill of Materials (BOM)
    // BOM giúp quản lý phiên bản của tất cả thư viện Firebase.
    // Bạn chỉ cần khai báo phiên bản BOM, các thư viện Firebase bên dưới
    // sẽ tự động sử dụng phiên bản tương thích nhất.
    implementation(platform("com.google.firebase:firebase-bom:33.2.0"))
    // Cloud Firestore (CSDL NoSQL) - bản KTX cho Kotlin
    implementation("com.google.firebase:firebase-firestore-ktx")
    // Firebase Storage (Lưu trữ file, ảnh) - bản KTX cho Kotlin
    implementation("com.google.firebase:firebase-storage-ktx")
    // Google Analytics for Firebase (Phân tích người dùng)
    implementation("com.google.firebase:firebase-analytics")
    // Firebase Authentication (Xác thực người dùng)
    implementation("com.google.firebase:firebase-auth")

    // --- KOTLIN COROUTINES (Xử lý bất đồng bộ) ---
    // Thư viện lõi của Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    // Hỗ trợ Coroutines cho nền tảng Android (ví dụ: Dispatchers.Main)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    // Hỗ trợ chuyển đổi Task API (của Firebase) sang Coroutines (dùng .await())
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

    // --- COIL (Tải và hiển thị ảnh) ---
    // Thư viện chính của Coil
    implementation("io.coil-kt:coil:2.6.0")
    // Thư viện hỗ trợ Coil cho Jetpack Compose (ví dụ: dùng AsyncImage)
    implementation("io.coil-kt:coil-compose:2.6.0")

    // --- ANDROIDX (Jetpack) ---
    // Navigation (Điều hướng trong Jetpack Compose)
    implementation("androidx.navigation:navigation-compose:2.7.6")
    // Lifecycle (Quản lý vòng đời)
    // Cung cấp tích hợp ViewModel cho Compose (ví dụ: hàm viewModel())
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")
    //Cung cap fun collectAsStateWithLifecycle()
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.3")
}