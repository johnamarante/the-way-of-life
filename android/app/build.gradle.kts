plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

// The version is shared with the browser build: edit VERSION in the repo root.
val APP_VERSION = rootProject.file("../VERSION").readText().trim()

android {
    namespace = "com.thewayoflife.rosary"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.thewayoflife.rosary"
        minSdk = 24
        targetSdk = 37
        // versionCode must go up with every APK you install over an older one; versionName is what people see.
        versionCode = 13
        versionName = APP_VERSION

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
