# Sahu Sales Solution - Gradle Wrapper Setup
# Run this in PowerShell if you don't have Gradle installed.
# It downloads the gradle-wrapper.jar directly.

$androidDir = "$PSScriptRoot\android"
$wrapperDir = "$androidDir\gradle\wrapper"

Write-Host "Setting up Gradle wrapper for Sahu Sales Android project..." -ForegroundColor Cyan

# Create wrapper directory
if (-not (Test-Path $wrapperDir)) {
    New-Item -ItemType Directory -Path $wrapperDir -Force | Out-Null
    Write-Host "Created: $wrapperDir"
}

# Write gradle-wrapper.properties
$propsContent = @"
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.7-bin.zip
networkTimeout=10000
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
"@
Set-Content -Path "$wrapperDir\gradle-wrapper.properties" -Value $propsContent
Write-Host "Written: gradle-wrapper.properties" -ForegroundColor Green

# Download gradle-wrapper.jar
$jarUrl = "https://github.com/gradle/gradle/raw/v8.7.0/gradle/wrapper/gradle-wrapper.jar"
$jarPath = "$wrapperDir\gradle-wrapper.jar"

Write-Host "Downloading gradle-wrapper.jar from GitHub..." -ForegroundColor Yellow
try {
    [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
    Invoke-WebRequest -Uri $jarUrl -OutFile $jarPath -UseBasicParsing
    Write-Host "Downloaded: gradle-wrapper.jar" -ForegroundColor Green
} catch {
    Write-Host "Download failed: $_" -ForegroundColor Red
    Write-Host "Alternative: Open android/ in Android Studio to auto-generate wrapper." -ForegroundColor Yellow
    exit 1
}

# Create local.properties if needed
$sdkPath = "$env:LOCALAPPDATA\Android\Sdk"
if (Test-Path $sdkPath) {
    $escaped = $sdkPath -replace '\\', '\\'
    Set-Content -Path "$androidDir\local.properties" -Value "sdk.dir=$($escaped -replace ':', '\:')"
    Write-Host "Written: local.properties (sdk.dir=$sdkPath)" -ForegroundColor Green
} else {
    Write-Host "WARNING: Android SDK not found at $sdkPath" -ForegroundColor Yellow
    Write-Host "Install Android Studio, then re-run or edit android\local.properties manually." -ForegroundColor Yellow
}

Write-Host ""
Write-Host "Setup complete! Now build the APK:" -ForegroundColor Cyan
Write-Host "  cd android"
Write-Host "  .\gradlew.bat assembleDebug"
Write-Host ""
Write-Host "APK will be at: android\app\build\outputs\apk\debug\app-debug.apk"
