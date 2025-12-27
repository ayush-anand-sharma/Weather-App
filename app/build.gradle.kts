plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.ayushcodes.weatherapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.ayushcodes.weatherapp"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    buildFeatures{
        viewBinding = true
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

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    //noinspection UseTomlInstead // Lottie Animations...
    implementation("com.airbnb.android:lottie:6.6.7")

    //noinspection UseTomlInstead // Retrofit core library
    implementation ("com.squareup.retrofit2:retrofit:3.0.0") // Use the latest stable version

    //noinspection UseTomlInstead // Retrofit converter for Gson
    implementation ("com.squareup.retrofit2:converter-gson:3.0.0") // Use the same version as Retrofit

    // noinspection UseTomlInstead // Optional: OkHttp if not already included by Retrofit's transitive dependencies
    // implementation ("com.squareup.okhttp3:okhttp:4.9.0")

    //noinspection UseTomlInstead // SPLASH SCREEN
    implementation("androidx.core:core-splashscreen:1.0.1")

    //noinspection UseTomlInstead // FANCY TOAST
    implementation("io.github.shashank02051997:FancyToast:2.0.2")

    //noinspection UseTomlInstead
    implementation("com.google.android.gms:play-services-location:21.3.0")

    //noinspection UseTomlInstead // CUSTOM SWEET ALERT DIALOGUE
    implementation("com.github.f0ris.sweetalert:library:1.6.2")

//    //noinspection UseTomlInstead // For location services (FusedLocationProviderClient)
//    implementation("com.google.android.gms:play-services-location:21.3.0")

//    //noinspection UseTomlInstead
//    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

//    //noinspection UseTomlInstead
//    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
}