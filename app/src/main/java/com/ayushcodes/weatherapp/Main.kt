package com.ayushcodes.weatherapp // Defines the package name for the application

// DATA CLASS FOR MAIN WEATHER DATA

// This data class represents the main weather data from the OpenWeatherMap API.
// It contains information about the temperature, pressure, humidity, and sea level.
data class Main(
    val feels_like: Double, // The "feels like" temperature
    val humidity: Int, // The humidity percentage
    val pressure: Int, // The atmospheric pressure
    val sea_level: Int, // The sea level pressure
    val temp: Double, // The current temperature
    val temp_max: Double, // The maximum temperature
    val temp_min: Double // The minimum temperature
) // A data class that represents the main weather data from the OpenWeatherMap API
