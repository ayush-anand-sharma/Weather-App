@file:Suppress("ALL") // Suppress all warnings in this file for cleaner code
package com.ayushcodes.weatherapp // Defines the package name for the application

// ===== IMPORTS =====
import android.Manifest // Required for location permissions
import android.annotation.SuppressLint // Suppresses lint warnings for specific code sections
import android.app.Activity // Represents an activity, a single screen in an app
import android.content.Context // Provides access to application-specific resources and classes
import android.content.Intent // An intent is a messaging object you can use to request an action from another app component
import android.content.pm.PackageManager // Provides information about application packages installed on the device
import android.location.Geocoder // A class for handling geocoding and reverse geocoding
import android.location.Location // Represents a geographic location
import android.location.LocationManager // Provides access to the system location services
import android.net.ConnectivityManager // Provides information about network connectivity
import android.net.Network // Represents a network
import android.net.NetworkCapabilities // Provides information about the capabilities of a network
import android.os.Build // Provides information about the current device's build
import android.os.Bundle // Used for passing data between activities
import android.provider.Settings // Provides access to system-level settings
import android.view.LayoutInflater // Instantiates a layout XML file into its corresponding View objects
import android.view.View // Represents a basic building block for user interface components
import android.view.ViewGroup // A view that can contain other views
import android.widget.SearchView // A widget that provides a user interface for the user to enter a search query
import android.widget.TextView // A view that displays text
import androidx.activity.enableEdgeToEdge // Enables edge-to-edge display for the app
import androidx.activity.result.ActivityResultLauncher // A launcher for an activity result
import androidx.activity.result.contract.ActivityResultContracts // A collection of standard activity result contracts
import androidx.appcompat.app.AppCompatActivity // Base class for activities that use the support library action bar features
import androidx.constraintlayout.widget.ConstraintLayout // A layout that allows you to create large and complex layouts with a flat view hierarchy
import androidx.core.app.ActivityCompat // Helper for accessing features in Activity
import androidx.recyclerview.widget.LinearLayoutManager // A layout manager that lays out items in a vertical or horizontal scrolling list
import androidx.recyclerview.widget.RecyclerView // A view for displaying long lists of items
import cn.pedant.SweetAlert.SweetAlertDialog // A beautiful and customizable alert dialog library
import com.ayushcodes.weatherapp.databinding.ActivityMainBinding // View binding for the main activity
import com.google.android.gms.location.FusedLocationProviderClient // The main entry point for interacting with the fused location provider
import com.google.android.gms.location.LocationServices // The main entry point for Google Play services location APIs
import com.google.android.gms.location.Priority // Represents the priority of a location request
import com.google.gson.Gson // A Java library that can be used to convert Java Objects into their JSON representation
import com.shashank.sony.fancytoastlib.FancyToast // A library for creating fancy and customizable toasts
import retrofit2.* // A type-safe HTTP client for Android and Java
import retrofit2.converter.gson.GsonConverterFactory // A converter which uses GSON for JSON
import java.text.SimpleDateFormat // A class for formatting and parsing dates in a chosen locale
import java.util.* // Contains the collection framework, legacy collection classes, event model, date and time facilities, internationalization, and miscellaneous utility classes

// ===== ACTIVITY =====
@Suppress("DEPRECATION") // Suppress warnings for deprecated code
class MainActivity : AppCompatActivity() { // Main activity of the application

    // VIEW BINDING
    private val binding: ActivityMainBinding by lazy { // Lazily initializes the view binding
        ActivityMainBinding.inflate(layoutInflater) // Inflates the layout for this activity
    }

    // UI ELEMENTS
    private lateinit var loadingView: ConstraintLayout // A view that shows a loading progress bar

    // NETWORK MANAGEMENT
    private lateinit var connectivityManager: ConnectivityManager // Manages network connections
    private lateinit var networkCallback: ConnectivityManager.NetworkCallback // Listens for network state changes
    private var isConnected = false // Flag to track network connectivity status

