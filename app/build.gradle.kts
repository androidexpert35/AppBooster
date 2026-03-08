
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
}

// ── Release signing (CI-only, resolved from env vars or gradle.properties) ──
val releaseKeystorePath: String? = providers.gradleProperty("ghReleaseKeystorePath")
    .orElse(providers.environmentVariable("GH_RELEASE_KEYSTORE_PATH"))
    .orNull
val releaseKeyAlias: String? = providers.gradleProperty("ghReleaseKeyAlias")
    .orElse(providers.environmentVariable("GH_RELEASE_KEY_ALIAS"))
    .orNull
val releaseKeyPassword: String? = providers.gradleProperty("ghReleaseKeyPassword")
    .orElse(providers.environmentVariable("GH_RELEASE_KEY_PASSWORD"))
    .orNull
val releaseStorePassword: String? = providers.gradleProperty("ghReleaseStorePassword")
    .orElse(providers.environmentVariable("GH_RELEASE_STORE_PASSWORD"))
    .orNull
val hasReleaseSigning: Boolean = listOf(
    releaseKeystorePath, releaseKeyAlias, releaseKeyPassword, releaseStorePassword
).none { it.isNullOrBlank() }

android {
    namespace = "com.tony.appbooster"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.tony.appbooster"
        minSdk = 26
        targetSdk = 36

        // ── Versioning (single source of truth) ──
        // Bump these three values for each release; versionCode is derived automatically.
        val major = 1
        val minor = 0
        val patch = 1
        versionCode = major * 10000 + minor * 100 + patch   // e.g. 1.2.3 → 10203
        versionName = "$major.$minor.$patch"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                // CI injects the keystore path at runtime so local debug builds stay zero-config.
                storeFile = file(releaseKeystorePath ?: error("Missing release keystore path."))
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    packaging {
        resources {
            excludes += "META-INF/AL2.0"
            excludes += "META-INF/LGPL2.1"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    buildFeatures {
        compose = true
        aidl = true
        buildConfig = true
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
    implementation(libs.material3.windowsizeclass)
    implementation(libs.androidx.navigation.compose)
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // ViewModel
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)


    // WorkManager (background optimization with foreground notification)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    // Material Icons Extended
    implementation(libs.androidx.compose.material3.icons.extended)

    // DataStore
    implementation(libs.androidx.datastore.preferences)


    // adb
    implementation(libs.dadb)

    // Shizuku - for privileged shell access
    implementation(libs.shizuku.api)
    implementation(libs.shizuku.provider)

}