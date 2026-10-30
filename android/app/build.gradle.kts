import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

// The version is shared with the browser build: edit VERSION in the repo root.
val APP_VERSION = rootProject.file("../VERSION").readText().trim()

// Release signing for Google Play. The upload key and its password stay on this computer: create them once
// with android/tools/create-upload-key.ps1, which writes keystore.properties (both are kept out of git).
val uploadKey = rootProject.file("keystore.properties").takeIf { it.exists() }?.let { file ->
    Properties().apply { file.inputStream().use { load(it) } }
}

android {
    namespace = "com.thewayoflife.rosary"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.thewayoflife.rosary"
        minSdk = 24
        targetSdk = 37
        // versionCode must go up with every APK you install over an older one; versionName is what people see.
        versionCode = 27
        versionName = APP_VERSION

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (uploadKey != null) {
            create("upload") {
                storeFile = rootProject.file(uploadKey.getProperty("storeFile"))
                storePassword = uploadKey.getProperty("storePassword")
                keyAlias = uploadKey.getProperty("keyAlias")
                keyPassword = uploadKey.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("upload")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.09.00")

    implementation(composeBom)
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")

    testImplementation("junit:junit:4.13.2")
    // Real org.json for unit tests that load game-data.json (Android's copy is a stub on the JVM).
    testImplementation("org.json:json:20240303")
}

// Copies each debug build to android/dist/the-way-of-life-v-<version>.apk, for example
// the-way-of-life-v-0-0-1.apk.
val namedDebugApk = tasks.register<Copy>("namedDebugApk") {
    from(layout.buildDirectory.file("outputs/apk/debug/app-debug.apk"))
    into(rootProject.layout.projectDirectory.dir("dist"))
    rename { "the-way-of-life-v-${APP_VERSION.replace('.', '-')}.apk" }
}
tasks.matching { it.name == "assembleDebug" }.configureEach { finalizedBy(namedDebugApk) }

// Copies each release bundle for Google Play to android/dist/the-way-of-life-v-<version>.aab.
val namedReleaseBundle = tasks.register<Copy>("namedReleaseBundle") {
    from(layout.buildDirectory.file("outputs/bundle/release/app-release.aab"))
    into(rootProject.layout.projectDirectory.dir("dist"))
    rename { "the-way-of-life-v-${APP_VERSION.replace('.', '-')}.aab" }
}
tasks.matching { it.name == "bundleRelease" }.configureEach { finalizedBy(namedReleaseBundle) }
