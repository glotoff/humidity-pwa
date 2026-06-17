package com.example.humidityapp.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.humidityapp.data.WeatherInfo
import com.example.humidityapp.data.WeatherService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HumidityUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val locationName: String = "Checking GPS Location...",
    val country: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val sensorHumidity: Float? = null,
    val apiHumidity: Float? = null,
    val temperature: Float? = null,
    val weatherCode: Int? = null,
    val isUsingSensor: Boolean = false,
    val hasHardwareSensor: Boolean = false,
    val useFahrenheit: Boolean = false,
    val errorMessage: String? = null,
    val searchQuery: String = "",
    val isSearchOpen: Boolean = false
) {
    // Computed values
    val currentHumidity: Float?
        get() = if (isUsingSensor) sensorHumidity else apiHumidity

    val weatherDescription: String
        get() = when (weatherCode) {
            0 -> "Clear Sky"
            1, 2, 3 -> "Partly Cloudy"
            45, 48 -> "Foggy"
            51, 53, 55 -> "Drizzle"
            56, 57 -> "Freezing Drizzle"
            61, 63, 65 -> "Rainy"
            66, 67 -> "Freezing Rain"
            71, 73, 75 -> "Snowy"
            77 -> "Snow Grains"
            80, 81, 82 -> "Rain Showers"
            85, 86 -> "Snow Showers"
            95, 96, 99 -> "Thunderstorm"
            else -> "Unknown Weather"
        }

    val comfortLevel: String
        get() {
            val hum = currentHumidity ?: return "Unknown"
            return when {
                hum < 30f -> "Very Dry"
                hum <= 60f -> "Comfortable"
                hum <= 75f -> "Sticky / Humid"
                else -> "Extremely Damp"
            }
        }

    val dewPoint: Float?
        get() {
            val temp = temperature ?: return null
            val hum = currentHumidity ?: return null
            val rh = hum.coerceIn(0.1f, 100f) / 100f
            val m = 17.62f
            val tn = 243.12f
            val alpha = Math.log(rh.toDouble()).toFloat() + (m * temp) / (tn + temp)
            return (tn * alpha) / (m - alpha)
        }

    val absoluteHumidity: Float?
        get() {
            val temp = temperature ?: return null
            val hum = currentHumidity ?: return null
            val rh = hum.coerceIn(0f, 100f) / 100f
            val m = 17.62f
            val tn = 243.12f
            val a = 6.112f // hPa
            val expTerm = Math.exp(((m * temp) / (tn + temp)).toDouble()).toFloat()
            val vaporPressure = rh * a * expTerm
            return (vaporPressure * 216.7f) / (273.15f + temp)
        }
}

class MainScreenViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(HumidityUiState(isLoading = true))
    val uiState: StateFlow<HumidityUiState> = _uiState.asStateFlow()

    // Load initial default weather if location is taking too long or denied
    init {
        loadDefaultLocation()
    }

    private fun loadDefaultLocation() {
        viewModelScope.launch {
            try {
                // Default to San Francisco
                val defaultLat = 37.7749
                val defaultLon = -122.4194
                val weather = WeatherService.fetchWeather(defaultLat, defaultLon, "San Francisco", "United States")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        apiHumidity = weather.humidity,
                        temperature = weather.temperature,
                        weatherCode = weather.weatherCode,
                        latitude = defaultLat,
                        longitude = defaultLon,
                        locationName = "San Francisco (Default)",
                        country = "United States"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to load default weather: ${e.message}"
                    )
                }
            }
        }
    }

    fun updateLocation(latitude: Double, longitude: Double, label: String = "Current Location") {
        _uiState.update { it.copy(latitude = latitude, longitude = longitude) }
        refreshWeather()
    }

    fun setHardwareSensorPresence(present: Boolean) {
        _uiState.update { it.copy(hasHardwareSensor = present) }
    }

    fun updateSensorHumidity(humidity: Float) {
        _uiState.update { it.copy(sensorHumidity = humidity) }
    }

    fun toggleSensorUsage(useSensor: Boolean) {
        _uiState.update { it.copy(isUsingSensor = useSensor) }
    }

    fun toggleFahrenheit() {
        _uiState.update { it.copy(useFahrenheit = !it.useFahrenheit) }
    }

    fun refreshWeather() {
        val lat = _uiState.value.latitude ?: return
        val lon = _uiState.value.longitude ?: return
        val label = _uiState.value.locationName.replace(" (Default)", "")
        val country = _uiState.value.country

        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
            try {
                val weather = WeatherService.fetchWeather(lat, lon, label, country)
                _uiState.update {
                    it.copy(
                        isRefreshing = false,
                        apiHumidity = weather.humidity,
                        temperature = weather.temperature,
                        weatherCode = weather.weatherCode
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isRefreshing = false,
                        errorMessage = "Refresh failed: ${e.message}"
                    )
                }
            }
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun toggleSearchOpen() {
        _uiState.update { it.copy(isSearchOpen = !it.isSearchOpen, searchQuery = "") }
    }

    fun searchCity() {
        val query = _uiState.value.searchQuery
        if (query.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val weather = WeatherService.searchLocation(query)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        apiHumidity = weather.humidity,
                        temperature = weather.temperature,
                        weatherCode = weather.weatherCode,
                        latitude = weather.latitude,
                        longitude = weather.longitude,
                        locationName = weather.locationName,
                        country = weather.country,
                        isSearchOpen = false,
                        searchQuery = ""
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Search failed: ${e.message}"
                    )
                }
            }
        }
    }
    
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
