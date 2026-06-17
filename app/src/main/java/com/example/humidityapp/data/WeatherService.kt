package com.example.humidityapp.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class WeatherInfo(
    val humidity: Float,
    val temperature: Float,
    val locationName: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
    val weatherCode: Int,
    val isFromSensor: Boolean = false
)

object WeatherService {

    // Helper to perform HTTP GET request and return the JSON string response
    private suspend fun makeGetRequest(urlString: String): String = withContext(Dispatchers.IO) {
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 10000
        connection.readTimeout = 10000
        
        val responseCode = connection.responseCode
        if (responseCode == HttpURLConnection.HTTP_OK) {
            val reader = BufferedReader(InputStreamReader(connection.inputStream))
            val response = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                response.append(line)
            }
            reader.close()
            connection.disconnect()
            response.toString()
        } else {
            connection.disconnect()
            throw Exception("HTTP Error code: $responseCode")
        }
    }

    // Fetch weather info by coordinate
    suspend fun fetchWeather(latitude: Double, longitude: Double, locationLabel: String = "Current Location", countryLabel: String = ""): WeatherInfo {
        val url = "https://api.open-meteo.com/v1/forecast?latitude=$latitude&longitude=$longitude&current=relative_humidity_2m,temperature_2m,weather_code"
        val responseJson = makeGetRequest(url)
        val json = JSONObject(responseJson)
        val current = json.getJSONObject("current")
        
        val humidity = current.getDouble("relative_humidity_2m").toFloat()
        val temperature = current.getDouble("temperature_2m").toFloat()
        val weatherCode = current.getInt("weather_code")
        
        return WeatherInfo(
            humidity = humidity,
            temperature = temperature,
            locationName = locationLabel,
            country = countryLabel,
            latitude = latitude,
            longitude = longitude,
            weatherCode = weatherCode,
            isFromSensor = false
        )
    }

    // Search coordinates by city name
    suspend fun searchLocation(query: String): WeatherInfo {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val searchUrl = "https://geocoding-api.open-meteo.com/v1/search?name=$encodedQuery&count=1&language=en&format=json"
        
        val searchResponse = makeGetRequest(searchUrl)
        val searchJson = JSONObject(searchResponse)
        
        if (!searchJson.has("results")) {
            throw Exception("Location not found")
        }
        
        val resultsArray = searchJson.getJSONArray("results")
        if (resultsArray.length() == 0) {
            throw Exception("Location not found")
        }
        
        val firstResult = resultsArray.getJSONObject(0)
        val name = firstResult.getString("name")
        val country = if (firstResult.has("country")) firstResult.getString("country") else ""
        val latitude = firstResult.getDouble("latitude")
        val longitude = firstResult.getDouble("longitude")
        
        // Now fetch weather for these coordinates
        return fetchWeather(latitude, longitude, name, country)
    }
}
