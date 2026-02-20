@file:Suppress("ALL") // suppress warnings
package com.ayushcodes.weatherapp // package name

// ===== Imports =====
import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.SearchView
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import cn.pedant.SweetAlert.SweetAlertDialog
import com.ayushcodes.weatherapp.databinding.ActivityMainBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.gson.Gson
import com.shashank.sony.fancytoastlib.FancyToast
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.*

// ===== Activity =====
@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {

    private val binding: ActivityMainBinding by lazy { // viewbinding
        ActivityMainBinding.inflate(layoutInflater)
    }

    private lateinit var connectivityManager: ConnectivityManager // network manager
    private lateinit var networkCallback: ConnectivityManager.NetworkCallback // network listener
    private var isConnected = false // network state flag

    private val PREFS_NAME = "WeatherPrefs" // sharedpref name
    private val LAST_RESPONSE = "LastWeatherResponse" // key
    private val LAST_CITY = "LastCity" // key

    private lateinit var fusedLocationClient: FusedLocationProviderClient // location client
    private lateinit var locationSettingsLauncher: ActivityResultLauncher<Intent> // settings launcher

    private var permissionAskedOnce = false // ⭐ prevent repeated permission dialog
    private var locationDialogShown = false // ⭐ prevent repeated enable-location dialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // full screen layout
        setContentView(binding.root) // set UI

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this) // init location
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager // init network

        // launcher when user returns from location settings
        locationSettingsLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                locationDialogShown = false // ⭐ reset flag
                if (result.resultCode == Activity.RESULT_OK) {
                    startLocationFlow() // ⭐ retry flow
                } else {
                    showLastUpdatedData() // fallback
                }
            }

        registerNetworkCallback() // start listening internet
        SearchCity() // init search

        initialLaunchLogic() // ⭐ fresh install logic
    }

    // ===== Initial Launch =====
    private fun initialLaunchLogic() {
        if (checkInternet()) { // if internet exists
            startLocationFlow() // ⭐ begin permission flow
        } else {
            FancyToast.makeText(this,"Please check your internet connection.",FancyToast.LENGTH_LONG,FancyToast.WARNING,R.drawable.white_cloud,false).show()
            fetchWeatherData("India") // default
        }
    }

    override fun onResume() {
        super.onResume()
        // ⭐ DO NOTHING HERE — prevents multiple dialogs
    }

    // ===== Location Flow Controller =====
    private fun startLocationFlow() {

        if (!checkInternet()) return // ⭐ stop if no internet

        // check permission first
        if (ActivityCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this,Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            checkLocationEnabled() // already allowed

        } else {

            if (!permissionAskedOnce) { // ⭐ ask only once
                permissionAskedOnce = true
                locationPermissionRequest.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION))
            } else {
                showLastUpdatedData()
            }
        }
    }

    // ===== Permission Result =====
    private val locationPermissionRequest =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->

            val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

            if (granted) {
                FancyToast.makeText(this,"Updating weather...",FancyToast.LENGTH_SHORT,FancyToast.INFO,R.drawable.white_cloud,false).show()
                checkLocationEnabled()
            } else {
                showLastUpdatedData()
            }
        }

    // ===== Check if GPS ON =====
    private fun checkLocationEnabled() {

        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val enabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        if (enabled) {
            getCurrentLocation()
        } else {
            if (locationDialogShown) return // ⭐ prevent repeat dialog
            locationDialogShown = true

            SweetAlertDialog(this,SweetAlertDialog.WARNING_TYPE)
                .setTitleText("Enable Location")
                .setContentText("Please enable location services.")
                .setConfirmText("Enable")
                .setConfirmClickListener {
                    it.dismissWithAnimation()
                    val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    locationSettingsLauncher.launch(intent)
                }
                .setCancelText("Cancel")
                .setCancelClickListener {
                    it.dismissWithAnimation()
                    showLastUpdatedData()
                }
                .show()
        }
    }

    // ===== Get Location =====
    @SuppressLint("MissingPermission")
    private fun getCurrentLocation() {

        if (!checkInternet()) {
            showLastUpdatedData()
            FancyToast.makeText(this,"Please check your internet connection",FancyToast.LENGTH_LONG,FancyToast.WARNING,R.drawable.white_cloud,false).show()
            return
        }

        FancyToast.makeText(this,"Updating weather...",FancyToast.LENGTH_SHORT,FancyToast.INFO,R.drawable.white_cloud,false).show()

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY,null)
            .addOnSuccessListener { location ->

                if (location != null) {

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

                        val geoCoder = Geocoder(this,Locale.getDefault())
                        geoCoder.getFromLocation(location.latitude,location.longitude,1){ addresses ->
                            val cityName = if(addresses.isNotEmpty())
                                addresses[0].locality ?: addresses[0].adminArea
                            else "India"

                            runOnUiThread { fetchWeatherData(cityName ?: "India") }
                        }

                    } else {

                        Thread {
                            val cityName = getCityName(location.latitude,location.longitude)
                            runOnUiThread { fetchWeatherData(cityName ?: "India") }
                        }.start()
                    }

                } else fetchWeatherData("India")
            }
    }

    // ===== Get City Name =====
    private fun getCityName(lat:Double,long:Double):String?{
        val geoCoder=Geocoder(this,Locale.getDefault())
        return try{
            val address=geoCoder.getFromLocation(lat,long,1)
            address?.firstOrNull()?.locality ?: address?.firstOrNull()?.adminArea
        }catch(e:Exception){ null }
    }

    // ===== Search =====
    private fun SearchCity(){
        val searchView=binding.searchView
        searchView.setOnQueryTextListener(object:SearchView.OnQueryTextListener{

            override fun onQueryTextSubmit(query:String?):Boolean{

                if(query.isNullOrBlank()){
                    FancyToast.makeText(this@MainActivity,"Please Enter A City Or Country Name",FancyToast.LENGTH_SHORT,FancyToast.WARNING,R.drawable.white_cloud,false).show()
                }else{
                    if(checkInternet()) fetchWeatherData(query,true)
                    else{
                        showLastUpdatedData()
                        FancyToast.makeText(this@MainActivity,"Network error, please check your internet connection",FancyToast.LENGTH_SHORT,FancyToast.ERROR,R.drawable.white_cloud,false).show()
                    }
                }
                return true
            }

            override fun onQueryTextChange(newText:String?)=true
        })
    }

    // ===== Retrofit API =====
    private fun fetchWeatherData(cityName:String,isSearchAction:Boolean=false){

        FancyToast.makeText(this,"Updating weather...",FancyToast.LENGTH_SHORT,FancyToast.INFO,R.drawable.white_cloud,false).show()

        val retrofit=Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl("https://api.openweathermap.org/data/2.5/")
            .build().create(ApiInterface::class.java)

        val response=retrofit.getWeatherData(cityName,"b6e8352ce03216a9fd44c88e118a94c3","metric")

        response.enqueue(object:Callback<WeatherApp>{

            override fun onResponse(call:Call<WeatherApp?>,response:Response<WeatherApp?>){

                val responseBody=response.body()

                if(response.isSuccessful && responseBody!=null){

                    updateUI(cityName,responseBody)

                    FancyToast.makeText(this@MainActivity,"Weather updated",FancyToast.LENGTH_SHORT,FancyToast.SUCCESS,R.drawable.white_cloud,false).show()

                    val sharedPrefs=getSharedPreferences(PREFS_NAME,Context.MODE_PRIVATE)
                    sharedPrefs.edit().putString(LAST_RESPONSE,Gson().toJson(responseBody)).putString(LAST_CITY,cityName).apply()

                }else if(isSearchAction){
                    FancyToast.makeText(this@MainActivity,"This City or Country Doesn't Exist",FancyToast.LENGTH_SHORT,FancyToast.ERROR,R.drawable.white_cloud,false).show()
                }
            }

            override fun onFailure(call:Call<WeatherApp?>,t:Throwable){
                FancyToast.makeText(this@MainActivity,"Please check your internet connection",FancyToast.LENGTH_LONG,FancyToast.ERROR,R.drawable.white_cloud,false).show()
            }
        })
    }

    // ===== Update UI =====
    @SuppressLint("SetTextI18n")
    private fun updateUI(cityName:String,responseBody:WeatherApp){

        val main=responseBody.main
        val wind=responseBody.wind
        val sys=responseBody.sys
        val weatherList=responseBody.weather

        binding.temp.text="${main?.temp ?: 0.0} °C"
        binding.windSpeed.text="${wind?.speed ?: 0.0} m/s"
        binding.humidity.text="${main?.humidity ?: 0} %"

        val sunrise=sys?.sunrise?.toLong() ?: 0L
        val sunset=sys?.sunset?.toLong() ?: 0L
        binding.sunrise.text="${time(sunrise)} am"
        binding.sunset.text="${time(sunset)} pm"

        binding.sea.text="${main?.sea_level ?: 0} hPa"

        val condition=weatherList?.firstOrNull()?.main ?: "unknown"
        binding.conditions.text=condition
        binding.maxTemp.text="Max: ${main?.temp_max ?: 0.0} °C"
        binding.minTemp.text="Min: ${main?.temp_min ?: 0.0} °C"
        binding.weather.text=condition

        binding.day.text=dayName(System.currentTimeMillis())
        binding.date.text=date()
        binding.cityName.text=cityName

        changeImagesAccordingtoWeatherCondition(condition)
    }

    private fun showLastUpdatedData(){
        val sharedPrefs=getSharedPreferences(PREFS_NAME,Context.MODE_PRIVATE)
        val json=sharedPrefs.getString(LAST_RESPONSE,null)
        val lastCity=sharedPrefs.getString(LAST_CITY,"India")

        if(json!=null){
            val lastResponse=Gson().fromJson(json,WeatherApp::class.java)
            updateUI(lastResponse.name ?: lastCity ?: "India",lastResponse)
        }else fetchWeatherData("India")
    }

    // ===== Network Observer =====
    private fun registerNetworkCallback(){

        networkCallback=object:ConnectivityManager.NetworkCallback(){

            override fun onAvailable(network:Network){
                if(!isConnected){
                    isConnected=true
                    runOnUiThread{
                        FancyToast.makeText(this@MainActivity,"Feed Updated.",FancyToast.LENGTH_SHORT,FancyToast.SUCCESS,R.drawable.white_cloud,false).show()
                        startLocationFlow() // ⭐ retry location only once
                    }
                }
            }

            override fun onLost(network:Network){
                isConnected=false
                runOnUiThread{
                    showLastUpdatedData()
                    FancyToast.makeText(this@MainActivity,"Please check your internet connection",FancyToast.LENGTH_LONG,FancyToast.WARNING,R.drawable.white_cloud,false).show()
                }
            }
        }

        connectivityManager.registerDefaultNetworkCallback(networkCallback)
    }

    private fun checkInternet():Boolean{
        val capabilities=connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        return capabilities!=null &&
                (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
    }

    private fun changeImagesAccordingtoWeatherCondition(conditions:String){
        when(conditions){
            "Clear Sky","Sunny","Clear"->{ binding.root.setBackgroundResource(R.drawable.sunny_background); binding.lottieAnimationView.setAnimation(R.raw.sun) }
            "Partly Clouds","Clouds","Overcast","Mist","Foggy","Haze"->{ binding.root.setBackgroundResource(R.drawable.colud_background); binding.lottieAnimationView.setAnimation(R.raw.cloud) }
            "Light Rain","Drizzle","Moderate Rain","Showers","Heavy Rain","Rain"->{ binding.root.setBackgroundResource(R.drawable.rain_background); binding.lottieAnimationView.setAnimation(R.raw.rain) }
            "Light Snow","Moderate Snow","Heavy Snow","Blizzard","Snow"->{ binding.root.setBackgroundResource(R.drawable.snow_background); binding.lottieAnimationView.setAnimation(R.raw.snow) }
            else->{ binding.root.setBackgroundResource(R.drawable.sunny_background); binding.lottieAnimationView.setAnimation(R.raw.sun) }
        }
        binding.lottieAnimationView.playAnimation()
    }

    fun dayName(timeStamp:Long):String{
        val sdf=SimpleDateFormat("EEEE",Locale.getDefault())
        return sdf.format(Date())
    }

    private fun date():String{
        val sdf=SimpleDateFormat("dd MMM yyyy",Locale.getDefault())
        return sdf.format(Date())
    }

    private fun time(timeStamp:Long):String{
        val sdf=SimpleDateFormat("HH:mm",Locale.getDefault())
        return sdf.format(Date(timeStamp*1000))
    }

    override fun onDestroy(){
        super.onDestroy()
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }
}