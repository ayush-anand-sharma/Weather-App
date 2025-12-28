package com.ayushcodes.weatherapp // Defines the package name for this file

// Data class representing the complete weather API response
data class WeatherApp(
    val base: String?, // Internal parameter
    val clouds: Clouds?, // Object containing cloud information
    val cod: Int?, // Internal parameter
    val coord: Coord?, // Object containing geographic coordinates
    val dt: Int?, // Time of data calculation, unix, UTC
    val id: Int?, // City ID
    val main: Main?, // Object containing main weather data
    val name: String?, // City name
    val sys: Sys?, // Object containing system data (country, sunrise, sunset)
    val timezone: Int?, // Shift in seconds from UTC
    val visibility: Int?, // Visibility, meter
    val weather: List<Weather>?, // List of weather condition objects
    val wind: Wind? // Object containing wind information
)