package com.ayushcodes.weatherapp // Defines the package name for the application

// DATA CLASS FOR COORDINATES

// This data class represents the geographic coordinates (latitude and longitude) from the OpenWeatherMap API.
data class Coord(
    val lat: Double, // The latitude of the location
    val lon: Double // The longitude of the location
) // A data class that represents the geographic coordinates from the OpenWeatherMap API
