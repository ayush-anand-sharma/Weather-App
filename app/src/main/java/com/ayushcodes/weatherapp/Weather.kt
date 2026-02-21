package com.ayushcodes.weatherapp // Defines the package name for the application

// DATA CLASS FOR WEATHER

// This data class represents the weather data from the OpenWeatherMap API.
// It contains information about the weather description and main weather condition.
data class Weather(
    val description: String, // The weather description
    val main: String // The main weather condition
) // A data class that represents the weather data from the OpenWeatherMap API
