# PowerShell script to build the Relative Humidity PWA and package it as an APK.
$ErrorActionPreference = "Stop"

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host " Building Humidity PWA and Android APK    " -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan

# 1. Build the PWA Web App
Write-Host "`n[1/4] Building Vite PWA web app..." -ForegroundColor Green
Push-Location "humidity-web-app"

if (-not (Test-Path "node_modules")) {
    Write-Host "node_modules not found. Running 'npm install'..." -ForegroundColor Yellow
    npm install
}

Write-Host "Running Vite production build..." -ForegroundColor Yellow
npm run build

Pop-Location

# 2. Clean and prepare Android Assets folder
Write-Host "`n[2/4] Cleaning and preparing Android assets folder..." -ForegroundColor Green
$assetsDir = "app\src\main\assets"

if (Test-Path $assetsDir) {
    Remove-Item -Path "$assetsDir\*" -Recurse -Force -ErrorAction SilentlyContinue
} else {
    New-Item -ItemType Directory -Path $assetsDir -Force
}

# 3. Copy compiled web assets to Android project
Write-Host "`n[3/4] Copying compiled assets from 'humidity-web-app/dist' to '$assetsDir'..." -ForegroundColor Green
$distDir = Join-Path "humidity-web-app" "dist"

if (-not (Test-Path $distDir)) {
    Write-Error "Error: Vite build directory '$distDir' does not exist."
}

Copy-Item -Path "$distDir\*" -Destination $assetsDir -Recurse -Force

# Verify assets copied
if (-not (Test-Path "$assetsDir\index.html")) {
    Write-Error "Error: index.html not found in assets directory after copying."
}
Write-Host "Web assets successfully copied." -ForegroundColor Yellow

# 4. Compile Android APK using Gradle
Write-Host "`n[4/4] Compiling Android APK with Gradle..." -ForegroundColor Green

if (-not (Test-Path "gradlew.bat")) {
    Write-Error "Error: Gradle Wrapper 'gradlew.bat' not found in project root."
}

Write-Host "Running Gradle assembleDebug..." -ForegroundColor Yellow
& ".\gradlew.bat" clean assembleDebug

# 5. Check Output
$apkPath = "app\build\outputs\apk\debug\app-debug.apk"
if (Test-Path $apkPath) {
    Write-Host "`n==========================================" -ForegroundColor Green
    Write-Host " SUCCESS: APK built successfully!" -ForegroundColor Green
    Write-Host " APK location: $(Resolve-Path $apkPath)" -ForegroundColor Green
    Write-Host "==========================================" -ForegroundColor Green
} else {
    Write-Error "Gradle build finished but output APK was not found at: $apkPath"
}
