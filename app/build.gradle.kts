// Block to apply plugins to the project
plugins {
    alias(libs.plugins.android.application) // Apply the Android Application plugin
}

// Configuration block for Android build settings
android {
    namespace = "com.ayushcodes.weatherapp" // Set the namespace for the application
    compileSdk = 36 // Set the compile SDK version to 36

    // Default configuration block for the application
    defaultConfig {
        applicationId = "com.ayushcodes.weatherapp" // Set the unique application ID
        minSdk = 24 // Set the minimum SDK version supported
        targetSdk = 36 // Set the target SDK version
        versionCode = 1 // Set the internal version code
        versionName = "1.0" // Set the visible version name

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner" // Set the test runner for instrumentation tests
    }

    // Block for configuring build types (release, debug, etc.)
    buildTypes {
        // Configuration for the release build type
        release {
            isMinifyEnabled = false // Disable code shrinking/obfuscation for release
            // Configure ProGuard rules
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), // Use default Android optimize rules
                "proguard-rules.pro" // Use project specific ProGuard rules
            )
        }
    }
    // Block for enabling specific build features
    buildFeatures{
        viewBinding = true // Enable View Binding feature
    }
    // Block for Java compilation options
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11 // Set Java source compatibility to 11
        targetCompatibility = JavaVersion.VERSION_11 // Set Java target compatibility to 11
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }
}

// Block for defining project dependencies
dependencies {

    implementation(libs.androidx.core.ktx) // Add dependency for AndroidX Core KTX
    implementation(libs.androidx.appcompat) // Add dependency for AppCompat
    implementation(libs.material) // Add dependency for Material Design
    implementation(libs.androidx.activity) // Add dependency for Activity
    implementation(libs.androidx.constraintlayout) // Add dependency for ConstraintLayout
    testImplementation(libs.junit) // Add dependency for JUnit unit testing
    androidTestImplementation(libs.androidx.junit) // Add dependency for AndroidX JUnit
    androidTestImplementation(libs.androidx.espresso.core) // Add dependency for Espresso Core

    // Lottie Animations...
    implementation(libs.lottie) // Add dependency for Lottie Animations

    // Retrofit core library
    implementation (libs.retrofit) // Add dependency for Retrofit networking library

    // Retrofit converter for Gson
    implementation (libs.retrofit.converter.gson) // Add dependency for Gson converter

    // SPLASH SCREEN
    implementation(libs.androidx.core.splashscreen) // Add dependency for Splash Screen

    // FANCY TOAST
    implementation(libs.fancyToast) // Add dependency for FancyToast

    // Play Services Location
    implementation(libs.play.services.location) // Add dependency for Play Services Location

    // CUSTOM SWEET ALERT DIALOGUE
    implementation(libs.sweetalert) // Add dependency for Sweet Alert Dialog
}