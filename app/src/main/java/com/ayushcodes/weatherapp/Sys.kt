package com.ayushcodes.weatherapp // Defines the package name for the application

// DATA CLASS FOR SYSTEM DATA

// This data class represents the system data from the OpenWeatherMap API.
// It contains information about the country, sunrise, and sunset times.
data class Sys(
    val country: String, // The country code
    val sunrise: Int, // The sunrise time
    val sunset: Int // The sunset time
) // A data class that represents the system data from the OpenWeatherApp API
