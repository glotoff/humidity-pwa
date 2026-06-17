import './style.css';

// App State
const state = {
  isLoading: false,
  isRefreshing: false,
  latitude: 37.7749, // Default to San Francisco
  longitude: -122.4194,
  locationName: 'San Francisco',
  countryName: 'United States',
  humidity: 0,
  temperature: 0,
  weatherCode: 0,
  useFahrenheit: false,
  errorMessage: null,
  isSearchOpen: false,
  targetHumidity: 0,  // For gauge sweep animation
  currentAnimatedHumidity: 0 // For gauge sweep animation
};

// Weather codes map to descriptions
const WEATHER_CODES = {
  0: 'Clear Sky',
  1: 'Partly Cloudy',
  2: 'Partly Cloudy',
  3: 'Partly Cloudy',
  45: 'Foggy',
  48: 'Foggy',
  51: 'Drizzle',
  53: 'Drizzle',
  55: 'Drizzle',
  56: 'Freezing Drizzle',
  57: 'Freezing Drizzle',
  61: 'Rainy',
  63: 'Rainy',
  65: 'Rainy',
  66: 'Freezing Rain',
  67: 'Freezing Rain',
  71: 'Snowy',
  73: 'Snowy',
  75: 'Snowy',
  77: 'Snow Grains',
  80: 'Rain Showers',
  81: 'Rain Showers',
  82: 'Rain Showers',
  85: 'Snow Showers',
  86: 'Snow Showers',
  95: 'Thunderstorm',
  96: 'Thunderstorm',
  99: 'Thunderstorm'
};

// DOM Elements
let elLocationName, elCountryName, elHumidityVal, elComfortBadge, elSourceStatus;
let elTempValue, elTempUnitLabel, elDewPointValue, elAbsHumidityValue;
let elWeatherDesc, elCoordinates, elBtnRefresh, elRefreshSpinner, elBtnSearchToggle;
let elSearchPanel, elSearchForm, elSearchInput, elLoadingOverlay, elErrorToast, elErrorMessage, elBtnErrorClose;
let canvas, ctx;

// Initialize App
window.addEventListener('DOMContentLoaded', () => {
  cacheDOMElements();
  setupEventListeners();
  initCanvas();
  
  // Try to request user location, fall back to default
  requestUserLocation();

  // Register Service Worker for PWA offline support
  if ('serviceWorker' in navigator) {
    navigator.serviceWorker.register('./sw.js')
      .then(reg => console.log('Service Worker registered successfully:', reg.scope))
      .catch(err => console.error('Service Worker registration failed:', err));
  }
});

function cacheDOMElements() {
  elLocationName = document.getElementById('location-name');
  elCountryName = document.getElementById('country-name');
  elHumidityVal = document.getElementById('humidity-value');
  elComfortBadge = document.getElementById('comfort-badge');
  elSourceStatus = document.getElementById('source-status-text');
  
  elTempValue = document.getElementById('temp-value');
  elTempUnitLabel = document.getElementById('temp-unit-label');
  elDewPointValue = document.getElementById('dew-point-value');
  elAbsHumidityValue = document.getElementById('abs-humidity-value');
  
  elWeatherDesc = document.getElementById('weather-description');
  elCoordinates = document.getElementById('coordinates-value');
  
  elBtnRefresh = document.getElementById('btn-refresh');
  elRefreshSpinner = document.getElementById('refresh-spinner');
  elBtnSearchToggle = document.getElementById('btn-search-toggle');
  elSearchPanel = document.getElementById('search-panel');
  elSearchForm = document.getElementById('search-form');
  elSearchInput = document.getElementById('search-input');
  
  elLoadingOverlay = document.getElementById('loading-overlay');
  elErrorToast = document.getElementById('error-toast');
  elErrorMessage = document.getElementById('error-message');
  elBtnErrorClose = document.getElementById('btn-error-close');
  
  canvas = document.getElementById('humidity-gauge');
  ctx = canvas.getContext('2d');
}

function setupEventListeners() {
  elBtnRefresh.addEventListener('click', refreshData);
  
  elBtnSearchToggle.addEventListener('click', () => {
    state.isSearchOpen = !state.isSearchOpen;
    if (state.isSearchOpen) {
      elSearchPanel.classList.remove('hidden');
      elSearchInput.focus();
      elBtnSearchToggle.style.backgroundColor = 'rgba(45, 212, 191, 0.2)';
    } else {
      elSearchPanel.classList.add('hidden');
      elSearchInput.value = '';
      elBtnSearchToggle.style.backgroundColor = '';
    }
  });

  elSearchForm.addEventListener('submit', (e) => {
    e.preventDefault();
    const query = elSearchInput.value.trim();
    if (query) {
      searchCity(query);
    }
  });

  document.getElementById('card-temp').addEventListener('click', () => {
    state.useFahrenheit = !state.useFahrenheit;
    renderDetails();
  });

  elBtnErrorClose.addEventListener('click', () => {
    elErrorToast.classList.add('hidden');
  });
}

