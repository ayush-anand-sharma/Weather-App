@file:Suppress("ALL") // SUPPRESS ALL WARNINGS IN THIS FILE FOR CLEANER CODE
package com.ayushcodes.weatherapp // DEFINES THE PACKAGE NAME FOR THE APPLICATION

// ===== IMPORTS =====
import android.Manifest // REQUIRED FOR LOCATION PERMISSIONS
import android.annotation.SuppressLint // SUPPRESSES LINT WARNINGS FOR SPECIFIC CODE SECTIONS
import android.app.Activity // REPRESENTS AN ACTIVITY, A SINGLE SCREEN IN AN APP
import android.content.Context // PROVIDES ACCESS TO APPLICATION-SPECIFIC RESOURCES AND CLASSES
import android.content.Intent // AN INTENT IS A MESSAGING OBJECT YOU CAN USE TO REQUEST AN ACTION FROM ANOTHER APP COMPONENT
import android.content.pm.PackageManager // PROVIDES INFORMATION ABOUT APPLICATION PACKAGES INSTALLED ON THE DEVICE
import android.location.Geocoder // A CLASS FOR HANDLING GEOCODING AND REVERSE GEOCODING
import android.location.Location // REPRESENTS A GEOGRAPHIC LOCATION
import android.location.LocationManager // PROVIDES ACCESS TO THE SYSTEM LOCATION SERVICES
import android.net.ConnectivityManager // PROVIDES INFORMATION ABOUT NETWORK CONNECTIVITY
import android.net.Network // REPRESENTS A NETWORK
import android.net.NetworkCapabilities // PROVIDES INFORMATION ABOUT THE CAPABILITIES OF A NETWORK
import android.os.Build // PROVIDES INFORMATION ABOUT THE CURRENT DEVICE'S BUILD
import android.os.Bundle // USED FOR PASSING DATA BETWEEN ACTIVITIES
import android.provider.Settings // PROVIDES ACCESS TO SYSTEM-LEVEL SETTINGS
import android.view.LayoutInflater // INSTANTIATES A LAYOUT XML FILE INTO ITS CORRESPONDING VIEW OBJECTS
import android.view.View // REPRESENTS A BASIC BUILDING BLOCK FOR USER INTERFACE COMPONENTS
import android.view.ViewGroup // A VIEW THAT CAN CONTAIN OTHER VIEWS
import android.widget.SearchView // A WIDGET THAT PROVIDES A USER INTERFACE FOR THE USER TO ENTER A SEARCH QUERY
import android.widget.TextView // A VIEW THAT DISPLAYS TEXT
import androidx.activity.enableEdgeToEdge // ENABLES EDGE-TO-EDGE DISPLAY FOR THE APP
import androidx.activity.result.ActivityResultLauncher // A LAUNCHER FOR AN ACTIVITY RESULT
import androidx.activity.result.contract.ActivityResultContracts // A COLLECTION OF STANDARD ACTIVITY RESULT CONTRACTS
import androidx.appcompat.app.AppCompatActivity // BASE CLASS FOR ACTIVITIES THAT USE THE SUPPORT LIBRARY ACTION BAR FEATURES
import androidx.constraintlayout.widget.ConstraintLayout // A LAYOUT THAT ALLOWS YOU TO CREATE LARGE AND COMPLEX LAYOUTS WITH A FLAT VIEW HIERARCHY
import androidx.core.app.ActivityCompat // HELPER FOR ACCESSING FEATURES IN ACTIVITY
import androidx.recyclerview.widget.LinearLayoutManager // A LAYOUT MANAGER THAT LAYS OUT ITEMS IN A VERTICAL OR HORIZONTAL SCROLLING LIST
import androidx.recyclerview.widget.RecyclerView // A VIEW FOR DISPLAYING LONG LISTS OF ITEMS
import cn.pedant.SweetAlert.SweetAlertDialog // A BEAUTIFUL AND CUSTOMIZABLE ALERT DIALOG LIBRARY
import com.ayushcodes.weatherapp.databinding.ActivityMainBinding // VIEW BINDING FOR THE MAIN ACTIVITY
import com.google.android.gms.location.FusedLocationProviderClient // THE MAIN ENTRY POINT FOR INTERACTING WITH THE FUSED LOCATION PROVIDER
import com.google.android.gms.location.LocationServices // THE MAIN ENTRY POINT FOR GOOGLE PLAY SERVICES LOCATION APIS
import com.google.android.gms.location.Priority // REPRESENTS THE PRIORITY OF A LOCATION REQUEST
import com.google.gson.Gson // A JAVA LIBRARY THAT CAN BE USED TO CONVERT JAVA OBJECTS INTO THEIR JSON REPRESENTATION
import com.shashank.sony.fancytoastlib.FancyToast // A LIBRARY FOR CREATING FANCY AND CUSTOMIZABLE TOASTS
import retrofit2.* // A TYPE-SAFE HTTP CLIENT FOR ANDROID AND JAVA
import retrofit2.converter.gson.GsonConverterFactory // A CONVERTER WHICH USES GSON FOR JSON
import java.text.SimpleDateFormat // A CLASS FOR FORMATTING AND PARSING DATES IN A CHOSEN LOCALE
import java.util.* // CONTAINS THE COLLECTION FRAMEWORK, LEGACY COLLECTION CLASSES, EVENT MODEL, DATE AND TIME FACILITIES, INTERNATIONALIZATION, AND MISCELLANEOUS UTILITY CLASSES

