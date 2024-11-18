// Root build.gradle.kts
buildscript {
    repositories {
        google()
        mavenCentral()
    }
}

plugins {
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    kotlin("jvm") version libs.versions.kotlin.get() apply false
}

tasks.register("clean", Delete::class) {
    delete(layout.buildDirectory)
}