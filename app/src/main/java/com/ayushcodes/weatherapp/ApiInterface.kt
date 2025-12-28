package com.ayushcodes.weatherapp // Defines the package name for this file

import retrofit2.Call // Imports Call class from Retrofit for handling API responses
import retrofit2.http.GET // Imports GET annotation for defining HTTP GET requests
import retrofit2.http.Query // Imports Query annotation for adding query parameters to the request

// Interface defining the API endpoints for Retrofit
interface ApiInterface {
    // Defines a GET request to the "weather" endpoint
    @GET("weather")
    fun getWeatherData(
        @Query("q") city: String, // Query parameter for the city name
        @Query("appid") appid: String, // Query parameter for the API key
        @Query("units") units: String // Query parameter for the units of measurement (e.g., metric)
        ): Call<WeatherApp> // Returns a Call object containing the WeatherApp data model
}