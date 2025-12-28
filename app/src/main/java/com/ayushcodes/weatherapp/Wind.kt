package com.ayushcodes.weatherapp // Defines the package name for this file

// Data class representing wind conditions
data class Wind(
    val deg: Int, // Wind direction in degrees
    val gust: Double, // Wind gust speed
    val speed: Double // Wind speed
)