@file:Suppress("ALL")
// MY API KEY:- b6e8352ce03216a9fd44c88e118a94c3

// BELOW IS JAIPUR'S WEATHER CODE...
/*{"coord":{"lon":75.8167,"lat":26.9167},
"weather":[{"id":801,"main":"Clouds","description":"few clouds","icon":"02d"}],
"base":"stations","main":{"temp":299.09,"feels_like":299.59,"temp_min":299.09,"temp_max":299.09,"pressure":1009,"humidity":71,"sea_level":1009,"grnd_level":962},
"visibility":10000,
"wind":{"speed":6.1,"deg":290,"gust":8.69},
"clouds":{"all":11},
"dt":1757559561,
"sys":{"country":"IN","sunrise":1757551233,"sunset":1757595998},
"timezone":19800,"id":1269515,"name":"Jaipur","cod":200}*/

package com.ayushcodes.weatherapp // Defines the package name for this file

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.widget.SearchView
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import cn.pedant.SweetAlert.SweetAlertDialog
import com.ayushcodes.weatherapp.databinding.ActivityMainBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.gson.Gson
import com.shashank.sony.fancytoastlib.FancyToast
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Main Activity class where the app logic resides
class MainActivity : AppCompatActivity() {

    // Lazy initialization of ViewBinding for accessing layout views
    private val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private lateinit var connectivityManager: ConnectivityManager // Manager for handling network connectivity
    private lateinit var networkCallback: ConnectivityManager.NetworkCallback // Callback for network changes
    private var isConnected = false // Flag to track network connection status

    private val PREFS_NAME = "WeatherPrefs" // Name for SharedPreferences file
    private val LAST_RESPONSE = "LastWeatherResponse" // Key for storing last weather response
    private val LAST_CITY = "LastCity" // Key for storing last city name

    private lateinit var fusedLocationClient: FusedLocationProviderClient // Client for accessing location API
    private lateinit var locationCallback: LocationCallback // Callback for receiving location updates

