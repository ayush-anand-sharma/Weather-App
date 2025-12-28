// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false // Apply Android Application plugin but do not apply it to the root project
    alias(libs.plugins.kotlin.android) apply false // Apply Kotlin Android plugin but do not apply it to the root project
}