// Canvas Gauge Setup & Loop
function initCanvas() {
  // Setup High DPI Canvas
  const dpr = window.devicePixelRatio || 1;
  const rect = canvas.getBoundingClientRect();
  canvas.width = rect.width * dpr;
  canvas.height = rect.height * dpr;
  ctx.scale(dpr, dpr);
  
  // Start drawing loop
  drawGaugeLoop();
}

function drawGaugeLoop() {
  // Animate humidity value sweep
  const diff = state.targetHumidity - state.currentAnimatedHumidity;
  if (Math.abs(diff) > 0.1) {
    state.currentAnimatedHumidity += diff * 0.08; // easing
  } else {
    state.currentAnimatedHumidity = state.targetHumidity;
  }

  drawGauge(state.currentAnimatedHumidity);
  requestAnimationFrame(drawGaugeLoop);
}

function drawGauge(humidity) {
  const w = canvas.width / (window.devicePixelRatio || 1);
  const h = canvas.height / (window.devicePixelRatio || 1);
  const center = { x: w / 2, y: h / 2 };
  const radius = Math.min(w, h) / 2 - 14;
  const strokeWidth = 14;

  ctx.clearRect(0, 0, w, h);

  // 1. Draw Background Track (Full Circle)
  ctx.beginPath();
  ctx.arc(center.x, center.y, radius, 0, Math.PI * 2);
  ctx.strokeStyle = 'rgba(255, 255, 255, 0.05)';
  ctx.lineWidth = strokeWidth;
  ctx.stroke();

  // 2. Determine Color Gradient Based on Humidity
  let color = '#2dd4bf'; // teal (comfort)
  if (humidity < 30) {
    color = '#fb923c'; // orange (dry)
  } else if (humidity > 60) {
    color = '#3b82f6'; // blue (humid)
  }

  // 3. Draw Sweeping Progress Arc
  const startAngle = -Math.PI / 2; // top center
  const sweepAngle = (humidity / 100) * (Math.PI * 2);
  const endAngle = startAngle + sweepAngle;

  if (humidity > 0) {
    ctx.beginPath();
    ctx.arc(center.x, center.y, radius, startAngle, endAngle);
    ctx.strokeStyle = color;
    ctx.lineWidth = strokeWidth;
    ctx.lineCap = 'round';
    ctx.stroke();
  }
}

// Fetch Logic
async function requestUserLocation() {
  setLoading(true);
  
  if (!navigator.geolocation) {
    showError("Geolocation is not supported by your browser. Using default location.");
    await fetchWeatherData(state.latitude, state.longitude, "San Francisco (Default)", "United States");
    setLoading(false);
    return;
  }

  navigator.geolocation.getCurrentPosition(
    async (position) => {
      state.latitude = position.coords.latitude;
      state.longitude = position.coords.longitude;
      await fetchWeatherData(state.latitude, state.longitude, "Current Location", "");
      setLoading(false);
    },
    async (error) => {
      console.warn("Location error:", error);
      showError("Could not access your location. Using default location.");
      await fetchWeatherData(state.latitude, state.longitude, "San Francisco (Default)", "United States");
      setLoading(false);
    },
    { timeout: 8000, enableHighAccuracy: true }
  );
}

async function fetchWeatherData(lat, lon, locationLabel, countryLabel) {
  try {
    const response = await fetch(`https://api.open-meteo.com/v1/forecast?latitude=${lat}&longitude=${lon}&current=relative_humidity_2m,temperature_2m,weather_code`);
    if (!response.ok) throw new Error("Weather service returned error");
    
    const data = await response.json();
    const current = data.current;
    
    state.humidity = current.relative_humidity_2m;
    state.temperature = current.temperature_2m;
    state.weatherCode = current.weather_code;
    state.locationName = locationLabel;
    state.countryName = countryLabel;
    state.latitude = lat;
    state.longitude = lon;
    state.targetHumidity = state.humidity;
    
    renderUI();
  } catch (err) {
    showError(`Failed to fetch weather: ${err.message}`);
  }
}

