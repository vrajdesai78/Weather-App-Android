package com.example.weather_android_app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.weather_android_app.databinding.ActivityMainBinding
import com.squareup.picasso.Picasso
import org.json.JSONException
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var weatherModel: ArrayList<WeatherModel>
    private lateinit var weatherAdapter: WeatherAdapter
    private lateinit var locationManager: LocationManager
    private val PERMISSION_CODE = 1
    private lateinit var cityName: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        weatherModel = arrayListOf()
        weatherAdapter = WeatherAdapter(this, weatherModel)

        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                PERMISSION_CODE
            )
        }

        val location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        cityName = getCityName(location!!.longitude, location.latitude)
        getWeatherInfo(cityName)
        binding.cityName.text = cityName

        binding.searchBtn.setOnClickListener {
            val city = binding.cityInputEditText.text
            if (city!!.isEmpty()) {
                Toast.makeText(this, "Please enter city Name", Toast.LENGTH_SHORT).show()
            } else {
                binding.cityName.text = city
                getWeatherInfo(city.toString())
            }
        }
        binding.weatherRecyclerView.adapter = weatherAdapter
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == PERMISSION_CODE) {
            if(grantResults.isEmpty() && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Please provide the permissions", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun getWeatherInfo(cityName: String) {
        val applicationInfo = applicationContext.packageManager.getApplicationInfo(
            applicationContext.packageName,
            PackageManager.GET_META_DATA
        )
        val apiKey = applicationInfo.metaData["keyValue"].toString()
        val url =
            "https://api.weatherapi.com/v1/forecast.json?key=$apiKey&q=$cityName&days=1&aqi=yes&alerts=yes"
        Log.d("location", url)
        val jsonObjectRequest = JsonObjectRequest(Request.Method.GET, url, null,
            { response ->
                weatherModel.clear()
                try {
                    val temperature = response.getJSONObject("current").getString("temp_c")
                    binding.temperature.text =
                        resources.getString(R.string.temperature_celsius, temperature)
                    val isDay = response.getJSONObject("current").getInt("is_day")
                    if (isDay == 0) {
                        Picasso.get()
                            .load("https://images.unsplash.com/photo-1507400492013-162706c8c05e?ixlib=rb-1.2.1&ixid=MnwxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8&auto=format&fit=crop&w=359&q=80")
                            .into(binding.backgroundImage)
                    } else {
                        Picasso.get()
                            .load("https://images.unsplash.com/photo-1514241516423-6c0a5e031aa2?ixlib=rb-1.2.1&ixid=MnwxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8&auto=format&fit=crop&w=387&q=80")
                            .into(binding.backgroundImage)
                    }
                    val condition = response.getJSONObject("current").getJSONObject("condition")
                        .getString("text")
                    binding.weatherCondition.text = condition
                    val conditionIcon = response.getJSONObject("current").getJSONObject("condition")
                        .getString("icon")
                    Picasso.get().load("https:".plus(conditionIcon)).into(binding.weatherIcon)
                    val windSpeed = response.getJSONObject("current").getString("wind_kph")
                    binding.windSpeed.text = resources.getString(R.string.wind_speed, windSpeed)
                    val humidity = response.getJSONObject("current").getString("humidity")
                    binding.humidity.text = resources.getString(R.string.humidity, humidity)
                    val cloud = response.getJSONObject("current").getString("cloud")
                    binding.cloudy.text = resources.getString(R.string.cloudy, cloud)
                    val feelsLike = response.getJSONObject("current").getString("feelslike_c")
                    binding.realFeel.text = resources.getString(R.string.real_feel, feelsLike)

                    val forecastObj = response.getJSONObject("forecast")
                    val forecast = forecastObj.getJSONArray("forecastday").getJSONObject(0)
                    val hourArray = forecast.getJSONArray("hour")
                    for(i in 0 until hourArray.length()) {
                        val hourObj = hourArray.getJSONObject(i)
                        val time = hourObj.getString("time")
                        val temperature = hourObj.getString("temp_c")
                        val icon = hourObj.getJSONObject("condition").getString("icon")
                        val wind = hourObj.getString("wind_kph")
                        weatherModel.add(WeatherModel(time, temperature, icon, wind))
                    }
                    weatherAdapter.notifyDataSetChanged()
                } catch(e: JSONException) {
                    e.printStackTrace()
                }
            }
        ) {
            Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
        }
        val requestQueue = Volley.newRequestQueue(this)
        requestQueue.add(jsonObjectRequest)
    }

    private fun getCityName(longitude: Double, latitude: Double): String {
        var cityName = "Not Found"
        val geocoder = Geocoder(baseContext, Locale.getDefault());
        try {
            val addresses: List<Address> = geocoder.getFromLocation(latitude, longitude, 10)

            for (address in addresses) {
                val city = address.locality
                if (city != null && city.isNotBlank()) {
                    cityName = city
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return cityName
    }
}