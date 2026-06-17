package com.example.humidityapp.ui.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavKey
import java.util.Locale

@Composable
fun MainScreen(
    onItemClick: (NavKey) -> Unit,
    viewModel: MainScreenViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsState()

    // Base background with a premium deep-indigo-to-black gradient
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A), // Slate 900
                        Color(0xFF1E1B4B), // Indigo 950
                        Color(0xFF020617)  // Slate 950
                    )
                )
            )
            .then(modifier)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Header Row
            HeaderRow(
                locationName = state.locationName,
                countryName = state.country,
                isRefreshing = state.isRefreshing,
                isSearchOpen = state.isSearchOpen,
                onSearchToggle = { viewModel.toggleSearchOpen() },
                onRefresh = { viewModel.refreshWeather() }
            )

            // Search Panel
            AnimatedVisibility(visible = state.isSearchOpen) {
                SearchPanel(
                    query = state.searchQuery,
                    onQueryChange = { viewModel.setSearchQuery(it) },
                    onSearchSubmit = { viewModel.searchCity() },
                    onClose = { viewModel.toggleSearchOpen() }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Glassmorphic Main Card
            GlassmorphicCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val currentHum = state.currentHumidity ?: 0f

                    // Animated gauge progress
                    val animatedHumProgress by animateFloatAsState(
                        targetValue = currentHum,
                        animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
                        label = "HumidityProgress"
                    )

                    HumidityGauge(humidity = animatedHumProgress, displayVal = currentHum)

                    Spacer(modifier = Modifier.height(20.dp))

                    // Comfort Pill Status
                    ComfortPill(comfortLevel = state.comfortLevel, humidity = currentHum)

                    Spacer(modifier = Modifier.height(16.dp))

                    // Sensor / Weather Source toggle
                    if (state.hasHardwareSensor) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0x0CFFFFFF))
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "Use Ambient Hardware Sensor",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Switch(
                                checked = state.isUsingSensor,
                                onCheckedChange = { viewModel.toggleSensorUsage(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFF2DD4BF),
                                    checkedTrackColor = Color(0xFF14B8A6).copy(alpha = 0.5f)
                                )
                            )
                        }
                    } else {
                        Text(
                            text = "Using weather station data. Ambient hardware sensor is not available on this device.",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Details Section Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                Text(
                    text = "Atmospheric Details",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Detail Grid Items (2x2 Layout using Columns and Rows)
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    DetailCard(
                        title = "Temperature",
                        value = formatTemperature(state.temperature, state.useFahrenheit),
                        subtitle = if (state.useFahrenheit) "Fahrenheit" else "Celsius",
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.toggleFahrenheit() }
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    DetailCard(
                        title = "Dew Point",
                        value = formatTemperature(state.dewPoint, state.useFahrenheit),
                        subtitle = "Condensation point",
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    DetailCard(
                        title = "Absolute Humidity",
                        value = formatAbsoluteHumidity(state.absoluteHumidity),
                        subtitle = "Water vapor density",
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    DetailCard(
                        title = "Location Source",
                        value = if (state.isUsingSensor) "Hardware Sensor" else state.weatherDescription,
                        subtitle = if (state.latitude != null && state.longitude != null) {
                            String.format(Locale.US, "%.3f, %.3f", state.latitude, state.longitude)
                        } else {
                            "Offline"
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        // Loading Overlay
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable(enabled = false) {},
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = Color(0xFF2DD4BF),
                    strokeWidth = 4.dp
                )
            }
        }

        // Error message popup
        state.errorMessage?.let { error ->
            Snackbar(
                action = {
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text("Dismiss", color = Color(0xFF2DD4BF))
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                containerColor = Color(0xFF7F1D1D), // Dark Red
                contentColor = Color.White
            ) {
                Text(error)
            }
        }
    }
}

@Composable
fun HeaderRow(
    locationName: String,
    countryName: String,
    isRefreshing: Boolean,
    isSearchOpen: Boolean,
    onSearchToggle: () -> Unit,
    onRefresh: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Location",
                    tint = Color(0xFF2DD4BF),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = locationName,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            if (countryName.isNotEmpty()) {
                Text(
                    text = countryName,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(start = 22.dp)
                )
            }
        }

        Row {
            IconButton(
                onClick = onRefresh,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0x0CFFFFFF))
            ) {
                if (isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = Color.White
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = onSearchToggle,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (isSearchOpen) Color(0x332DD4BF) else Color(0x0CFFFFFF))
            ) {
                Icon(
                    imageVector = if (isSearchOpen) Icons.Default.Close else Icons.Default.Search,
                    contentDescription = "Search",
                    tint = if (isSearchOpen) Color(0xFF2DD4BF) else Color.White
                )
            }
        }
    }
}

