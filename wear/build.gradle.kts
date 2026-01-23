plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.tony.appbooster.wear"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.tony.appbooster.wear"
        minSdk = 30  // Wear OS 3+ (Android 11+)
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
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
        buildConfig = true
    }
}

dependencies {
    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Compose (using BOM for consistent versions)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Wear OS Compose
    implementation(libs.wear.compose.foundation)
    implementation(libs.wear.compose.material3)
    implementation(libs.wear.compose.navigation)

    // Horologist - Wear OS helpers from Google
    implementation(libs.horologist.compose.layout)
    implementation(libs.horologist.compose.material)

    // Wear Tiles (for quick access tile)
    implementation(libs.wear.tiles)
    implementation(libs.wear.tiles.material)
    implementation("com.google.guava:guava:32.1.3-android")

    // Wear Input (for text input via RemoteInput)
    implementation("androidx.wear:wear-input:1.1.0")

    // Material Icons Extended
    implementation(libs.androidx.compose.material3.icons.extended)

    // ViewModel
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Hilt DI
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // DataStore for settings persistence
    implementation(libs.androidx.datastore.preferences)

    // dadb - Pure Kotlin ADB client (fallback for self-connection)
    implementation(libs.dadb)

    // Wearable Data Layer - for phone communication (bridge mode)
    implementation(libs.play.services.wearable)
    implementation(libs.kotlinx.coroutines.play.services)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
