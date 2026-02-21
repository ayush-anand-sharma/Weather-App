package com.ayushcodes.weatherapp // Defines the package name for the application

// DATA CLASS FOR GEOCODING RESPONSE

// This data class represents the response from the Geocoding API. It contains a list of city results.
data class GeoCodingResponse(val results: List<CityResult>) // A data class that represents the response from the Geocoding API

// This data class represents a single city result. It contains the name, country, and administrative area of the city.
data class CityResult(val name: String, val country: String, val admin1: String?) // A data class that represents a single city result
