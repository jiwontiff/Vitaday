plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("org.jetbrains.kotlin.kapt") // Room용 kapt
    id("com.google.gms.google-services") // Firebase용
}

android {
    namespace = "edu.sswu.vitaday"
    compileSdk = 36

    defaultConfig {
        applicationId = "edu.sswu.vitaday"
        minSdk = 21
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
}

dependencies {
    // 기본 Android 라이브러리
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // ---------- ✅ Room (DB) ----------
    implementation("androidx.room:room-runtime:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    // ---------- ✅ Firebase & Google Auth ----------
    // Firebase BoM (버전 통합 관리)
    implementation(platform("com.google.firebase:firebase-bom:32.8.1"))

    // Firebase Authentication & Analytics
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-auth-ktx")
    //implementation("com.google.firebase:firebase-common-ktx")
    implementation("com.google.firebase:firebase-analytics")

    // Firebase UI for Auth
    implementation("com.firebaseui:firebase-ui-auth:8.0.2")

    // ✅ (중요) Google Sign-In / Smart Lock  -> 추가
    implementation("com.google.android.gms:play-services-auth:20.7.0")

    // Google 계정 연동용 Credential Manager
    implementation("androidx.credentials:credentials:1.3.0")
    //implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    //implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    // ---------- ✅ ViewModel + Coroutine ----------
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // ---------- ✅ Navigation Component ----------
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.7")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.7")

    // ---------- ✅ 테스트 ----------
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // ✅ Fragment KTX (by viewModels() 사용을 위해 필수!)
    implementation("androidx.fragment:fragment-ktx:1.6.2")
}
