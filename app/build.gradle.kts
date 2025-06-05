plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.sigma.openfashion"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.sigma.openfashion"
        minSdk = 31
        targetSdk = 35
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
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // OkHttp + Retrofit (для работы с REST‑запросами, если потребуется)
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.logging.interceptor)

    // Glide (для загрузки изображений в будущем)
    implementation(libs.glide)
    annotationProcessor(libs.compiler)

    // AndroidX Lifecycle (ViewModel, LiveData)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.livedata)

    // ConstraintLayout (для разметки)
    implementation(libs.constraintlayout.v214)

    // Navigation Component
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)

    // Блюр
    implementation("com.github.Dimezis:BlurView:version-2.0.6")

    // Палетка
    implementation("androidx.palette:palette:1.0.0")
}