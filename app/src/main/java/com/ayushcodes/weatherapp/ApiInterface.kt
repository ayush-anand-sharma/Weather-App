package com.ayushcodes.weatherapp // Defines the package name for the application

import retrofit2.Call // A Retrofit class that represents a single request/response pair
import retrofit2.http.GET // A Retrofit annotation that declares an HTTP GET request
import retrofit2.http.Query // A Retrofit annotation that specifies a query parameter for a request

// API INTERFACE
interface ApiInterface { // An interface that defines the API endpoints
    @GET("weather") // Declares a GET request to the "weather" endpoint
    fun getWeatherData( // A function that gets the weather data for a specified city
        @Query("q") city: String, // The city name to get the weather data for
        @Query("appid") appid: String, // The API key for the OpenWeatherMap API
        @Query("units") units: String // The units to use for the weather data (e.g., metric, imperial)
    ): Call<WeatherApp> // Returns a Call object that represents the request

    @GET("search") // Declares a GET request to the "search" endpoint
    fun getCitySuggestions( // A function that gets city suggestions for a given query
        @Query("name") name: String, // The name of the city to get suggestions for
        @Query("count") count: Int // The number of suggestions to return
    ): Call<GeoCodingResponse> // Returns a Call object that represents the request
}
