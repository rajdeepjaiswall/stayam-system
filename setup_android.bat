@echo off
echo ============================================
echo  SAHU SALES - ANDROID SETUP
echo ============================================
echo.

cd /d "%~dp0android"

echo [1] Detecting Android SDK...
set ANDROID_SDK=
if exist "%LOCALAPPDATA%\Android\Sdk" (
    set ANDROID_SDK=%LOCALAPPDATA%\Android\Sdk
    echo Found at: %LOCALAPPDATA%\Android\Sdk
) else if defined ANDROID_HOME (
    set ANDROID_SDK=%ANDROID_HOME%
    echo Found via ANDROID_HOME: %ANDROID_HOME%
) else if defined ANDROID_SDK_ROOT (
    set ANDROID_SDK=%ANDROID_SDK_ROOT%
    echo Found via ANDROID_SDK_ROOT: %ANDROID_SDK_ROOT%
) else (
    echo ERROR: Android SDK not found.
    echo Please install Android Studio from https://developer.android.com/studio
    echo Then re-run this script.
    pause
    exit /b 1
)

echo.
echo [2] Writing local.properties...
echo sdk.dir=%ANDROID_SDK:\=\\% > local.properties
echo Written: sdk.dir=%ANDROID_SDK%

echo.
echo [3] Generating Gradle wrapper...
where gradle >NUL 2>&1
if %ERRORLEVEL% equ 0 (
    echo Gradle found! Generating wrapper...
    call gradle wrapper --gradle-version=8.7 --distribution-type=bin
    if %ERRORLEVEL% neq 0 (
        echo WARNING: gradle wrapper failed. Will try alternative...
        goto :alt_wrapper
    )
    echo Gradle wrapper generated successfully!
    goto :build
) else (
    echo Gradle not found in PATH.
    :alt_wrapper
    echo Checking if gradlew already exists from previous setup...
    if exist gradlew.bat (
        echo gradlew.bat found, skipping generation.
        goto :build
    )
    echo.
    echo The Gradle wrapper must be generated. Options:
    echo A) Install Gradle: https://gradle.org/install/ then re-run this script
    echo B) Open this project in Android Studio - it will auto-generate the wrapper
    echo    File -^> Open -^> Select: %~dp0android
    echo.
    pause
    exit /b 1
)

:build
echo.
echo [4] Building debug APK...
if not exist gradlew.bat (
    echo ERROR: gradlew.bat not found. Run Step 3 again.
    pause
    exit /b 1
)

call gradlew.bat assembleDebug --no-daemon --stacktrace
if %ERRORLEVEL% neq 0 (
    echo.
    echo BUILD FAILED. Common fixes:
    echo - Make sure JDK 17 is installed: java -version
    echo - Make sure Android SDK is installed via Android Studio
    echo - Check the error output above
    pause
    exit /b 1
)

echo.
echo ============================================
echo  BUILD SUCCESSFUL!
echo ============================================
echo.
echo APK location:
echo   %~dp0android\app\build\outputs\apk\debug\app-debug.apk
echo.
echo Install on device via USB (adb must be in PATH):
echo   adb install app\build\outputs\apk\debug\app-debug.apk
echo.
echo Or copy the APK to your phone and install manually.
echo.
pause