@Composable
fun SearchPanel(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearchSubmit: () -> Unit,
    onClose: () -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x0CFFFFFF))
            .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("Search city (e.g. London)", color = Color.White.copy(alpha = 0.4f)) },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                cursorColor = Color(0xFF2DD4BF),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                onSearchSubmit()
                keyboardController?.hide()
            }),
            singleLine = true
        )
        IconButton(onClick = {
            onSearchSubmit()
            keyboardController?.hide()
        }) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search Submit",
                tint = Color(0xFF2DD4BF)
            )
        }
    }
}

@Composable
fun GlassmorphicCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(28.dp))
            .background(Color(0x0AFFFFFF)) // Translucent background
            .border(1.dp, Color(0x1F7F7F7F), RoundedCornerShape(28.dp))
            .border(
                1.dp,
                Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.15f),
                        Color.White.copy(alpha = 0.02f)
                    )
                ),
                RoundedCornerShape(28.dp)
            )
    ) {
        content()
    }
}

@Composable
fun HumidityGauge(
    humidity: Float,
    displayVal: Float
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(240.dp)
    ) {
        // Colored status glow in background
        val glowColor = when {
            displayVal < 30f -> Color(0x1AFBBF24)
            displayVal <= 60f -> Color(0x1A2DD4BF)
            else -> Color(0x1A3B82F6)
        }
        Box(
            modifier = Modifier
                .size(190.dp)
                .background(glowColor, shape = CircleShape)
        )

        Canvas(modifier = Modifier.size(210.dp)) {
            val strokeWidth = 14.dp.toPx()
            val size = this.size
            val center = Offset(size.width / 2, size.height / 2)
            val radius = (size.width - strokeWidth) / 2

            // Track Background
            drawCircle(
                color = Color.White.copy(alpha = 0.05f),
                radius = radius,
                center = center,
                style = Stroke(width = strokeWidth)
            )

            // Dynamic progress arc color transition
            val sweepColor = when {
                humidity < 30f -> Color(0xFFFB923C)  // Orange (Dry)
                humidity <= 60f -> Color(0xFF2DD4BF) // Teal (Comfortable)
                else -> Color(0xFF3B82F6)            // Blue (Humid)
            }

            drawArc(
                color = sweepColor,
                startAngle = -90f,
                sweepAngle = (humidity / 100f) * 360f,
                useCenter = false,
                topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                size = Size(size.width - strokeWidth, size.height - strokeWidth),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        // Inner stats display
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = String.format(Locale.US, "%.0f%%", displayVal),
                color = Color.White,
                fontSize = 52.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-1.5).sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Relative Humidity",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
fun ComfortPill(
    comfortLevel: String,
    humidity: Float
) {
    val (backgroundColor, textColor) = when {
        humidity < 30f -> Pair(Color(0x23FB923C), Color(0xFFFDBA74))  // Amber/Orange
        humidity <= 60f -> Pair(Color(0x232DD4BF), Color(0xFF99F6E4)) // Teal
        else -> Pair(Color(0x233B82F6), Color(0xFF93C5FD))            // Blue
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50.dp))
            .background(backgroundColor)
            .border(1.dp, textColor.copy(alpha = 0.2f), RoundedCornerShape(50.dp))
            .padding(horizontal = 18.dp, vertical = 6.dp)
    ) {
        Text(
            text = comfortLevel,
            color = textColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
fun DetailCard(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val clickModifier = if (onClick != null) {
        Modifier.clickable { onClick() }
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0x08FFFFFF))
            .border(1.dp, Color(0x0DFFFFFF), RoundedCornerShape(20.dp))
            .then(clickModifier)
            .padding(16.dp)
    ) {
        Column {
            Text(
                text = title,
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 11.sp
            )
        }
    }
}

private fun formatTemperature(temp: Float?, fahrenheit: Boolean): String {
    if (temp == null) return "--"
    val displayVal = if (fahrenheit) (temp * 9f / 5f) + 32f else temp
    return String.format(Locale.US, "%.1f°%s", displayVal, if (fahrenheit) "F" else "C")
}

private fun formatAbsoluteHumidity(value: Float?): String {
    if (value == null) return "--"
    return String.format(Locale.US, "%.1f g/m³", value)
}
