// android/build.gradle.kts

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    id("io.deepmedia.tools.deployer") version "0.15.0"
}

// Version constants
val projectVersion = "0.1.0"
group = "com.mavbozo.crypto"
version = "0.1.0"

android {
    namespace = "com.mavbozo.androidsecurecrypto"
    compileSdk = 34

    defaultConfig {
        minSdk = 23
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
    	debug {
            // Enable ProGuard for testing in debug
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
                "consumer-rules.pro"
            )
            // Add test ProGuard rules
            testProguardFiles(
                "proguard-test-rules.pro"
            )
        }

        release {
            isMinifyEnabled = true // enable proguard for release
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
		"consumer-rules.pro"
            )
        }

    }

    testBuildType = "release"

 
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
        artifactId.set("android")
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
    // Make it depend on the build task
    dependsOn("assembleDebug")
    
    doLast {
        val mappingDirs = listOf(
            file("build/outputs/mapping/debug"),
            file("build/outputs/mapping/release")
        )
        
        var found = false
        mappingDirs.forEach { dir ->
            if (dir.exists()) {
                found = true
                println("Found ProGuard outputs at: $dir")
                dir.listFiles()?.forEach { file ->
                    println("- ${file.name}")
                }
            }
        }
        
        if (!found) {
            throw GradleException("""
                ProGuard mapping directory not found!
                Checked locations:
                ${mappingDirs.joinToString("\n")}
                
                Make sure to run assembleDebug or assembleRelease first.
                """.trimIndent())
        }
    }
}

// Add a combined task for easier execution
tasks.register("buildWithProguard") {
    dependsOn("clean", "assembleDebug", "verifyProguardOutputs")
    tasks.findByName("verifyProguardOutputs")?.mustRunAfter("assembleDebug")
    tasks.findByName("assembleDebug")?.mustRunAfter("clean")
}