// ===== ACTIVITY =====
@Suppress("DEPRECATION") // SUPPRESS WARNINGS FOR DEPRECATED CODE
class MainActivity : AppCompatActivity() { // MAIN ACTIVITY OF THE APPLICATION

    // VIEW BINDING
    private val binding: ActivityMainBinding by lazy { // LAZILY INITIALIZES THE VIEW BINDING
        ActivityMainBinding.inflate(layoutInflater) // INFLATES THE LAYOUT FOR THIS ACTIVITY
    }

    // UI ELEMENTS
    private lateinit var loadingView: ConstraintLayout // A VIEW THAT SHOWS A LOADING PROGRESS BAR

    // NETWORK MANAGEMENT
    private lateinit var connectivityManager: ConnectivityManager // MANAGES NETWORK CONNECTIONS
    private lateinit var networkCallback: ConnectivityManager.NetworkCallback // LISTENS FOR NETWORK STATE CHANGES
    private var isConnected = false // FLAG TO TRACK NETWORK CONNECTIVITY STATUS

    // SHARED PREFERENCES
    private val PREFS_NAME = "WeatherPrefs" // NAME OF THE SHARED PREFERENCES FILE
    private val LAST_RESPONSE = "LastWeatherResponse" // KEY FOR THE LAST WEATHER RESPONSE
    private val LAST_CITY = "LastCity" // KEY FOR THE LAST SEARCHED CITY

    // LOCATION SERVICES
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient // PROVIDES ACCESS TO THE FUSED LOCATION PROVIDER
    private lateinit var locationSettingsLauncher: ActivityResultLauncher<Intent> // LAUNCHER FOR THE LOCATION SETTINGS SCREEN

    // FLAGS
    private var permissionAskedOnce = false // FLAG TO PREVENT REPEATED PERMISSION DIALOGS
    private var locationDialogShown = false // FLAG TO PREVENT THE LOCATION SETTINGS DIALOG FROM APPEARING MULTIPLE TIMES.

    // SUGGESTIONS ADAPTER
    private lateinit var suggestionsAdapter: SuggestionsAdapter // ADAPTER FOR THE SEARCH SUGGESTIONS

    // ON CREATE
    override fun onCreate(savedInstanceState: Bundle?) { // CALLED WHEN THE ACTIVITY IS FIRST CREATED
        super.onCreate(savedInstanceState) // CALLS THE PARENT CLASS'S ONCREATE METHOD
        enableEdgeToEdge() // ENABLES EDGE-TO-EDGE DISPLAY
        setContentView(binding.root) // SETS THE CONTENT VIEW TO THE ROOT OF THE BINDING

        loadingView = findViewById(R.id.loading_view) // INITIALIZES THE LOADING VIEW
        loadingView.visibility = View.VISIBLE // MAKES THE LOADING VIEW VISIBLE

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this) // INITIALIZES THE FUSED LOCATION CLIENT
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager // INITIALIZES THE CONNECTIVITY MANAGER

        // INITIALIZE THE SUGGESTIONS ADAPTER AND RECYCLERVIEW
        suggestionsAdapter = SuggestionsAdapter(emptyList()) { cityName -> // CREATES A NEW SUGGESTIONS ADAPTER
            fetchWeatherData(cityName, true) // FETCHES THE WEATHER DATA FOR THE SELECTED CITY
            binding.suggestionsRecyclerView.visibility = View.GONE // HIDES THE SUGGESTIONS RECYCLER VIEW
        }
        binding.suggestionsRecyclerView.layoutManager = LinearLayoutManager(this) // SETS THE LAYOUT MANAGER FOR THE SUGGESTIONS RECYCLER VIEW
        binding.suggestionsRecyclerView.adapter = suggestionsAdapter // SETS THE ADAPTER FOR THE SUGGESTIONS RECYCLER VIEW

