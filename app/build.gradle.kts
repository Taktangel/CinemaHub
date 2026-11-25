plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.cinemahub.app"
    compileSdk = 36
    buildFeatures { compose = true; buildConfig = true }
    defaultConfig {
        applicationId = "com.cinemahub.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 7
        versionName = "0.6.0"
        // Optional personal-build key. Never commit an API key to the repository.
        val tmdbKey = providers.environmentVariable("TMDB_API_KEY").orNull.orEmpty()
        val sanitizedKey = tmdbKey.replace("\\", "\\\\").replace("\"", "\\\"")
        buildConfigField("String", "TMDB_API_KEY", "\"" + sanitizedKey + "\"")
    }
    // A persistent signing certificate makes Android updates installable over
    // previous CinemaHub builds. Never put key material into Git.
    val keyFile = providers.environmentVariable("CINEMAHUB_KEYSTORE_PATH").orNull
    if (!keyFile.isNullOrBlank()) {
        signingConfigs {
            create("persistentDebug") {
                storeFile = file(keyFile)
                storePassword = providers.environmentVariable("CINEMAHUB_STORE_PASSWORD").orNull
                keyAlias = providers.environmentVariable("CINEMAHUB_KEY_ALIAS").orNull
                keyPassword = providers.environmentVariable("CINEMAHUB_KEY_PASSWORD").orNull
            }
        }
        buildTypes {
            getByName("debug") {
                signingConfig = signingConfigs.getByName("persistentDebug")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.04.01"))
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.9")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:okhttp-dnsoverhttps:4.12.0")
    implementation("androidx.media3:media3-exoplayer:1.11.1")
    implementation("androidx.media3:media3-exoplayer-hls:1.11.1")
    implementation("androidx.media3:media3-exoplayer-dash:1.11.1")
    implementation("androidx.media3:media3-ui:1.11.1")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
