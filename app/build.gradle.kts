// Block to apply plugins to the project
plugins {
    alias(libs.plugins.android.application) // Apply the Android Application plugin
    alias(libs.plugins.kotlin.android) // Apply the Kotlin Android plugin
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
    // Block for Kotlin compiler options
    kotlinOptions {
        jvmTarget = "11" // Set the JVM target version to 11
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

    //noinspection UseTomlInstead // Lottie Animations...
    implementation("com.airbnb.android:lottie:6.6.7") // Add dependency for Lottie Animations

    //noinspection UseTomlInstead // Retrofit core library
    implementation ("com.squareup.retrofit2:retrofit:3.0.0") // Add dependency for Retrofit networking library

    //noinspection UseTomlInstead // Retrofit converter for Gson
    implementation ("com.squareup.retrofit2:converter-gson:3.0.0") // Add dependency for Gson converter

    // noinspection UseTomlInstead // Optional: OkHttp if not already included by Retrofit's transitive dependencies
    // implementation ("com.squareup.okhttp3:okhttp:4.9.0")

    //noinspection UseTomlInstead // SPLASH SCREEN
    implementation("androidx.core:core-splashscreen:1.0.1") // Add dependency for Splash Screen

    //noinspection UseTomlInstead // FANCY TOAST
    implementation("io.github.shashank02051997:FancyToast:2.0.2") // Add dependency for FancyToast

    //noinspection UseTomlInstead
    implementation("com.google.android.gms:play-services-location:21.3.0") // Add dependency for Play Services Location

    //noinspection UseTomlInstead // CUSTOM SWEET ALERT DIALOGUE
    implementation("com.github.f0ris.sweetalert:library:1.6.2") // Add dependency for Sweet Alert Dialog
}