    // Called when the activity is first created
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState) // Call superclass method
        enableEdgeToEdge() // Enable edge-to-edge display
        setContentView(binding.root) // Set the content view to the root of the binding

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this) // Initialize FusedLocationProviderClient
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager // Initialize ConnectivityManager

        registerNetworkCallback() // Register network callback to monitor connection changes

        // Check for location permission and internet on startup
        checkLocationPermission()

        SearchCity() // Initialize search functionality

        // Handle back button press
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Show confirmation dialog before exiting
                SweetAlertDialog(this@MainActivity, SweetAlertDialog.WARNING_TYPE)
                    .setTitleText("Exit App")
                    .setContentText("You Really Want To Exit The App?")
                    .setConfirmText("Yes")
                    .setConfirmClickListener { sDialog ->
                        sDialog.dismissWithAnimation()
                        finish() // Exit the app
                    }
                    .setCancelText("Cancel")
                    .setCancelClickListener { sDialog ->
                        sDialog.dismissWithAnimation() // Dismiss dialog
                    }
                    .show()
            }
        })
    }

    // Function to check and request location permissions
    private fun checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Permission not granted, show alert dialog explaining why it's needed
            SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE)
                .setTitleText("Location Permission Needed")
                .setContentText("Allow location access to show weather for your current location.")
                .setConfirmText("Allow")
                .setConfirmClickListener { sDialog ->
                    sDialog.dismissWithAnimation()
                    // Request permissions
                    locationPermissionRequest.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
                .setCancelText("Deny")
                .setCancelClickListener { sDialog ->
                    sDialog.dismissWithAnimation()
                    // Permission denied, handle accordingly (e.g., show default city)
                    handlePermissionDenied()
                }
                .show()
        } else {
            // Permission already granted, proceed to get location
            getCurrentLocation()
        }
    }

    // Permission request launcher
    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                // Precise location access granted.
                getCurrentLocation()
            }
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                // Only approximate location access granted.
                getCurrentLocation()
            }
            else -> {
                // No location access granted.
                handlePermissionDenied()
            }
        }
    }

    // Handle case where location permission is denied
    private fun handlePermissionDenied() {
        if (checkInternet()) {
             fetchWeatherData("India") // Fetch weather for India as fallback
        } else {
             showLastUpdatedData() // Show last updated data if offline
             // Display toast for internet connection
             FancyToast.makeText(
                this,
                "Please check your internet connection",
                FancyToast.LENGTH_LONG,
                FancyToast.WARNING,
                R.drawable.white_cloud,
                false
            ).show()
        }
    }

    // Function to get current location
    @SuppressLint("MissingPermission")
    private fun getCurrentLocation() {
        if (checkInternet()) {
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000).build()

            locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                     fusedLocationClient.removeLocationUpdates(this) // Stop updates after getting one
                     for (location in locationResult.locations) {
                         // Geocoding can block the main thread, so run it on a background thread
                         // but only for older APIs. For API 33+, we can use the async method.
                         
                         if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                             val geoCoder = Geocoder(this@MainActivity, Locale.getDefault())
                             geoCoder.getFromLocation(location.latitude, location.longitude, 1) { addresses ->
                                 if (addresses.isNotEmpty()) {
                                     val cityName = addresses[0].locality ?: addresses[0].adminArea
                                     runOnUiThread {
                                         fetchWeatherData(cityName ?: "India")
                                     }
                                 } else {
                                     runOnUiThread {
                                         fetchWeatherData("India")
                                     }
                                 }
                             }
                         } else {
                             // For older devices, do it in a thread
                             Thread {
                                 // Get city name from coordinates
                                 val cityName = getCityName(location.latitude, location.longitude)
                                 
                                 // Switch back to Main Thread to update UI/Fetch Data
                                 runOnUiThread {
                                     if (cityName != null) {
                                         fetchWeatherData(cityName) // Fetch weather for current location
                                     } else {
                                         fetchWeatherData("India") // Fallback if city not found
                                     }
                                 }
                             }.start()
                         }
                         return 
                     }
                }
            }
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        } else {
            showLastUpdatedData() // Show last updated data if offline
             // Display toast for internet connection
             FancyToast.makeText(
                this,
                "Please check your internet connection",
                FancyToast.LENGTH_LONG,
                FancyToast.WARNING,
                R.drawable.white_cloud,
                false
            ).show()
        }
    }

    // Function to get city name from coordinates using Geocoder (Deprecated for API 33+, used for fallback)
    private fun getCityName(lat: Double, long: Double): String? {
        val geoCoder = Geocoder(this, Locale.getDefault())
        try {
            @Suppress("DEPRECATION")
            val address = geoCoder.getFromLocation(lat, long, 1)
            if (!address.isNullOrEmpty()) {
                return address[0].locality ?: address[0].adminArea // Return locality or admin area
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    // Function to handle city search
    private fun SearchCity() {
        val searchView = binding.searchView // Get reference to SearchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (query.isNullOrBlank()) {
                    // Empty input warning
                    FancyToast.makeText(
                        this@MainActivity,
                        "Please Enter A City Or Country Name",
                        FancyToast.LENGTH_SHORT,
                        FancyToast.WARNING,
                        R.drawable.white_cloud,
                        false
                    ).show()
                } else {
                    if (checkInternet()) {
                        fetchWeatherData(query, true) // fetch weather for entered city
                    } else {
                        showLastUpdatedData() // Show last data if no internet
                        FancyToast.makeText(
                            this@MainActivity,
                            "Network error, please check your internet connection",
                            FancyToast.LENGTH_SHORT,
                            FancyToast.ERROR,
                            R.drawable.white_cloud,
                            false
                        ).show()
                    }
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean = true // Return true on text change
        })
    }
    
    // Function to fetch weather data from API
    // Added extra param: isSearchAction to differentiate between default load and user search
    private fun fetchWeatherData(cityName: String, isSearchAction: Boolean = false) {
        // Initialize Retrofit
        val retrofit = Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl("https://api.openweathermap.org/data/2.5/")
            .build().create(ApiInterface::class.java)

        // Make API call
        val response = retrofit.getWeatherData(cityName, "b6e8352ce03216a9fd44c88e118a94c3", "metric")
        response.enqueue(object : Callback<WeatherApp> {
            @SuppressLint("SetTextI18n", "UseKtx")
            override fun onResponse(call: Call<WeatherApp?>, response: Response<WeatherApp?>) {
                val responseBody = response.body() // Get response body
                if (response.isSuccessful && responseBody != null) {
                    updateUI(cityName, responseBody) // Update UI with data

                    // Save last response and city to SharedPreferences
                    val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    val editor = sharedPrefs.edit()
                    editor.putString(LAST_RESPONSE, Gson().toJson(responseBody))
                    editor.putString(LAST_CITY, cityName)
                    editor.apply()

                } else {
                    // Only show "Doesn't exist" if this was triggered by search
                    if (isSearchAction) {
                        FancyToast.makeText(
                            this@MainActivity,
                            "This City or Country Doesn't Exist",
                            FancyToast.LENGTH_SHORT,
                            FancyToast.ERROR,
                            R.drawable.white_cloud,
                            false
                        ).show()
                    }
                    // suppress other errors on app start
                }
            }

            override fun onFailure(call: Call<WeatherApp?>, t: Throwable) {
                // Only show network errors, suppress others
                if (t.localizedMessage?.contains("Unable to resolve host") == true ||
                    t.localizedMessage?.contains("timeout") == true) {
                    FancyToast.makeText(
                        this@MainActivity,
                        "Network error, please check your internet connection",
                        FancyToast.LENGTH_LONG,
                        FancyToast.ERROR,
                        R.drawable.white_cloud,
                        false
                    ).show()
                }
            }
        })
    }

    // Function to update the UI with weather data
    @SuppressLint("SetTextI18n")
    private fun updateUI(cityName: String, responseBody: WeatherApp) {
        val main = responseBody.main // Extract main weather data, nullable
        val wind = responseBody.wind // Extract wind data, nullable
        val sys = responseBody.sys // Extract system data, nullable
        val weatherList = responseBody.weather // Extract weather list, nullable

        binding.temp.text = "${main?.temp ?: 0.0} °C" // Set temperature with null check, default 0.0
        binding.windSpeed.text = "${wind?.speed ?: 0.0} m/s" // Set wind speed with null check, default 0.0
        binding.humidity.text = "${main?.humidity ?: 0} %" // Set humidity with null check, default 0
        
        val sunrise = sys?.sunrise?.toLong() ?: 0L // Get sunrise with null check, default 0L
        val sunset = sys?.sunset?.toLong() ?: 0L // Get sunset with null check, default 0L
        binding.sunrise.text = "${time(sunrise)} am" // Set sunrise time
        binding.sunset.text = "${time(sunset)} pm" // Set sunset time
        
        binding.sea.text = "${main?.sea_level ?: 0} hPa" // Set sea level with null check, default 0

        val condition = weatherList?.firstOrNull()?.main ?: "unknown" // Get weather condition safely, default unknown
        binding.conditions.text = condition // Set condition text
        binding.maxTemp.text = "Max: ${main?.temp_max ?: 0.0} °C" // Set max temp with null check, default 0.0
        binding.minTemp.text = "Min: ${main?.temp_min ?: 0.0} °C" // Set min temp with null check, default 0.0
        binding.weather.text = condition // Set weather text

        binding.day.text = dayName(System.currentTimeMillis()) // Set day name
        binding.date.text = date() // Set date
        binding.cityName.text = cityName // Set city name

        changeImagesAccordingtoWeatherCondition(condition) // Update images based on condition
    }

    // Function to show last updated data from SharedPreferences
    private fun showLastUpdatedData() {
        val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = sharedPrefs.getString(LAST_RESPONSE, null) // Retrieve stored JSON
        val lastCity = sharedPrefs.getString(LAST_CITY, "India") // Retrieve stored city, default India

        if (json != null) {
            val lastResponse = Gson().fromJson(json, WeatherApp::class.java) // Parse JSON to object
            updateUI(lastResponse.name ?: lastCity ?: "Last City", lastResponse) // Update UI
        }
    }
    
    // Function to register network callback for connectivity changes
    private fun registerNetworkCallback() {
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                if (!isConnected) {
                    isConnected = true // Set connected flag
                    runOnUiThread {
                        FancyToast.makeText(
                            this@MainActivity,
                            "Feed Updated.", // Display "Feed Updated" toast as requested
                            FancyToast.LENGTH_SHORT,
                            FancyToast.SUCCESS,
                            R.drawable.white_cloud,
                            false
                        ).show()
                        
                        // If location permission is granted, try to refresh current location weather
                        // Otherwise, refresh last known city
                         if (ActivityCompat.checkSelfPermission(
                                this@MainActivity,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                                this@MainActivity,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            getCurrentLocation()
                        } else {
                            val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                            val lastCity = sharedPrefs.getString(LAST_CITY, "India")
                            fetchWeatherData(lastCity ?: "India") // Refresh weather for last known city or India
                        }
                    }
                }
            }
            override fun onLost(network: Network) {
                isConnected = false // Set disconnected flag
                runOnUiThread {
                    showLastUpdatedData() // Show cached data
                    FancyToast.makeText(
                        this@MainActivity,
                        "Please check your internet connection",
                        FancyToast.LENGTH_LONG,
                        FancyToast.WARNING,
                        R.drawable.white_cloud,
                        false
                    ).show()
                }
            }
        }
        connectivityManager.registerDefaultNetworkCallback(networkCallback) // Register the callback
    }

    // Function to check if internet is available
    private fun checkInternet(): Boolean {
        val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        return capabilities != null &&
                (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
    }

    // Function to change background and animations based on weather condition
    private fun changeImagesAccordingtoWeatherCondition(conditions: String) {
        when (conditions) {
            "Clear Sky", "Sunny", "Clear" -> {
                binding.root.setBackgroundResource(R.drawable.sunny_background) // Set sunny background
                binding.lottieAnimationView.setAnimation(R.raw.sun) // Set sun animation
                FancyToast.makeText(this, "Weather Is Sunny", FancyToast.LENGTH_SHORT, FancyToast.CONFUSING, R.drawable.sunny, false)
            }
            "Partly Clouds", "Clouds", "Overcast", "Mist", "Foggy", "Haze" -> {
                binding.root.setBackgroundResource(R.drawable.colud_background) // Set cloudy background
                binding.lottieAnimationView.setAnimation(R.raw.cloud) // Set cloud animation
                FancyToast.makeText(this, "Weather Is Cloudy", FancyToast.LENGTH_SHORT, FancyToast.CONFUSING, R.drawable.cloud_black, false)
            }
            "Light Rain", "Drizzle", "Moderate Rain", "Showers", "Heavy Rain", "Rain" -> {
                binding.root.setBackgroundResource(R.drawable.rain_background) // Set rainy background
                binding.lottieAnimationView.setAnimation(R.raw.rain) // Set rain animation
                FancyToast.makeText(this, "Weather Is Rainy", FancyToast.LENGTH_SHORT, FancyToast.CONFUSING, R.drawable.rain, false)
            }
            "Light Snow", "Moderate Snow", "Heavy Snow", "Blizzard", "Snow" -> {
                binding.root.setBackgroundResource(R.drawable.snow_background) // Set snowy background
                binding.lottieAnimationView.setAnimation(R.raw.snow) // Set snow animation
                FancyToast.makeText(this, "Weather Is Snowy", FancyToast.LENGTH_SHORT, FancyToast.CONFUSING, R.drawable.snow, false)
            }
            else -> {
                binding.root.setBackgroundResource(R.drawable.sunny_background) // Default to sunny background
                binding.lottieAnimationView.setAnimation(R.raw.sun) // Default to sun animation
                FancyToast.makeText(this, "Weather Is Sunny", FancyToast.LENGTH_SHORT, FancyToast.CONFUSING, R.drawable.sunny, false)
            }
        }
        binding.lottieAnimationView.playAnimation() // Play the animation
    }

    // Function to get the day name from timestamp
    fun dayName(timeStamp: Long): String {
        val sdf = SimpleDateFormat("EEEE", Locale.getDefault())
        return sdf.format(Date())
    }

    // Function to get current date formatted
    private fun date(): String {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        return sdf.format(Date())
    }

    // Function to get time from timestamp
    private fun time(timeStamp: Long): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date(timeStamp * 1000))
    }

    // Called when the activity is destroyed
    override fun onDestroy() {
        super.onDestroy()
        connectivityManager.unregisterNetworkCallback(networkCallback) // Unregister network callback
    }
}