    // SHARED PREFERENCES
    private val PREFS_NAME = "WeatherPrefs" // Name of the shared preferences file
    private val LAST_RESPONSE = "LastWeatherResponse" // Key for the last weather response
    private val LAST_CITY = "LastCity" // Key for the last searched city

    // LOCATION SERVICES
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient // Provides access to the fused location provider
    private lateinit var locationSettingsLauncher: ActivityResultLauncher<Intent> // Launcher for the location settings screen

    // FLAGS
    private var permissionAskedOnce = false // Flag to prevent repeated permission dialogs
    private var locationDialogShown = false // Flag to prevent repeated enable-location dialogs

    // SUGGESTIONS ADAPTER
    private lateinit var suggestionsAdapter: SuggestionsAdapter // Adapter for the search suggestions

    // ON CREATE
    override fun onCreate(savedInstanceState: Bundle?) { // Called when the activity is first created
        super.onCreate(savedInstanceState) // Calls the parent class's onCreate method
        enableEdgeToEdge() // Enables edge-to-edge display
        setContentView(binding.root) // Sets the content view to the root of the binding

        loadingView = findViewById(R.id.loading_view) // Initializes the loading view
        loadingView.visibility = View.VISIBLE // Makes the loading view visible

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this) // Initializes the fused location client
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager // Initializes the connectivity manager

        // Initialize the suggestions adapter and RecyclerView
        suggestionsAdapter = SuggestionsAdapter(emptyList()) { cityName -> // Creates a new suggestions adapter
            fetchWeatherData(cityName, true) // Fetches the weather data for the selected city
            binding.suggestionsRecyclerView.visibility = View.GONE // Hides the suggestions recycler view
        }
        binding.suggestionsRecyclerView.layoutManager = LinearLayoutManager(this) // Sets the layout manager for the suggestions recycler view
        binding.suggestionsRecyclerView.adapter = suggestionsAdapter // Sets the adapter for the suggestions recycler view

