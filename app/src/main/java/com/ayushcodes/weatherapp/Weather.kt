package com.ayushcodes.weatherapp // Defines the package name for this file

// Data class representing weather condition details
data class Weather(
    val description: String, // Weather condition within the group (e.g., "light rain", "clear sky")
    val icon: String, // Weather icon id
    val id: Int, // Weather condition id
    val main: String // Group of weather parameters (Rain, Snow, Extreme etc.)
)