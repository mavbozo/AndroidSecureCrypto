// gradle/build.gradle.kts
import java.util.Properties
import java.io.File
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.get
import org.gradle.api.publish.PublishingExtension
import org.gradle.plugins.signing.SigningExtension
import org.jetbrains.kotlin.com.intellij.util.ConcurrencyUtil

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    `maven-publish`
    id("signing")
}

// Version and Version Group
val libraryVersion = "0.1.0"
val publishedGroupId = "com.mavbozo.securecrypto"

// Load local.properties
val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        load(localPropertiesFile.inputStream())
    }
}

// Need to import extensions
val publishing = extensions.getByType<PublishingExtension>()
val signing = extensions.getByType<SigningExtension>()

android {
    namespace = "com.mavbozo.securecrypto"
    compileSdk = 34

    defaultConfig {

        minSdk = 23

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")  // Added for library
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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

    testOptions {
        unitTests {
            isIncludeAndroidResources = true  // Required for Robolectric
            all {
                it.systemProperty("org.bouncycastle.provider.order", "1")
            }
        }
    }
}

dependencies {

    // Concurrency
    implementation(libs.bundles.coroutines)

    // Crypto dependency

    implementation(libs.androidx.security.crypto)

    // Testing dependencies
    testImplementation(libs.bundles.testing)
    testImplementation(libs.robolectric)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

afterEvaluate {
    publishing {
        publications {
            register<MavenPublication>("release") {
                groupId = publishedGroupId
                artifactId = "securecrypto"
                version = libraryVersion

                afterEvaluate {
                    from(components["release"])
                }

                pom {
                    name.set("SecureCrypto")
                    description.set("An Android cryptography library for secure key generation and encryption")
                    url.set("https://github.com/mavbozo/securecrypto")  // Replace with your repository URL

                    licenses {
                        license {
                            name.set("The MIT License")
                            url.set("https://opensource.org/license/mit")
                        }
                    }

                    developers {
                        developer {
                            id.set("mavbozo")
                            name.set("Maverick Bozo")
                            email.set("mavbozo@pm.me")
                        }
                    }
                }
            }
        }

        repositories {
            maven {
                name = "LocalRepository"
                url = layout.buildDirectory.dir("repo").get().asFile.toURI()
            }
        }
    }
}

tasks.withType<Sign>().configureEach {
    onlyIf { !project.version.toString().endsWith("SNAPSHOT") }
}

signing {
    // Check if signing info is available
    if (localProperties.containsKey("signing.keyId") &&
        localProperties.containsKey("signing.password") &&
        localProperties.containsKey("signing.secretKeyRingFile")) {

        useInMemoryPgpKeys(
            localProperties["signing.keyId"] as String?,
            localProperties["signing.secretKeyRingFile"] as String?,
            localProperties["signing.password"] as String?
        )

        // Sign all publications
        afterEvaluate {
            if (project.extensions.findByType<PublishingExtension>() != null) {
                sign(publishing.publications)
            }
        }
    } else {
        // Optional: Skip signing for non-release builds
        isRequired = false
    }
}

// Convenient task for publishing signed artifacts
tasks.register("publishSigned") {
    dependsOn("publishReleasePublicationToLocalRepositoryRepository")
}