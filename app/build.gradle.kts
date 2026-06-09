import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// Read Supabase credentials from local.properties (not committed to VCS).
// If the file is missing (e.g. on CI), provide the keys via environment variables
// SUPABASE_URL and SUPABASE_KEY, or set them as empty strings for build validation.
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { stream -> localProperties.load(stream) }
}

android {
    namespace = "com.mathcore.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.mathcore.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "2.0"
        // Поддержка реальных телефонов (ARM64) + эмулятора (x86_64)
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64")
        }
        buildConfigField(
            "String", "SUPABASE_URL",
            "\"${localProperties.getProperty("SUPABASE_URL", System.getenv("SUPABASE_URL") ?: "")}\""
        )
        buildConfigField(
            "String", "SUPABASE_KEY",
            "\"${localProperties.getProperty("SUPABASE_KEY", System.getenv("SUPABASE_KEY") ?: "")}\""
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions { jvmTarget = "11" }
    buildFeatures {
        compose     = true
        buildConfig = true   // required for BuildConfig.SUPABASE_URL / KEY
    }
    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.gson)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    // Ktor HTTP client (all cached)
    implementation(libs.ktor.client.android)
    implementation(libs.ktor.client.auth)              // bearer token refresh (BUG-CRIT-4)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.websockets)
    implementation(libs.ktor.serialization.json)
    implementation(libs.coil.compose)
    debugImplementation(libs.androidx.ui.tooling)
}
