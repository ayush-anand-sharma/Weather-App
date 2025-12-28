package com.ayushcodes.weatherapp // Defines the package name for this file

// Data class representing system information like country, sunrise, and sunset
data class Sys(
    val country: String, // The country code (e.g., "US", "IN")
    val sunrise: Int, // The sunrise time in Unix timestamp format
    val sunset: Int // The sunset time in Unix timestamp format
)