        // LAUNCHER FOR LOCATION SETTINGS
        locationSettingsLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result -> // Registers a launcher for the location settings activity
                locationDialogShown = false // Resets the location dialog shown flag
                if (result.resultCode == Activity.RESULT_OK) { // Checks if the result is OK
                    startLocationFlow() // Retries the location flow
                } else { // If the result is not OK
                    showLastUpdatedData() // Shows the last updated data
                    loadingView.visibility = View.GONE // Hides the loading view
                }
            }

        registerNetworkCallback() // Registers a network callback to listen for network changes
        SearchCity() // Initializes the search functionality

        initialLaunchLogic() // Executes the initial launch logic
    }

    // INITIAL LAUNCH LOGIC
    private fun initialLaunchLogic() { // Logic to be executed on the initial launch of the app
        if (checkInternet()) { // Checks if the device is connected to the internet
            startLocationFlow() // Starts the location flow
        } else { // If there is no internet connection
            loadingView.visibility = View.GONE // Hides the loading view
            FancyToast.makeText(this,"Please check your internet connection.",FancyToast.LENGTH_LONG,FancyToast.WARNING,R.drawable.white_cloud,false).show() // Shows a warning toast
            showLastUpdatedData() // Shows the last updated weather data
        }
    }

    // ON RESUME
    override fun onResume() { // Called when the activity will start interacting with the user
        super.onResume() // Calls the parent class's onResume method
        // DO NOTHING HERE — prevents multiple dialogs
    }

    // LOCATION FLOW CONTROLLER
    private fun startLocationFlow() { // Controls the flow of obtaining the user's location

        if (!checkInternet()) return // Stops the flow if there is no internet connection

        // CHECK PERMISSION
        if (ActivityCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || // Checks if fine location permission is granted
            ActivityCompat.checkSelfPermission(this,Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) { // Checks if coarse location permission is granted

            checkLocationEnabled() // Checks if location is enabled

        } else { // If permission is not granted

            if (!permissionAskedOnce) { // Checks if permission has been asked before
                permissionAskedOnce = true // Sets the flag to true
                locationPermissionRequest.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION)) // Launches the permission request
            } else { // If permission has been asked before
                showLastUpdatedData() // Shows the last updated data
                loadingView.visibility = View.GONE // Hides the loading view
            }
        }
    }

    // PERMISSION RESULT
    private val locationPermissionRequest = // A launcher for the location permission request
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions -> // Registers a launcher for multiple permissions

            val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true || // Checks if fine location permission is granted
                    permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true // Checks if coarse location permission is granted

            if (granted) { // If permission is granted
                checkLocationEnabled() // Checks if location is enabled
            } else { // If permission is not granted
                showLastUpdatedData() // Shows the last updated data
                loadingView.visibility = View.GONE // Hides the loading view
            }
        }

    // CHECK IF GPS IS ON
    private fun checkLocationEnabled() { // Checks if the location services are enabled on the device

        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager // Initializes the location manager

        val enabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || // Checks if GPS provider is enabled
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) // Checks if network provider is enabled

        if (enabled) { // If location is enabled
            getCurrentLocation() // Gets the current location
        } else { // If location is not enabled
            if (locationDialogShown) return // Prevents repeated dialogs
            locationDialogShown = true // Sets the flag to true

            SweetAlertDialog(this,SweetAlertDialog.WARNING_TYPE) // Shows a warning dialog
                .setTitleText("Enable Location") // Sets the title of the dialog
                .setContentText("Please enable location services.") // Sets the content of the dialog
                .setConfirmText("Enable") // Sets the confirm button text
                .setConfirmClickListener { // Sets the confirm button click listener
                    it.dismissWithAnimation() // Dismisses the dialog with an animation
                    val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS) // Creates an intent to open the location settings
                    locationSettingsLauncher.launch(intent) // Launches the location settings screen
                }
                .setCancelText("Cancel") // Sets the cancel button text
                .setCancelClickListener { // Sets the cancel button click listener
                    it.dismissWithAnimation() // Dismisses the dialog with an animation
                    showLastUpdatedData() // Shows the last updated data
                    loadingView.visibility = View.GONE // Hides the loading view
                }
                .show() // Shows the dialog
        }
    }

    // GET LOCATION
    @SuppressLint("MissingPermission") // Suppresses the missing permission warning
    private fun getCurrentLocation() { // Gets the user's current location

        if (!checkInternet()) { // Checks for internet connection
            showLastUpdatedData() // Shows last updated data if offline
            FancyToast.makeText(this,"Please check your internet connection",FancyToast.LENGTH_LONG,FancyToast.WARNING,R.drawable.white_cloud,false).show() // Shows a warning toast
            loadingView.visibility = View.GONE // Hides the loading view
            return // Exits the function
        }

        fusedLocationProviderClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY,null) // Gets the current location with high accuracy
            .addOnSuccessListener { location: Location? -> // Adds a success listener

                if (location != null) { // If the location is not null

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Checks the Android version

                        val geoCoder = Geocoder(this,Locale.getDefault()) // Initializes the geocoder
                        geoCoder.getFromLocation(location.latitude,location.longitude,1){ addresses -> // Gets the address from the location
                            val cityName = if(addresses.isNotEmpty()) // Checks if the address list is not empty
                                addresses[0].locality ?: addresses[0].adminArea // Gets the city name
                            else "India" // Sets a default city name

                            runOnUiThread { fetchWeatherData(cityName ?: "India") } // Fetches the weather data on the main thread
                        }

                    } else { // If the Android version is lower than Tiramisu

                        Thread { // Creates a new thread
                            val cityName = getCityName(location.latitude,location.longitude) // Gets the city name
                            runOnUiThread { fetchWeatherData(cityName ?: "India") } // Fetches the weather data on the main thread
                        }.start() // Starts the thread
                    }

                } else fetchWeatherData("India") // Fetches weather data for a default city
            }
    }

    // GET CITY NAME
    private fun getCityName(lat:Double,long:Double):String?{ // Gets the city name from latitude and longitude
        val geoCoder=Geocoder(this,Locale.getDefault()) // Initializes the geocoder
        return try{ // Tries to get the address
            val address=geoCoder.getFromLocation(lat,long,1) // Gets the address from the location
            address?.firstOrNull()?.locality ?: address?.firstOrNull()?.adminArea // Returns the city name
        }catch(e:Exception){ null } // Returns null if an exception occurs
    }

    // SEARCH
    private fun SearchCity(){ // Initializes the search functionality
        val searchView=binding.searchView // Gets the search view from the binding
        searchView.setOnQueryTextListener(object:SearchView.OnQueryTextListener{ // Sets a query text listener

            override fun onQueryTextSubmit(query:String?):Boolean{ // Called when the user submits the query

                if(query.isNullOrBlank()){ // Checks if the query is null or blank
                    FancyToast.makeText(this@MainActivity,"Please Enter A City Or Country Name",FancyToast.LENGTH_SHORT,FancyToast.WARNING,R.drawable.white_cloud,false).show()
                }else{ // If the query is not null or blank
                    if(checkInternet()) { // Checks for internet connection
                        loadingView.visibility = View.VISIBLE // Shows the loading view
                        fetchWeatherData(query,true) // Fetches the weather data for the searched city
                    }
                    else{ // If there is no internet connection
                        showLastUpdatedData() // Shows the last updated data
                        FancyToast.makeText(this@MainActivity,"Network error, please check your internet connection",FancyToast.LENGTH_SHORT,FancyToast.ERROR,R.drawable.white_cloud,false).show() // Shows an error toast
                    }
                }
                return true // Returns true to indicate that the query has been handled
            }

            override fun onQueryTextChange(newText:String?):Boolean { // Called when the query text changes
                if (newText.isNullOrBlank()) { // Checks if the new text is null or blank
                    binding.suggestionsRecyclerView.visibility = View.GONE // Hides the suggestions recycler view
                } else { // If the new text is not null or blank
                    fetchCitySuggestions(newText) // Fetches city suggestions
                }
                return true // Returns true to indicate that the query has been handled
            }
        })
    }

    // FETCH CITY SUGGESTIONS
    // This function uses the ApiInterface to fetch city suggestions from the Geocoding API.
    // The response is parsed using the GeoCodingResponse data class, which was created to handle the JSON response from the Geocoding API.
    private fun fetchCitySuggestions(query: String) { // Fetches city suggestions from the Geocoding API
        val retrofit = Retrofit.Builder() // Creates a new Retrofit builder
            .addConverterFactory(GsonConverterFactory.create()) // Adds a GSON converter factory
            .baseUrl("https://geocoding-api.open-meteo.com/v1/") // Sets the base URL of the Geocoding API
            .build().create(ApiInterface::class.java) // Creates an instance of the API interface

        val response = retrofit.getCitySuggestions(query, 10) // Gets city suggestions for the given query

        response.enqueue(object : Callback<GeoCodingResponse> { // Enqueues the request
            override fun onResponse(call: Call<GeoCodingResponse>, response: Response<GeoCodingResponse>) { // Called when a response is received
                if (response.isSuccessful) { // Checks if the response is successful
                    val suggestions = response.body()?.results?.map { "${it.name}, ${it.country}" } ?: emptyList() // Maps the results to a list of strings
                    if (suggestions.isEmpty()) { // Checks if the suggestions list is empty
                        suggestionsAdapter.updateData(listOf("No Result Available..")) // Updates the adapter with a "No Result Available.." message
                    } else { // If the suggestions list is not empty
                        suggestionsAdapter.updateData(suggestions) // Updates the adapter with the suggestions
                    }
                    binding.suggestionsRecyclerView.visibility = View.VISIBLE // Shows the suggestions recycler view
                }
            }

            override fun onFailure(call: Call<GeoCodingResponse>, t: Throwable) { // Called when the request fails
                // Handle failure
            }
        })
    }

    // RETROFIT API
    // This function uses the ApiInterface to fetch weather data from the OpenWeatherMap API.
    // The ApiInterface was created to define the API endpoints for the OpenWeatherMap API.
    private fun fetchWeatherData(cityName:String,isSearchAction:Boolean=false){ // Fetches weather data from the API

        val retrofit=Retrofit.Builder() // Creates a new Retrofit builder
            .addConverterFactory(GsonConverterFactory.create()) // Adds a GSON converter factory
            .baseUrl("https://api.openweathermap.org/data/2.5/") // Sets the base URL of the API
            .build().create(ApiInterface::class.java) // Creates an instance of the API interface

        val response=retrofit.getWeatherData(cityName,"b6e8352ce03216a9fd44c88e118a94c3","metric") // Gets the weather data for the specified city

        response.enqueue(object:Callback<WeatherApp> { // Enqueues the request

            override fun onResponse(call:Call<WeatherApp?>,response:Response<WeatherApp?>){ // Called when a response is received
                loadingView.visibility = View.GONE // Hides the loading view
                val responseBody=response.body() // Gets the response body

                if(response.isSuccessful && responseBody!=null){ // Checks if the response is successful and the body is not null

                    updateUI(cityName,responseBody) // Updates the UI with the new data

                    FancyToast.makeText(this@MainActivity,"Weather updated",FancyToast.LENGTH_SHORT,FancyToast.SUCCESS,R.drawable.white_cloud,false).show() // Shows a success toast

                    val sharedPrefs=getSharedPreferences(PREFS_NAME,Context.MODE_PRIVATE) // Gets the shared preferences
                    sharedPrefs.edit().putString(LAST_RESPONSE,Gson().toJson(responseBody)).putString(LAST_CITY,cityName).apply() // Saves the last response and city

                }else if(isSearchAction){ // If the search action failed
                    FancyToast.makeText(this@MainActivity,"Please enter a correct city or place...",FancyToast.LENGTH_SHORT,FancyToast.ERROR,R.drawable.white_cloud,false).show()
                }
            }

            override fun onFailure(call:Call<WeatherApp?>,t:Throwable){ // Called when the request fails
                loadingView.visibility = View.GONE // Hides the loading view
                FancyToast.makeText(this@MainActivity,"Please check your internet connection",FancyToast.LENGTH_LONG,FancyToast.ERROR,R.drawable.white_cloud,false).show() // Shows an error toast
            }
        })
    }

    // UPDATE UI
    @SuppressLint("SetTextI18n") // Suppresses the SetTextI18n warning
    private fun updateUI(cityName:String,responseBody:WeatherApp){ // Updates the UI with the weather data

        val main=responseBody.main // Gets the main weather data
        val wind=responseBody.wind // Gets the wind data
        val sys=responseBody.sys // Gets the system data
        val weatherList=responseBody.weather // Gets the weather list

        binding.temp.text="${main?.temp ?: 0.0} °C" // Sets the temperature
        binding.windSpeed.text="${wind?.speed ?: 0.0} m/s" // Sets the wind speed
        binding.humidity.text="${main?.humidity ?: 0} %" // Sets the humidity

        val sunrise=sys?.sunrise?.toLong() ?: 0L // Gets the sunrise time
        val sunset=sys?.sunset?.toLong() ?: 0L // Gets the sunset time
        binding.sunrise.text="${time(sunrise)} am" // Sets the sunrise time
        binding.sunset.text="${time(sunset)} pm" // Sets the sunset time

        binding.sea.text="${main?.sea_level ?: 0} hPa" // Sets the sea level

        val condition=weatherList?.firstOrNull()?.main ?: "unknown" // Gets the weather condition
        binding.conditions.text=condition // Sets the weather condition
        binding.maxTemp.text="Max: ${main?.temp_max ?: 0.0} °C" // Sets the max temperature
        binding.minTemp.text="Min: ${main?.temp_min ?: 0.0} °C" // Sets the min temperature
        binding.weather.text=condition // Sets the weather condition

        binding.day.text=dayName(System.currentTimeMillis()) // Sets the day of the week
        binding.date.text=date() // Sets the date
        binding.cityName.text=cityName.uppercase(Locale.getDefault()) // Sets the city name in uppercase

        changeImagesAccordingtoWeatherCondition(condition) // Changes the images based on the weather condition
    }

    // SHOW LAST UPDATED DATA
    private fun showLastUpdatedData(){ // Shows the last updated weather data
        val sharedPrefs=getSharedPreferences(PREFS_NAME,Context.MODE_PRIVATE) // Gets the shared preferences
        val json=sharedPrefs.getString(LAST_RESPONSE,null) // Gets the last response from shared preferences
        val lastCity=sharedPrefs.getString(LAST_CITY,"India") // Gets the last city from shared preferences

        if(json!=null){ // Checks if the last response is not null
            val lastResponse=Gson().fromJson(json,WeatherApp::class.java) // Converts the JSON string to a WeatherApp object
            updateUI(lastResponse.name ?: lastCity ?: "India",lastResponse) // Updates the UI with the last response
        }else fetchWeatherData("India") // Fetches weather data for a default city if no last data is available
    }

    // NETWORK OBSERVER
    private fun registerNetworkCallback(){ // Registers a network callback to listen for network changes

        networkCallback=object:ConnectivityManager.NetworkCallback(){ // Creates a new network callback

            override fun onAvailable(network:Network){ // Called when a network is available
                if(!isConnected){ // Checks if the device is not connected
                    isConnected=true // Sets the connected flag to true
                    runOnUiThread{ // Runs the code on the main thread
                        // FancyToast.makeText(this@MainActivity,"Feed Updated.",FancyToast.LENGTH_SHORT,FancyToast.SUCCESS,R.drawable.white_cloud,false).show()
                        startLocationFlow() // Retries the location flow
                    }
                }
            }

            override fun onLost(network:Network){ // Called when a network is lost
                isConnected=false // Sets the connected flag to false
                runOnUiThread{ // Runs the code on the main thread
                    showLastUpdatedData() // Shows the last updated data
                    FancyToast.makeText(this@MainActivity,"Please check your internet connection",FancyToast.LENGTH_LONG,FancyToast.WARNING,R.drawable.white_cloud,false).show() // Shows a warning toast
                }
            }
        }

        connectivityManager.registerDefaultNetworkCallback(networkCallback) // Registers the network callback
    }

    // CHECK INTERNET
    private fun checkInternet():Boolean{ // Checks if the device is connected to the internet
        val capabilities=connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork) // Gets the network capabilities
        return capabilities!=null && // Checks if the capabilities are not null
                (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || // Checks if Wi-Fi is available
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) // Checks if cellular is available
    }

    // CHANGE IMAGES ACCORDING TO WEATHER CONDITION
    private fun changeImagesAccordingtoWeatherCondition(conditions:String){ // Changes the background and animation based on the weather condition
        when(conditions){ // Switches on the weather condition
            "Clear Sky","Sunny","Clear"->{ binding.root.setBackgroundResource(R.drawable.sunny_background); binding.lottieAnimationView.setAnimation(R.raw.sun) } // Sets the background and animation for sunny weather
            "Partly Clouds","Clouds","Overcast","Mist","Foggy","Haze"->{ binding.root.setBackgroundResource(R.drawable.colud_background); binding.lottieAnimationView.setAnimation(R.raw.cloud) } // Sets the background and animation for cloudy weather
            "Light Rain","Drizzle","Moderate Rain","Showers","Heavy Rain","Rain"->{ binding.root.setBackgroundResource(R.drawable.rain_background); binding.lottieAnimationView.setAnimation(R.raw.rain) } // Sets the background and animation for rainy weather
            "Light Snow","Moderate Snow","Heavy Snow","Blizzard","Snow"->{ binding.root.setBackgroundResource(R.drawable.snow_background); binding.lottieAnimationView.setAnimation(R.raw.snow) } // Sets the background and animation for snowy weather
            else->{ binding.root.setBackgroundResource(R.drawable.sunny_background); binding.lottieAnimationView.setAnimation(R.raw.sun) } // Sets the default background and animation
        }
        binding.lottieAnimationView.playAnimation() // Plays the Lottie animation
    }

    // DAY NAME
    fun dayName(timeStamp:Long):String{ // Gets the day name from a timestamp
        val sdf=SimpleDateFormat("EEEE",Locale.getDefault()) // Creates a new SimpleDateFormat object
        return sdf.format(Date()) // Returns the formatted day name
    }

    // DATE
    private fun date():String{ // Gets the current date
        val sdf=SimpleDateFormat("dd MMM yyyy",Locale.getDefault()) // Creates a new SimpleDateFormat object
        return sdf.format(Date()) // Returns the formatted date
    }

    // TIME
    private fun time(timeStamp:Long):String{ // Gets the time from a timestamp
        val sdf=SimpleDateFormat("HH:mm",Locale.getDefault()) // Creates a new SimpleDateFormat object
        return sdf.format(Date(timeStamp*1000)) // Returns the formatted time
    }

    // ON DESTROY
    override fun onDestroy(){ // Called when the activity is destroyed
        super.onDestroy() // Calls the parent class's onDestroy method
        connectivityManager.unregisterNetworkCallback(networkCallback) // Unregisters the network callback
    }

    // SUGGESTIONS ADAPTER
    inner class SuggestionsAdapter(
        private var suggestions: List<String>, // A list of suggestions to be displayed
        private val onItemClick: (String) -> Unit // A lambda function to be called when an item is clicked
    ) : RecyclerView.Adapter<SuggestionsAdapter.SuggestionViewHolder>() { // An adapter for the suggestions recycler view

        // SUGGESTION VIEW HOLDER
        inner class SuggestionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) { // A view holder for a single suggestion item
            val textView: TextView = itemView.findViewById(R.id.suggestion_text_view) // The text view that displays the suggestion
        }

        // ON CREATE VIEW HOLDER
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SuggestionViewHolder { // Called when a new view holder is created
            val view = LayoutInflater.from(parent.context).inflate(R.layout.suggestion_item, parent, false) // Inflates the layout for a single suggestion item
            return SuggestionViewHolder(view) // Returns a new view holder
        }

        // ON BIND VIEW HOLDER
        override fun onBindViewHolder(holder: SuggestionViewHolder, position: Int) { // Called when a view holder is bound to a new position
            val suggestion = suggestions[position] // Gets the suggestion at the current position
            holder.textView.text = suggestion // Sets the text of the text view to the suggestion
            holder.itemView.setOnClickListener { // Sets a click listener on the item view
                if (suggestion != "No Result Available..") { // Checks if the suggestion is not "No Result Available.."
                    onItemClick(suggestion) // Calls the on item click lambda function
                }
            }
        }

        // GET ITEM COUNT
        override fun getItemCount(): Int = suggestions.size // Returns the number of suggestions

        // UPDATE DATA
        fun updateData(newSuggestions: List<String>) { // Updates the suggestions list
            suggestions = newSuggestions // Sets the new suggestions
            notifyDataSetChanged() // Notifies the adapter that the data has changed
        }
    }
}
