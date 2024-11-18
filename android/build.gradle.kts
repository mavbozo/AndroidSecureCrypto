// android/build.gradle.kts

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.deepmedia.deployer)
    alias(libs.plugins.dokka)
}

// Version constants
val projectVersion = "0.2.0"
group = "com.mavbozo.crypto"
version = "0.2.0"

android {
    namespace = "com.mavbozo.crypto.androidsecurecrypto"
    compileSdk = 34

    defaultConfig {
        minSdk = 23
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        debug {
            // Disable ProGuard for regular debug builds
            isMinifyEnabled = false
        }

        // Add a new buildType specifically for ProGuard testing
        create("debugMinified") {
            initWith(getByName("debug"))
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
                "consumer-rules.pro"
            )
            testProguardFiles("proguard-test-rules.pro")
            matchingFallbacks += listOf("debug")
        }

        // Add a specific build type for ProGuard testing
        create("proguardTest") {
            initWith(getByName("debug"))
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
                "consumer-rules.pro"
            )
            matchingFallbacks += listOf("debug")
        }

        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
                "consumer-rules.pro"
            )
        }

    }

    // Use debug for regular development and testing
    testBuildType = "debug"


    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
        freeCompilerArgs += listOf(
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=kotlinx.coroutines.FlowPreview"
        )
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
}


dependencies {
    implementation(libs.bundles.coroutines)
    implementation(libs.androidx.security.crypto)

    testImplementation(libs.bundles.testing)
    testImplementation(libs.robolectric)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}


deployer {
    content {
        androidComponents("release") {}
    }


    projectInfo {
        name.set("AndroidSecureCrypto")
        description.set("An Android cryptography library for secure key generation and encryption")
        url.set("https://github.com/mavbozo/AndroidSecureCrypto")
        groupId.set("com.mavbozo.crypto")
        artifactId.set("androidsecurecrypto")
        scm {
            url.set("https://github.com/mavbozo/AndroidSecureCrypto")
            connection.set("scm:git:git://github.com/mavbozo/AndroidSecureCrypto.git")
            developerConnection.set("scm:git:ssh://github.com/mavbozo/AndroidSecureCrypto.git")
        }
        license(MIT)
        developer("Maverick Bozo", "mavbozo@pm.me")
    }

    release {
        release.version.set(projectVersion)
        release.tag.set("v$projectVersion")
    }

    signing {
        key.set(secret("signingKey"))
        password.set(secret("signingPassword"))
    }

    centralPortalSpec {
        auth.user.set(secret("mavenCentralUsername"))
        auth.password.set(secret("mavenCentralPassword"))

        signing.key.set(secret("signingKey"))
        signing.password.set(secret("signingPassword"))

        allowMavenCentralSync = false
    }
}


// Update the verification task
tasks.register("verifyProguardOutputs") {
    // Make it depend on the proguardTest build
    dependsOn("assembleProguardTest")

    doLast {
        val mappingFile = file("build/outputs/mapping/proguardTest/mapping.txt")

        if (!mappingFile.exists()) {
            throw GradleException(
                """
                ProGuard mapping file not found at:
                ${mappingFile.absolutePath}
                
                Make sure the proguardTest build completed successfully.
                """.trimIndent()
            )
        }

        println("Found ProGuard mapping at: ${mappingFile.absolutePath}")
        println("ProGuard configuration verified successfully.")
    }
}

// Add a combined task for easier execution
tasks.register("buildWithProguard") {
    dependsOn("clean", "assembleDebug", "verifyProguardOutputs")
    tasks.findByName("verifyProguardOutputs")?.mustRunAfter("assembleDebug")
    tasks.findByName("assembleDebug")?.mustRunAfter("clean")
}


// Configuration for linking to GitHub
val githubRepo = "mavbozo/AndroidSecureCrypto"
val githubBranch = "main"

tasks.dokkaHtml.configure {
    outputDirectory.set(file("../docs/api"))

    dokkaSourceSets {
        named("main") {
            moduleName.set("AndroidSecureCrypto")

            // Link to Android SDK documentation
            noAndroidSdkLink.set(false)

            // Link to external Markdown files
            includes.from("module.md")
            includes.from("packages.md")

            // Link to samples
            samples.from(file("../samples/src/main/kotlin"))

            // Package options
            perPackageOption {
                matchingRegex.set("com\\.mavbozo\\.androidsecurecrypto.*")
                skipDeprecated.set(false)
                reportUndocumented.set(true)
                includeNonPublic.set(false)
            }
        }
    }
}

// Generate documentation as part of the build
tasks.named("build") {
    dependsOn(tasks.dokkaHtml)
}