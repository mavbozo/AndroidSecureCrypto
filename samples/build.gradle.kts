// samples/build.gradle.kts
plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    namespace = "com.mavbozo.crypto.samples"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.mavbozo.crypto.samples"
        minSdk = 23
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
    implementation(project(":android"))
    implementation(libs.bundles.coroutines)
    implementation(libs.bundles.lifecycle)
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation("com.google.android.material:material:1.11.0")
}