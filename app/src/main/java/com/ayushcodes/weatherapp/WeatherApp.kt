package com.ayushcodes.weatherapp // Defines the package name for the application

// DATA CLASS FOR WEATHER APP

// This data class represents the main response from the OpenWeatherMap API.
// It contains all the weather data for a specific location.
data class WeatherApp(
    val base: String, // The base station that provided the weather data
    val clouds: Clouds, // The cloud data
    val cod: Int, // The response code
    val coord: Coord, // The geographic coordinates
    val dt: Int, // The date and time of the weather data
    val id: Int, // The location ID
    val main: Main, // The main weather data
    val name: String, // The name of the location
    val sys: Sys, // The system data
    val timezone: Int, // The timezone of the location
    val visibility: Int, // The visibility in meters
    val weather: List<Weather>, // A list of weather conditions
    val wind: Wind // The wind data
) // A data class that represents the main response from the OpenWeatherMap API
