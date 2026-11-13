plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "si.jakobkreft.aftergleam"
    compileSdk = 37

    defaultConfig {
        applicationId = "si.jakobkreft.aftergleam"
        minSdk = 26
        targetSdk = 37
        versionCode = 14
        versionName = "1.8.1"
    }

    /**
     * Both custom apps must use the same locally controlled certificate for signature-level IPC.
     * Prefer RESEARCH_WIDGETS_* so Aftergleam and its companion resolve exactly the same key.
     * Legacy AFTERGLEAM_* values remain a fallback for standalone builds. Never commit secrets.
     */
    fun secret(env: String, prop: String): String? = System.getenv(env) ?: providers.gradleProperty(prop).orNull

    val sharedStore = secret("RESEARCH_WIDGETS_KEYSTORE", "researchWidgets.keystore")
    val sharedStorePassword = secret("RESEARCH_WIDGETS_KEYSTORE_PASSWORD", "researchWidgets.keystorePassword")
    val sharedKeyAlias = secret("RESEARCH_WIDGETS_KEY_ALIAS", "researchWidgets.keyAlias")
    val sharedKeyPassword = secret("RESEARCH_WIDGETS_KEY_PASSWORD", "researchWidgets.keyPassword")
    val releaseStore = sharedStore ?: secret("AFTERGLEAM_KEYSTORE", "aftergleam.keystore")
    val releaseStorePassword = sharedStorePassword ?: secret("AFTERGLEAM_KEYSTORE_PASSWORD", "aftergleam.keystorePassword")
    val releaseKeyAlias = sharedKeyAlias ?: secret("AFTERGLEAM_KEY_ALIAS", "aftergleam.keyAlias")
    val releaseKeyPassword = sharedKeyPassword ?: secret("AFTERGLEAM_KEY_PASSWORD", "aftergleam.keyPassword")

    // Source builds without local secrets remain unsigned. The custom fork is not F-Droid-update-compatible.
    signingConfigs {
        if (releaseStore != null) create("release") {
            storeFile = file(releaseStore)
            storePassword = releaseStorePassword
            keyAlias = releaseKeyAlias
            keyPassword = releaseKeyPassword
            enableV1Signing = false
            enableV2Signing = true
            enableV3Signing = true
        }
    }

    buildTypes {
        debug {
            // Use the shared release certificate for two-app IPC tests when configured.
            signingConfigs.findByName("release")?.let { signingConfig = it }
        }
        release {
            signingConfig = signingConfigs.findByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Reproducible builds: AGP otherwise bakes the git hash into META-INF.
            vcsInfo { include = false }
        }
    }

    buildFeatures { compose = true; buildConfig = true }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // AGP 9 ships Kotlin; kotlinOptions {} no longer exists.
    kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }

    // Keep the dependency blob out of the APK too; it also varies between builds.
    dependenciesInfo { includeInApk = false }

    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.all {
            it.testLogging { showStandardStreams = true }
        }
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
    // A virtual clock, so pacing can be tested without waiting three real seconds a few times.
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    // Robolectric for anything touching framework classes. Plain android.jar stubs return
    // nulls or throw "not mocked", so org.json and SQLite cannot be tested without it.
    testImplementation("org.robolectric:robolectric:4.16.1")
    testImplementation("androidx.test:core:1.7.0")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    // Icons come from the material (not material3) package.
    implementation("androidx.compose.material:material-icons-core")
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime.ktx)
}
