plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.fullhipertrofia.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.fullhipertrofia.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 2
        versionName = "1.1"
    }

    signingConfigs {
        create("fh") {
            storeFile = rootProject.file("keystore/fh.jks")
            storePassword = "fullhiper"
            keyAlias = "fh"
            keyPassword = "fullhiper"
        }
    }

    buildTypes {
        debug { signingConfig = signingConfigs.getByName("fh") }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("fh")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-ktx:1.9.1")
    implementation("androidx.webkit:webkit:1.11.0")
}
