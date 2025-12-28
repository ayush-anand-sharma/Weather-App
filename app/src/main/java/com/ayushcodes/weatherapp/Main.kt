package com.ayushcodes.weatherapp // Defines the package name for this file

// Data class representing main weather data
data class Main(
    val feels_like: Double?, // Temperature accounting for human perception of weather
    val grnd_level: Int?, // Atmospheric pressure on the ground level
    val humidity: Int?, // Humidity percentage
    val pressure: Int?, // Atmospheric pressure (on the sea level, if there is no sea_level or grnd_level data)
    val sea_level: Int?, // Atmospheric pressure on the sea level
    val temp: Double?, // Temperature in the unit specified by the API call
    val temp_max: Double?, // Maximum temperature at the moment
    val temp_min: Double? // Minimum temperature at the moment
)