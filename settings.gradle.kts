// Configuration block for plugin management
pluginManagement {
    // Define repositories for searching plugins
    repositories {
        // Configure Google Maven repository
        google {
            // Define content inclusion rules
            content {
                includeGroupByRegex("com\\.android.*") // Include Android plugins
                includeGroupByRegex("com\\.google.*") // Include Google plugins
                includeGroupByRegex("androidx.*") // Include AndroidX plugins
            }
        }
        mavenCentral() // Add Maven Central repository
        gradlePluginPortal() // Add Gradle Plugin Portal
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

// Configuration block for dependency resolution management
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS) // Enforce usage of declared repositories
    // Define repositories for project dependencies
    repositories {
        google() // Add Google Maven repository
        mavenCentral() // Add Maven Central repository
    }
}

rootProject.name = "Weather App" // Set the name of the root project
include(":app") // Include the app module in the build