import org.gradle.api.provider.Provider

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

fun Provider<String>.quotedForBuildConfig(): String =
    get().replace("\\", "\\\\").replace("\"", "\\\"").let { "\"$it\"" }

val debugServerBaseUrl = providers
    .gradleProperty("WATCH_MARKET_DEBUG_BASE_URL")
    .orElse(providers.environmentVariable("WATCH_MARKET_DEBUG_BASE_URL"))
    .orElse("https://watch-market.example.com/")

val debugBearerToken = providers
    .gradleProperty("WATCH_MARKET_DEBUG_BEARER_TOKEN")
    .orElse(providers.environmentVariable("WATCH_MARKET_DEBUG_BEARER_TOKEN"))
    .orElse("")

android {
    namespace = "com.sg.watchmarket"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.sg.watchmarket"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    buildTypes {
        debug {
            buildConfigField("String", "SERVER_BASE_URL", debugServerBaseUrl.quotedForBuildConfig())
            buildConfigField("String", "BEARER_TOKEN", debugBearerToken.quotedForBuildConfig())
        }
        release {
            buildConfigField("String", "SERVER_BASE_URL", "\"\"")
            buildConfigField("String", "BEARER_TOKEN", "\"\"")
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.05.01"))
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.wear.compose:compose-foundation:1.6.2")
    implementation("androidx.wear.compose:compose-material:1.6.2")
    implementation("androidx.wear.tiles:tiles:1.6.0")
    implementation("androidx.wear.protolayout:protolayout:1.4.0")
    implementation("androidx.wear.protolayout:protolayout-material3:1.4.0")
    implementation("com.squareup.retrofit2:retrofit:3.0.0")
    implementation("com.squareup.retrofit2:converter-gson:3.0.0")
    implementation("com.google.guava:guava:33.5.0-android")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
