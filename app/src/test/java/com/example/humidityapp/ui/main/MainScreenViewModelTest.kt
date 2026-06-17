package com.example.humidityapp.ui.main

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MainScreenViewModelTest {

    @Test
    fun testDefaultUiState() {
        val state = HumidityUiState(
            isLoading = false,
            apiHumidity = 50f,
            temperature = 20f,
            isUsingSensor = false,
            useFahrenheit = false
        )
        assertEquals(50f, state.currentHumidity)
        assertEquals("Comfortable", state.comfortLevel)
        assertEquals(20f, state.temperature)
        
        // Check dew point calculation (T = 20C, RH = 50% -> Dew point should be ~9.3C)
        val dewPoint = state.dewPoint
        assertNotNull(dewPoint)
        assertTrue(dewPoint!! > 9.0f && dewPoint < 9.5f)
        
        // Check absolute humidity calculation (T = 20C, RH = 50% -> Absolute humidity should be ~8.6 g/m^3)
        val absHum = state.absoluteHumidity
        assertNotNull(absHum)
        assertTrue(absHum!! > 8.0f && absHum < 9.0f)
    }

    @Test
    fun testComfortLevelBoundaries() {
        val dryState = HumidityUiState(apiHumidity = 25f)
        assertEquals("Very Dry", dryState.comfortLevel)

        val comfortableState = HumidityUiState(apiHumidity = 50f)
        assertEquals("Comfortable", comfortableState.comfortLevel)

        val stickyState = HumidityUiState(apiHumidity = 70f)
        assertEquals("Sticky / Humid", stickyState.comfortLevel)

        val dampState = HumidityUiState(apiHumidity = 85f)
        assertEquals("Extremely Damp", dampState.comfortLevel)
    }

    @Test
    fun testSensorUsagePriority() {
        val state = HumidityUiState(
            apiHumidity = 60f,
            sensorHumidity = 45f,
            isUsingSensor = true
        )
        assertEquals(45f, state.currentHumidity)
        assertEquals("Comfortable", state.comfortLevel)

        val apiOnlyState = state.copy(isUsingSensor = false)
        assertEquals(60f, apiOnlyState.currentHumidity)
    }
}