async function searchCity(query) {
  setLoading(true);
  try {
    const encoded = encodeURIComponent(query);
    const response = await fetch(`https://geocoding-api.open-meteo.com/v1/search?name=${encoded}&count=1&language=en&format=json`);
    if (!response.ok) throw new Error("Search service returned error");
    
    const data = await response.json();
    if (!data.results || data.results.length === 0) {
      throw new Error(`Location "${query}" not found.`);
    }
    
    const city = data.results[0];
    const country = city.country || "";
    
    await fetchWeatherData(city.latitude, city.longitude, city.name, country);
    
    // Close search panel
    state.isSearchOpen = false;
    elSearchPanel.classList.add('hidden');
    elSearchInput.value = '';
    elBtnSearchToggle.style.backgroundColor = '';
  } catch (err) {
    showError(err.message);
  } finally {
    setLoading(false);
  }
}

async function refreshData() {
  if (state.isRefreshing) return;
  
  state.isRefreshing = true;
  elRefreshSpinner.classList.remove('hidden');
  // Hide standard refresh icon path
  elBtnRefresh.querySelector('.icon-refresh').style.display = 'none';
  
  try {
    await fetchWeatherData(state.latitude, state.longitude, state.locationName.replace(" (Default)", ""), state.countryName);
  } finally {
    state.isRefreshing = false;
    elRefreshSpinner.classList.add('hidden');
    elBtnRefresh.querySelector('.icon-refresh').style.display = 'block';
  }
}

// Scientific Formulas
function getDewPoint(temp, rh) {
  const m = 17.62;
  const tn = 243.12;
  const alpha = Math.log(rh / 100) + (m * temp) / (tn + temp);
  return (tn * alpha) / (m - alpha);
}

function getAbsoluteHumidity(temp, rh) {
  const m = 17.62;
  const tn = 243.12;
  const a = 6.112; // hPa
  const expTerm = Math.exp((m * temp) / (tn + temp));
  const vaporPressure = (rh / 100) * a * expTerm;
  return (vaporPressure * 216.7) / (273.15 + temp);
}

function getComfortRating(rh) {
  if (rh < 30) return { label: "Very Dry", class: "dry" };
  if (rh <= 60) return { label: "Comfortable", class: "comfort" };
  if (rh <= 75) return { label: "Sticky / Humid", class: "humid" };
  return { label: "Extremely Damp", class: "humid" };
}

// UI Updating
function renderUI() {
  elLocationName.textContent = state.locationName;
  elCountryName.textContent = state.countryName;
  elCountryName.style.display = state.countryName ? 'block' : 'none';
  
  // Value numbers
  elHumidityVal.textContent = `${Math.round(state.humidity)}%`;
  
  // Comfort label rating
  const comfort = getComfortRating(state.humidity);
  elComfortBadge.textContent = comfort.label;
  elComfortBadge.className = `comfort-badge ${comfort.class}`;
  
  renderDetails();
}

function renderDetails() {
  // Temp Details C/F
  const tempC = state.temperature;
  const tempVal = state.useFahrenheit ? (tempC * 9/5) + 32 : tempC;
  const tempUnit = state.useFahrenheit ? '°F' : '°C';
  elTempValue.textContent = `${tempVal.toFixed(1)}${tempUnit}`;
  elTempUnitLabel.textContent = state.useFahrenheit ? 'Fahrenheit' : 'Celsius';
  
  // Dew Point Details
  const dpC = getDewPoint(tempC, state.humidity);
  const dpVal = state.useFahrenheit ? (dpC * 9/5) + 32 : dpC;
  elDewPointValue.textContent = `${dpVal.toFixed(1)}${tempUnit}`;
  
  // Absolute Humidity Details
  const absHum = getAbsoluteHumidity(tempC, state.humidity);
  elAbsHumidityValue.textContent = `${absHum.toFixed(1)} g/m³`;
  
  // Description and coordinates
  const desc = WEATHER_CODES[state.weatherCode] || 'Unknown Conditions';
  elWeatherDesc.textContent = desc;
  elCoordinates.textContent = `${state.latitude.toFixed(3)}°, ${state.longitude.toFixed(3)}°`;
}

function setLoading(loading) {
  state.isLoading = loading;
  if (loading) {
    elLoadingOverlay.classList.remove('hidden');
  } else {
    elLoadingOverlay.classList.add('hidden');
  }
}

function showError(message) {
  elErrorMessage.textContent = message;
  elErrorToast.classList.remove('hidden');
  
  // Auto-hide after 5 seconds
  setTimeout(() => {
    elErrorToast.classList.add('hidden');
  }, 5000);
}