        // LAUNCHER FOR LOCATION SETTINGS
        // THIS LAUNCHER HANDLES THE RESULT OF RETURNING FROM THE DEVICE'S LOCATION SETTINGS SCREEN.
        locationSettingsLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                locationDialogShown = false // RESET THE FLAG, SINCE THE USER HAS RETURNED FROM THE SETTINGS SCREEN.
                loadingView.visibility = View.VISIBLE // SHOW THE PROGRESS BAR WHILE WE RE-CHECK FOR LOCATION.
                startLocationFlow() // RESTART THE LOCATION FLOW TO GET THE UPDATED STATUS.
            }

        registerNetworkCallback() // REGISTERS A NETWORK CALLBACK TO LISTEN FOR NETWORK CHANGES
        SearchCity() // INITIALIZES THE SEARCH FUNCTIONALITY

        initialLaunchLogic() // EXECUTES THE INITIAL LAUNCH LOGIC
    }

    // INITIAL LAUNCH LOGIC
    private fun initialLaunchLogic() { // LOGIC TO BE EXECUTED ON THE INITIAL LAUNCH OF THE APP
        if (checkInternet()) { // CHECKS IF THE DEVICE IS CONNECTED TO THE INTERNET
            startLocationFlow() // STARTS THE LOCATION FLOW
        } else { // IF THERE IS NO INTERNET CONNECTION
            FancyToast.makeText(this, "Please check your internet connection.", FancyToast.LENGTH_LONG, FancyToast.WARNING, R.drawable.white_cloud, false).show() // SHOWS A WARNING TOAST
            if (!showLastUpdatedData()) { // IF THERE IS NO LAST UPDATED DATA TO SHOW
                loadingView.visibility = View.GONE // HIDE THE LOADING VIEW AS THERE IS NOTHING TO LOAD
            }
        }
    }

    // ON RESUME
    override fun onResume() { // CALLED WHEN THE ACTIVITY WILL START INTERACTING WITH THE USER
        super.onResume() // CALLS THE PARENT CLASS'S ONRESUME METHOD
        // THE LOGIC TO HANDLE RETURNING FROM SETTINGS IS NOW CORRECTLY IN THE LOCATIONSETTINGSLUANCHER.
        // NO SPECIAL LOGIC IS NEEDED HERE TO PREVENT THE DIALOG FROM SHOWING MULTIPLE TIMES.
    }

    // LOCATION FLOW CONTROLLER
    private fun startLocationFlow() { // CONTROLS THE FLOW OF OBTAINING THE USER'S LOCATION
        if (!checkInternet()) { // IF THERE IS NO INTERNET
            if (!showLastUpdatedData()) { // AND IF THERE IS NO LAST DATA TO SHOW
                loadingView.visibility = View.GONE // HIDE THE PROGRESS BAR
            }
            return // STOP THE FLOW
        }

        // CHECK PERMISSION
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || // CHECKS IF FINE LOCATION PERMISSION IS GRANTED
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        ) { // CHECKS IF COARSE LOCATION PERMISSION IS GRANTED
            checkLocationEnabled() // CHECKS IF LOCATION IS ENABLED
        } else { // IF PERMISSION IS NOT GRANTED
            if (!permissionAskedOnce) { // CHECKS IF PERMISSION HAS BEEN ASKED BEFORE
                permissionAskedOnce = true // SETS THE FLAG TO TRUE
                locationPermissionRequest.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) // LAUNCHES THE PERMISSION REQUEST
            } else { // IF PERMISSION HAS BEEN ASKED BEFORE AND WAS DENIED
                if (!showLastUpdatedData()) { // TRY TO SHOW LAST DATA
                    fetchWeatherData("Innichen") // IF THERE IS NO LAST DATA (FRESH INSTALL), FETCH FOR DEFAULT
                }
            }
        }
    }

    // PERMISSION RESULT
    private val locationPermissionRequest = // A LAUNCHER FOR THE LOCATION PERMISSION REQUEST
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions -> // REGISTERS A LAUNCHER FOR MULTIPLE PERMISSIONS
            val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true || // CHECKS IF FINE LOCATION PERMISSION IS GRANTED
                    permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true // CHECKS IF COARSE LOCATION PERMISSION IS GRANTED

            if (granted) { // IF PERMISSION IS GRANTED
                checkLocationEnabled() // CHECKS IF LOCATION IS ENABLED
            } else { // IF PERMISSION IS NOT GRANTED
                if (!showLastUpdatedData()) { // TRY TO SHOW LAST DATA
                    fetchWeatherData("Innichen") // IF THERE IS NO LAST DATA (FRESH INSTALL), FETCH FOR DEFAULT
                }
            }
        }

    // CHECK IF GPS IS ON
    private fun checkLocationEnabled() { // CHECKS IF THE LOCATION SERVICES ARE ENABLED ON THE DEVICE
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager // INITIALIZES THE LOCATION MANAGER
        val enabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || // CHECKS IF GPS PROVIDER IS ENABLED
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) // CHECKS IF NETWORK PROVIDER IS ENABLED

        if (enabled) { // IF LOCATION IS ENABLED
            getCurrentLocation() // GETS THE CURRENT LOCATION
        } else { // IF LOCATION IS NOT ENABLED
            if (locationDialogShown) return // THIS IS THE CRUCIAL CHECK. IF THE DIALOG IS ALREADY SHOWING, DO NOT SHOW ANOTHER ONE.
            locationDialogShown = true // SET THE FLAG TO TRUE BECAUSE WE ARE ABOUT TO SHOW THE DIALOG.

            SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE) // SHOWS A WARNING DIALOG
                .setTitleText("Enable Location") // SETS THE TITLE OF THE DIALOG
                .setContentText("Please enable location services.") // SETS THE CONTENT OF THE DIALOG
                .setConfirmText("Enable") // SETS THE CONFIRM BUTTON TEXT
                .setConfirmClickListener { // SETS THE CONFIRM BUTTON CLICK LISTENER
                    it.dismissWithAnimation() // DISMISSES THE DIALOG WITH AN ANIMATION
                    val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS) // CREATES AN INTENT TO OPEN THE LOCATION SETTINGS
                    locationSettingsLauncher.launch(intent) // LAUNCHES THE LOCATION SETTINGS SCREEN
                }
                .setCancelText("Cancel") // SETS THE CANCEL BUTTON TEXT
                .setCancelClickListener { // SETS THE CANCEL BUTTON CLICK LISTENER
                    it.dismissWithAnimation() // DISMISSES THE DIALOG WITH AN ANIMATION
                    locationDialogShown = false // IMPORTANT: RESET THE FLAG ON CANCELLATION.
                    if (!showLastUpdatedData()) { // TRY TO SHOW LAST DATA
                        fetchWeatherData("Innichen") // IF THERE IS NO LAST DATA (FRESH INSTALL), FETCH FOR DEFAULT
                    }
                }
                .show() // SHOWS THE DIALOG
        }
    }

    // GET LOCATION
    @SuppressLint("MissingPermission") // SUPPRESSES THE MISSING PERMISSION WARNING
    private fun getCurrentLocation() { // GETS THE USER'S CURRENT LOCATION
        loadingView.visibility = View.VISIBLE // SHOW LOADING VIEW
        fusedLocationProviderClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null) // GETS THE CURRENT LOCATION WITH HIGH ACCURACY
            .addOnSuccessListener { location: Location? -> // ADDS A SUCCESS LISTENER
                if (location != null) { // IF THE LOCATION IS NOT NULL
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // CHECKS THE ANDROID VERSION
                        val geoCoder = Geocoder(this, Locale.getDefault()) // INITIALIZES THE GEOCODER
                        geoCoder.getFromLocation(location.latitude, location.longitude, 1) { addresses -> // GETS THE ADDRESS FROM THE LOCATION
                            val cityName = if (addresses.isNotEmpty()) // CHECKS IF THE ADDRESS LIST IS NOT EMPTY
                                addresses[0].locality ?: addresses[0].adminArea // GETS THE CITY NAME
                            else null // SET TO NULL IF NO CITY NAME FOUND

                            if (cityName != null) { // IF A CITY NAME WAS FOUND
                                runOnUiThread { fetchWeatherData(cityName) } // FETCHES THE WEATHER DATA ON THE MAIN THREAD
                            } else { // IF NO CITY NAME WAS FOUND FROM COORDINATES
                                if (!showLastUpdatedData()) { // TRY TO SHOW LAST DATA
                                    fetchWeatherData("Innichen") // FETCH DEFAULT WEATHER DATA
                                }
                            }
                        }
                    } else { // IF THE ANDROID VERSION IS LOWER THAN TIRAMISU
                        Thread { // CREATES A NEW THREAD
                            val cityName = getCityName(location.latitude, location.longitude) // GETS THE CITY NAME
                            runOnUiThread { // SWITCH BACK TO THE MAIN THREAD
                                if (cityName != null) { // IF A CITY NAME WAS FOUND
                                    fetchWeatherData(cityName) // FETCH THE WEATHER DATA FOR THE FOUND CITY
                                } else { // IF A CITY NAME WAS NOT FOUND
                                    if (!showLastUpdatedData()) { // TRY TO SHOW LAST DATA
                                        fetchWeatherData("Innichen") // FETCH DEFAULT WEATHER DATA
                                    }
                                }
                            }
                        }.start() // STARTS THE THREAD
                    }
                } else { // THIS BLOCK IS EXECUTED IF LOCATION IS NULL, MEANING LOCATION COULD NOT BE FETCHED.
                    if (!showLastUpdatedData()) { // TRY TO SHOW LAST KNOWN WEATHER.
                        fetchWeatherData("Innichen") // FETCH DEFAULT WEATHER DATA
                    }
                }
            }
    }

    // GET CITY NAME
    private fun getCityName(lat: Double, long: Double): String? { // GETS THE CITY NAME FROM LATITUDE AND LONGITUDE
        val geoCoder = Geocoder(this, Locale.getDefault()) // INITIALIZES THE GEOCODER
        return try { // TRIES TO GET THE ADDRESS
            val address = geoCoder.getFromLocation(lat, long, 1) // GETS THE ADDRESS FROM THE LOCATION
            address?.firstOrNull()?.locality ?: address?.firstOrNull()?.adminArea // RETURNS THE CITY NAME
        } catch (e: Exception) { // CATCHES ANY EXCEPTION THAT MIGHT OCCUR
            null // RETURNS NULL IF AN EXCEPTION OCCURS
        } // RETURNS NULL IF AN EXCEPTION OCCURS
    }

    // SEARCH
    private fun SearchCity() { // INITIALIZES THE SEARCH FUNCTIONALITY
        val searchView = binding.searchView // GETS THE SEARCH VIEW FROM THE BINDING
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener { // SETS A QUERY TEXT LISTENER

            override fun onQueryTextSubmit(query: String?): Boolean { // CALLED WHEN THE USER SUBMITS THE QUERY
                if (query.isNullOrBlank()) { // CHECKS IF THE QUERY IS NULL OR BLANK
                    FancyToast.makeText(this@MainActivity, "Please Enter A City Or Country Name", FancyToast.LENGTH_SHORT, FancyToast.WARNING, R.drawable.white_cloud, false).show() // SHOW A WARNING TOAST IF THE QUERY IS BLANK
                } else { // IF THE QUERY IS NOT NULL OR BLANK
                    if (checkInternet()) { // CHECKS FOR INTERNET CONNECTION
                        loadingView.visibility = View.VISIBLE // SHOWS THE LOADING VIEW
                        fetchWeatherData(query, true) // FETCHES THE WEATHER DATA FOR THE SEARCHED CITY
                    } else { // IF THERE IS NO INTERNET CONNECTION
                        showLastUpdatedData() // SHOWS THE LAST UPDATED DATA
                        FancyToast.makeText(this@MainActivity, "Network error, please check your internet connection", FancyToast.LENGTH_SHORT, FancyToast.ERROR, R.drawable.white_cloud, false).show() // SHOWS AN ERROR TOAST
                    }
                }
                return true // RETURNS TRUE TO INDICATE THAT THE QUERY HAS BEEN HANDLED
            }

            override fun onQueryTextChange(newText: String?): Boolean { // CALLED WHEN THE QUERY TEXT CHANGES
                if (newText.isNullOrBlank()) { // CHECKS IF THE NEW TEXT IS NULL OR BLANK
                    binding.suggestionsRecyclerView.visibility = View.GONE // HIDES THE SUGGESTIONS RECYCLER VIEW
                } else { // IF THE NEW TEXT IS NOT NULL OR BLANK
                    fetchCitySuggestions(newText) // FETCHES CITY SUGGESTIONS
                }
                return true // RETURNS TRUE TO INDICATE THAT THE QUERY HAS BEEN HANDLED
            }
        })
    }

    // FETCH CITY SUGGESTIONS
    // THIS FUNCTION USES THE APIINTERFACE TO FETCH CITY SUGGESTIONS FROM THE GEOCODING API.
    // THE RESPONSE IS PARSED USING THE GEOCODINGRESPONSE DATA CLASS, WHICH WAS CREATED TO HANDLE THE JSON RESPONSE FROM THE GEOCODING API.
    private fun fetchCitySuggestions(query: String) { // FETCHES CITY SUGGESTIONS FROM THE GEOCODING API
        val retrofit = Retrofit.Builder() // CREATES A NEW RETROFIT BUILDER
            .addConverterFactory(GsonConverterFactory.create()) // ADDS A GSON CONVERTER FACTORY
            .baseUrl("https://geocoding-api.open-meteo.com/v1/") // SETS THE BASE URL OF THE GEOCODING API
            .build().create(ApiInterface::class.java) // CREATES AN INSTANCE OF THE API INTERFACE

        val response = retrofit.getCitySuggestions(query, 10) // GETS CITY SUGGESTIONS FOR THE GIVEN QUERY

        response.enqueue(object : Callback<GeoCodingResponse> { // ENQUEUES THE REQUEST
            override fun onResponse(call: Call<GeoCodingResponse>, response: Response<GeoCodingResponse>) { // CALLED WHEN A RESPONSE IS RECEIVED
                if (response.isSuccessful) { // CHECKS IF THE RESPONSE IS SUCCESSFUL
                    val suggestions = response.body()?.results?.map { "${it.name}, ${it.country}" } ?: emptyList() // MAPS THE RESULTS TO A LIST OF STRINGS
                    if (suggestions.isEmpty()) { // CHECKS IF THE SUGGESTIONS LIST IS EMPTY
                        suggestionsAdapter.updateData(listOf("No Result Available..")) // UPDATES THE ADAPTER WITH A "NO RESULT AVAILABLE.." MESSAGE
                    } else { // IF THE SUGGESTIONS LIST IS NOT EMPTY
                        suggestionsAdapter.updateData(suggestions) // UPDATES THE ADAPTER WITH THE SUGGESTIONS
                    }
                    binding.suggestionsRecyclerView.visibility = View.VISIBLE // SHOWS THE SUGGESTIONS RECYCLER VIEW
                }
            }

            override fun onFailure(call: Call<GeoCodingResponse>, t: Throwable) { // CALLED WHEN THE REQUEST FAILS
                // YOU COULD LOG THE ERROR OR SHOW A TOAST TO THE USER. FOR NOW, WE DO NOTHING.
            }
        })
    }

    // RETROFIT API
    // THIS FUNCTION USES THE APIINTERFACE TO FETCH WEATHER DATA FROM THE OPENWEATHERMAP API.
    // THE APIINTERFACE WAS CREATED TO DEFINE THE API ENDPOINTS FOR THE OPENWEATHERMAP API.
    private fun fetchWeatherData(cityName: String, isSearchAction: Boolean = false) { // FETCHES WEATHER DATA FROM THE API

        val retrofit = Retrofit.Builder() // CREATES A NEW RETROFIT BUILDER
            .addConverterFactory(GsonConverterFactory.create()) // ADDS A GSON CONVERTER FACTORY
            .baseUrl("https://api.openweathermap.org/data/2.5/") // SETS THE BASE URL OF THE API
            .build().create(ApiInterface::class.java) // CREATES AN INSTANCE OF THE API INTERFACE

        val response = retrofit.getWeatherData(cityName, "b6e8352ce03216a9fd44c88e118a94c3", "metric") // GETS THE WEATHER DATA FOR THE SPECIFIED CITY

        response.enqueue(object : Callback<WeatherApp?> { // ENQUEUES THE REQUEST

            override fun onResponse(call: Call<WeatherApp?>, response: Response<WeatherApp?>) { // CALLED WHEN A RESPONSE IS RECEIVED
                loadingView.visibility = View.GONE // HIDES THE LOADING VIEW
                val responseBody = response.body() // GETS THE RESPONSE BODY

                if (response.isSuccessful && responseBody != null) { // CHECKS IF THE RESPONSE IS SUCCESSFUL AND THE BODY IS NOT NULL

                    updateUI(cityName, responseBody) // UPDATES THE UI WITH THE NEW DATA

                    FancyToast.makeText(this@MainActivity, "Weather updated", FancyToast.LENGTH_SHORT, FancyToast.SUCCESS, R.drawable.white_cloud, false).show() // SHOWS A SUCCESS TOAST

                    val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) // GETS THE SHARED PREFERENCES
                    sharedPrefs.edit().putString(LAST_RESPONSE, Gson().toJson(responseBody)).putString(LAST_CITY, cityName).apply() // SAVES THE LAST RESPONSE AND CITY

                } else if (isSearchAction) { // IF THE SEARCH ACTION FAILED
                    FancyToast.makeText(this@MainActivity, "Please enter a correct city or place...", FancyToast.LENGTH_SHORT, FancyToast.ERROR, R.drawable.white_cloud, false).show() // SHOW AN ERROR TOAST FOR AN INVALID CITY
                }
            }

            override fun onFailure(call: Call<WeatherApp?>, t: Throwable) { // CALLED WHEN THE REQUEST FAILS
                loadingView.visibility = View.GONE // HIDES THE LOADING VIEW
                if (!showLastUpdatedData()) { // TRY TO SHOW LAST DATA
                    FancyToast.makeText(this@MainActivity, "Please check your internet connection", FancyToast.LENGTH_LONG, FancyToast.ERROR, R.drawable.white_cloud, false).show() // SHOWS AN ERROR TOAST
                }
            }
        })
    }

    // UPDATE UI
    @SuppressLint("SetTextI18n") // SUPPRESSES THE SETTEXTI18N WARNING
    private fun updateUI(cityName: String, responseBody: WeatherApp) { // UPDATES THE UI WITH THE WEATHER DATA

        val main = responseBody.main // GETS THE MAIN WEATHER DATA
        val wind = responseBody.wind // GETS THE WIND DATA
        val sys = responseBody.sys // GETS THE SYSTEM DATA
        val weatherList = responseBody.weather // GETS THE WEATHER LIST

        binding.temp.text = "${main?.temp ?: 0.0} °C" // SETS THE TEMPERATURE
        binding.windSpeed.text = "${wind?.speed ?: 0.0} m/s" // SETS THE WIND SPEED
        binding.humidity.text = "${main?.humidity ?: 0} %" // SETS THE HUMIDITY

        val sunrise = sys?.sunrise?.toLong() ?: 0L // GETS THE SUNRISE TIME
        val sunset = sys?.sunset?.toLong() ?: 0L // GETS THE SUNSET TIME
        binding.sunrise.text = "${time(sunrise)} am" // SETS THE SUNRISE TIME
        binding.sunset.text = "${time(sunset)} pm" // SETS THE SUNSET TIME

        binding.sea.text = "${main?.sea_level ?: 0} hPa" // SETS THE SEA LEVEL

        val condition = weatherList?.firstOrNull()?.main ?: "unknown" // GETS THE WEATHER CONDITION
        binding.conditions.text = condition // SETS THE WEATHER CONDITION
        binding.maxTemp.text = "Max: ${main?.temp_max ?: 0.0} °C" // SETS THE MAX TEMPERATURE
        binding.minTemp.text = "Min: ${main?.temp_min ?: 0.0} °C" // SETS THE MIN TEMPERATURE
        binding.weather.text = condition // SETS THE WEATHER CONDITION

        binding.day.text = dayName(System.currentTimeMillis()) // SETS THE DAY OF THE WEEK
        binding.date.text = date() // SETS THE DATE
        binding.cityName.text = cityName.uppercase(Locale.getDefault()) // SETS THE CITY NAME IN UPPERCASE

        changeImagesAccordingtoWeatherCondition(condition) // CHANGES THE IMAGES BASED ON THE WEATHER CONDITION
    }

    // SHOW LAST UPDATED DATA
    private fun showLastUpdatedData(): Boolean { // SHOWS THE LAST UPDATED WEATHER DATA AND RETURNS TRUE IF DATA WAS FOUND AND SHOWN
        val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) // GETS THE SHARED PREFERENCES
        val json = sharedPrefs.getString(LAST_RESPONSE, null) // GETS THE LAST RESPONSE FROM SHARED PREFERENCES
        val lastCity = sharedPrefs.getString(LAST_CITY, "Innichen") // GETS THE LAST CITY FROM SHARED PREFERENCES

        if (json != null) { // CHECKS IF THE LAST RESPONSE IS NOT NULL
            val lastResponse = Gson().fromJson(json, WeatherApp::class.java) // CONVERTS THE JSON STRING TO A WEATHERAPP OBJECT
            updateUI(lastResponse.name ?: lastCity ?: "Innichen", lastResponse) // UPDATES THE UI WITH THE LAST RESPONSE
            loadingView.visibility = View.GONE // HIDE LOADING VIEW SINCE WE HAVE SHOWN THE LAST DATA
            return true // RETURN TRUE INDICATING DATA WAS SHOWN
        }
        return false // RETURN FALSE IF NO DATA WAS FOUND
    }


    // NETWORK OBSERVER
    private fun registerNetworkCallback() { // REGISTERS A NETWORK CALLBACK TO LISTEN FOR NETWORK CHANGES

        networkCallback = object : ConnectivityManager.NetworkCallback() { // CREATES A NEW NETWORK CALLBACK

            override fun onAvailable(network: Network) { // CALLED WHEN A NETWORK IS AVAILABLE
                if (!isConnected) { // CHECKS IF THE DEVICE IS NOT CONNECTED
                    isConnected = true // SETS THE CONNECTED FLAG TO TRUE
                    runOnUiThread { // RUNS THE CODE ON THE MAIN THREAD
                        startLocationFlow() // RETRIES THE LOCATION FLOW
                    }
                }
            }

            override fun onLost(network: Network) { // CALLED WHEN A NETWORK IS LOST
                isConnected = false // SETS THE CONNECTED FLAG TO FALSE
                runOnUiThread { // RUNS THE CODE ON THE MAIN THREAD
                    FancyToast.makeText(this@MainActivity, "Please check your internet connection", FancyToast.LENGTH_LONG, FancyToast.WARNING, R.drawable.white_cloud, false).show() // SHOWS A WARNING TOAST
                }
            }
        }

        connectivityManager.registerDefaultNetworkCallback(networkCallback) // REGISTERS THE NETWORK CALLBACK
    }

    // CHECK INTERNET
    private fun checkInternet(): Boolean { // CHECKS IF THE DEVICE IS CONNECTED TO THE INTERNET
        val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork) // GETS THE NETWORK CAPABILITIES
        return capabilities != null && // CHECKS IF THE CAPABILITIES ARE NOT NULL
                (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || // CHECKS IF WI-FI IS AVAILABLE
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) // CHECKS IF CELLULAR IS AVAILABLE
    }

    // CHANGE IMAGES ACCORDING TO WEATHER CONDITION
    private fun changeImagesAccordingtoWeatherCondition(conditions: String) { // CHANGES THE BACKGROUND AND ANIMATION BASED ON THE WEATHER CONDITION
        when (conditions) { // SWITCHES ON THE WEATHER CONDITION
            "Clear Sky", "Sunny", "Clear" -> { // WHEN THE WEATHER IS CLEAR OR SUNNY
                binding.root.setBackgroundResource(R.drawable.sunny_background) // SET THE BACKGROUND TO THE SUNNY BACKGROUND DRAWABLE
                binding.lottieAnimationView.setAnimation(R.raw.sun) // SET THE LOTTIE ANIMATION TO THE SUN ANIMATION
            } // SETS THE BACKGROUND AND ANIMATION FOR SUNNY WEATHER
            "Partly Clouds", "Clouds", "Overcast", "Mist", "Foggy", "Haze" -> { // WHEN THE WEATHER IS CLOUDY OR HAZY
                binding.root.setBackgroundResource(R.drawable.colud_background) // SET THE BACKGROUND TO THE CLOUDY BACKGROUND DRAWABLE
                binding.lottieAnimationView.setAnimation(R.raw.cloud) // SET THE LOTTIE ANIMATION TO THE CLOUD ANIMATION
            } // SETS THE BACKGROUND AND ANIMATION FOR CLOUDY WEATHER
            "Light Rain", "Drizzle", "Moderate Rain", "Showers", "Heavy Rain", "Rain" -> { // WHEN IT IS RAINING
                binding.root.setBackgroundResource(R.drawable.rain_background) // SET THE BACKGROUND TO THE RAINY BACKGROUND DRAWABLE
                binding.lottieAnimationView.setAnimation(R.raw.rain) // SET THE LOTTIE ANIMATION TO THE RAIN ANIMATION
            } // SETS THE BACKGROUND AND ANIMATION FOR RAINY WEATHER
            "Light Snow", "Moderate Snow", "Heavy Snow", "Blizzard", "Snow" -> { // WHEN IT IS SNOWING
                binding.root.setBackgroundResource(R.drawable.snow_background) // SET THE BACKGROUND TO THE SNOWY BACKGROUND DRAWABLE
                binding.lottieAnimationView.setAnimation(R.raw.snow) // SET THE LOTTIE ANIMATION TO THE SNOW ANIMATION
            } // SETS THE BACKGROUND AND ANIMATION FOR SNOWY WEATHER
            else -> { // FOR ANY OTHER WEATHER CONDITION
                binding.root.setBackgroundResource(R.drawable.sunny_background) // SET THE BACKGROUND TO THE SUNNY BACKGROUND DRAWABLE AS A DEFAULT
                binding.lottieAnimationView.setAnimation(R.raw.sun) // SET THE LOTTIE ANIMATION TO THE SUN ANIMATION AS A DEFAULT
            } // SETS THE DEFAULT BACKGROUND AND ANIMATION
        }
        binding.lottieAnimationView.playAnimation() // PLAYS THE LOTTIE ANIMATION
    }

    // DAY NAME
    fun dayName(timeStamp: Long): String { // GETS THE DAY NAME FROM A TIMESTAMP
        val sdf = SimpleDateFormat("EEEE", Locale.getDefault()) // CREATES A NEW SIMPLE DATE FORMAT OBJECT
        return sdf.format(Date()) // RETURNS THE FORMATTED DAY NAME
    }

    // DATE
    private fun date(): String { // GETS THE CURRENT DATE
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) // CREATES A NEW SIMPLE DATE FORMAT OBJECT
        return sdf.format(Date()) // RETURNS THE FORMATTED DATE
    }

    // TIME
    private fun time(timeStamp: Long): String { // GETS THE TIME FROM A TIMESTAMP
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault()) // CREATES A NEW SIMPLE DATE FORMAT OBJECT
        return sdf.format(Date(timeStamp * 1000)) // RETURNS THE FORMATTED TIME
    }

    // ON DESTROY
    override fun onDestroy() { // CALLED WHEN THE ACTIVITY IS DESTROYED
        super.onDestroy() // CALLS THE PARENT CLASS'S ONDESTROY METHOD
        connectivityManager.unregisterNetworkCallback(networkCallback) // UNREGISTERS THE NETWORK CALLBACK
    }

    // SUGGESTIONS ADAPTER
    inner class SuggestionsAdapter(
        private var suggestions: List<String>, // A LIST OF SUGGESTIONS TO BE DISPLAYED
        private val onItemClick: (String) -> Unit // A LAMBDA FUNCTION TO BE CALLED WHEN AN ITEM IS CLICKED
    ) : RecyclerView.Adapter<SuggestionsAdapter.SuggestionViewHolder>() { // AN ADAPTER FOR THE SUGGESTIONS RECYCLER VIEW

        // SUGGESTION VIEW HOLDER
        inner class SuggestionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) { // A VIEW HOLDER FOR A SINGLE SUGGESTION ITEM
            val textView: TextView = itemView.findViewById(R.id.suggestion_text_view) // THE TEXT VIEW THAT DISPLAYS THE SUGGESTION
        }

        // ON CREATE VIEW HOLDER
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SuggestionViewHolder { // CALLED WHEN A NEW VIEW HOLDER IS CREATED
            val view = LayoutInflater.from(parent.context).inflate(R.layout.suggestion_item, parent, false) // INFLATES THE LAYOUT FOR A SINGLE SUGGESTION ITEM
            return SuggestionViewHolder(view) // RETURNS A NEW VIEW HOLDER
        }

        // ON BIND VIEW HOLDER
        override fun onBindViewHolder(holder: SuggestionViewHolder, position: Int) { // CALLED WHEN A VIEW HOLDER IS BOUND TO A NEW POSITION
            val suggestion = suggestions[position] // GETS THE SUGGESTION AT THE CURRENT POSITION
            holder.textView.text = suggestion // SETS THE TEXT OF THE TEXT VIEW TO THE SUGGESTION
            holder.itemView.setOnClickListener { // SETS A CLICK LISTENER ON THE ITEM VIEW
                if (suggestion != "No Result Available..") { // CHECKS IF THE SUGGESTION IS NOT "NO RESULT AVAILABLE.."
                    onItemClick(suggestion) // CALLS THE ON ITEM CLICK LAMBDA FUNCTION
                }
            }
        }

        // GET ITEM COUNT
        override fun getItemCount(): Int = suggestions.size // RETURNS THE NUMBER OF SUGGESTIONS

        // UPDATE DATA
        fun updateData(newSuggestions: List<String>) { // UPDATES THE SUGGESTIONS LIST
            suggestions = newSuggestions // SETS THE NEW SUGGESTIONS
            notifyDataSetChanged() // NOTIFIES THE ADAPTER THAT THE DATA HAS CHANGED
        }
    }
}
