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

package com.ayushcodes.weatherapp

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Bundle
import android.widget.SearchView
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import cn.pedant.SweetAlert.SweetAlertDialog
import com.ayushcodes.weatherapp.databinding.ActivityMainBinding
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

@Suppress("USELESS_ELVIS")
class MainActivity : AppCompatActivity() {

    private val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var networkCallback: ConnectivityManager.NetworkCallback
    private var isConnected = false

    private val PREFS_NAME = "WeatherPrefs"
    private val LAST_RESPONSE = "LastWeatherResponse"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)

        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        registerNetworkCallback()

        if (checkInternet()) {
            fetchWeatherData("Jaipur")
        } else {
            showLastUpdatedData()
            FancyToast.makeText(
                this,
                "Network error, please check your internet connection",
                FancyToast.LENGTH_LONG,
                FancyToast.ERROR,
                R.drawable.white_cloud,
                false
            ).show()
        }
        SearchCity()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                SweetAlertDialog(this@MainActivity, SweetAlertDialog.WARNING_TYPE)
                    .setTitleText("Exit App")
                    .setContentText("You Really Want To Exit The App?")
                    .setConfirmText("Yes")
                    .setConfirmClickListener { sDialog ->
                        sDialog.dismissWithAnimation()
                        finish()
                    }
                    .setCancelText("Cancel")
                    .setCancelClickListener { sDialog ->
                        sDialog.dismissWithAnimation()
                    }
                    .show()
            }
        })
    }

    private fun SearchCity() {
        val searchView = binding.searchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (query.isNullOrBlank()) {
                    // Empty input
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
                        fetchWeatherData(query, true) // search action
                    } else {
                        showLastUpdatedData()
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

            override fun onQueryTextChange(newText: String?): Boolean = true
        })
    }
    // Added extra param: isSearchAction
    private fun fetchWeatherData(cityName: String, isSearchAction: Boolean = false) {
        val retrofit = Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl("https://api.openweathermap.org/data/2.5/")
            .build().create(ApiInterface::class.java)

        val response = retrofit.getWeatherData(cityName, "b6e8352ce03216a9fd44c88e118a94c3", "metric")
        response.enqueue(object : Callback<WeatherApp> {
            @SuppressLint("SetTextI18n", "UseKtx")
            override fun onResponse(call: Call<WeatherApp?>, response: Response<WeatherApp?>) {
                val responseBody = response.body()
                if (response.isSuccessful && responseBody != null) {
                    updateUI(cityName, responseBody)

                    // Save last response
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

    @SuppressLint("SetTextI18n")
    private fun updateUI(cityName: String, responseBody: WeatherApp) {
        binding.temp.text = "${responseBody.main.temp} °C"
        binding.windSpeed.text = "${responseBody.wind.speed} m/s"
        binding.humidity.text = "${responseBody.main.humidity} %"
        binding.sunrise.text = "${time(responseBody.sys.sunrise.toLong())} am"
        binding.sunset.text = "${time(responseBody.sys.sunset.toLong())} pm"
        binding.sea.text = "${responseBody.main.sea_level} hPa"

        val condition = responseBody.weather.firstOrNull()?.main ?: "unknown"
        binding.conditions.text = condition
        binding.maxTemp.text = "Max: ${responseBody.main.temp_max} °C"
        binding.minTemp.text = "Min: ${responseBody.main.temp_min} °C"
        binding.weather.text = condition

        binding.day.text = dayName(System.currentTimeMillis())
        binding.date.text = date()
        binding.cityName.text = cityName

        changeImagesAccordingtoWeatherCondition(condition)
    }

    private fun showLastUpdatedData() {
        val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = sharedPrefs.getString(LAST_RESPONSE, null)

        if (json != null) {
            val lastResponse = Gson().fromJson(json, WeatherApp::class.java)
            updateUI(lastResponse.name ?: "Last City", lastResponse)
        }
    }
    private fun registerNetworkCallback() {
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                if (!isConnected) {
                    isConnected = true
                    runOnUiThread {
                        FancyToast.makeText(
                            this@MainActivity,
                            "Updating weather...",
                            FancyToast.LENGTH_SHORT,
                            FancyToast.CONFUSING,
                            R.drawable.white_cloud,
                            false
                        ).show()
                        fetchWeatherData(binding.cityName.text.toString())
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
                isConnected = false
                runOnUiThread {
                    showLastUpdatedData()
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
        connectivityManager.registerDefaultNetworkCallback(networkCallback)
    }

    private fun checkInternet(): Boolean {
        val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        return capabilities != null &&
                (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
    }

    private fun changeImagesAccordingtoWeatherCondition(conditions: String) {
        when (conditions) {
            "Clear Sky", "Sunny", "Clear" -> {
                binding.root.setBackgroundResource(R.drawable.sunny_background)
                binding.lottieAnimationView.setAnimation(R.raw.sun)
                FancyToast.makeText(this, "Weather Is Sunny", FancyToast.LENGTH_SHORT, FancyToast.CONFUSING, R.drawable.sunny, false)
            }
            "Partly Clouds", "Clouds", "Overcast", "Mist", "Foggy", "Haze" -> {
                binding.root.setBackgroundResource(R.drawable.colud_background)
                binding.lottieAnimationView.setAnimation(R.raw.cloud)
                FancyToast.makeText(this, "Weather Is Cloudy", FancyToast.LENGTH_SHORT, FancyToast.CONFUSING, R.drawable.cloud_black, false)
            }
            "Light Rain", "Drizzle", "Moderate Rain", "Showers", "Heavy Rain", "Rain" -> {
                binding.root.setBackgroundResource(R.drawable.rain_background)
                binding.lottieAnimationView.setAnimation(R.raw.rain)
                FancyToast.makeText(this, "Weather Is Rainy", FancyToast.LENGTH_SHORT, FancyToast.CONFUSING, R.drawable.rain, false)
            }
            "Light Snow", "Moderate Snow", "Heavy Snow", "Blizzard", "Snow" -> {
                binding.root.setBackgroundResource(R.drawable.snow_background)
                binding.lottieAnimationView.setAnimation(R.raw.snow)
                FancyToast.makeText(this, "Weather Is Snowy", FancyToast.LENGTH_SHORT, FancyToast.CONFUSING, R.drawable.snow, false)
            }
            else -> {
                binding.root.setBackgroundResource(R.drawable.sunny_background)
                binding.lottieAnimationView.setAnimation(R.raw.sun)
                FancyToast.makeText(this, "Weather Is Sunny", FancyToast.LENGTH_SHORT, FancyToast.CONFUSING, R.drawable.sunny, false)
            }
        }
        binding.lottieAnimationView.playAnimation()
    }

    fun dayName(timeStamp: Long): String {
        val sdf = SimpleDateFormat("EEEE", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun date(): String {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun time(timeStamp: Long): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date(timeStamp * 1000))
    }

    override fun onDestroy() {
        super.onDestroy()
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }
}
