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

import android.annotation.SuppressLint // Imports SuppressLint annotation
import android.content.Context // Imports Context class for accessing application environment
import android.net.ConnectivityManager // Imports ConnectivityManager for checking network connection
import android.net.Network // Imports Network class
import android.net.NetworkCapabilities // Imports NetworkCapabilities for checking network capabilities
import android.os.Bundle // Imports Bundle class for passing data between activities
import android.widget.SearchView // Imports SearchView for search functionality
import androidx.activity.OnBackPressedCallback // Imports OnBackPressedCallback for handling back button
import androidx.activity.enableEdgeToEdge // Imports enableEdgeToEdge for full-screen display
import androidx.appcompat.app.AppCompatActivity // Imports AppCompatActivity as the base class for activities
import cn.pedant.SweetAlert.SweetAlertDialog // Imports SweetAlertDialog for custom alert dialogs
import com.ayushcodes.weatherapp.databinding.ActivityMainBinding // Imports ViewBinding class for main activity layout
import com.google.gson.Gson // Imports Gson for JSON parsing
import com.shashank.sony.fancytoastlib.FancyToast // Imports FancyToast for custom toast messages
import retrofit2.Call // Imports Call class from Retrofit
import retrofit2.Callback // Imports Callback interface from Retrofit
import retrofit2.Response // Imports Response class from Retrofit
import retrofit2.Retrofit // Imports Retrofit class
import retrofit2.converter.gson.GsonConverterFactory // Imports GsonConverterFactory for Retrofit
import java.text.SimpleDateFormat // Imports SimpleDateFormat for formatting dates
import java.util.Date // Imports Date class
import java.util.Locale // Imports Locale class

@Suppress("USELESS_ELVIS") // Suppresses lint warning for useless elvis operator
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

    // Called when the activity is first created
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState) // Call superclass method
        enableEdgeToEdge() // Enable edge-to-edge display
        setContentView(binding.root) // Set the content view to the root of the binding

        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager // Initialize ConnectivityManager

        registerNetworkCallback() // Register network callback to monitor connection changes

        // Check if internet is available
        if (checkInternet()) {
            fetchWeatherData("Jaipur") // Fetch weather data for default city Jaipur
        } else {
            showLastUpdatedData() // Show last saved data if no internet
            FancyToast.makeText(
                this,
                "Network error, please check your internet connection",
                FancyToast.LENGTH_LONG,
                FancyToast.ERROR,
                R.drawable.white_cloud,
                false
            ).show() // Show error toast
        }
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

                    // Save last response to SharedPreferences
                    val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    sharedPrefs.edit().putString(LAST_RESPONSE, Gson().toJson(responseBody)).apply()
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

        if (json != null) {
            val lastResponse = Gson().fromJson(json, WeatherApp::class.java) // Parse JSON to object
            updateUI(lastResponse.name ?: "Last City", lastResponse) // Update UI
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
                            "Updating weather...",
                            FancyToast.LENGTH_SHORT,
                            FancyToast.CONFUSING,
                            R.drawable.white_cloud,
                            false
                        ).show()
                        fetchWeatherData(binding.cityName.text.toString()) // Refresh weather
                        FancyToast.makeText(
                            this@MainActivity,
                            "Weather updated",
                            FancyToast.LENGTH_SHORT,
                            FancyToast.SUCCESS,
                            R.drawable.white_cloud,
                            false
                        ).show()
                    }
                }
            }
            override fun onLost(network: Network) {
                isConnected = false // Set disconnected flag
                runOnUiThread {
                    showLastUpdatedData() // Show cached data
                    FancyToast.makeText(
                        this@MainActivity,
                        "Please connect to your internet",
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