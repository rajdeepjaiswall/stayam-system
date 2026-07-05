@echo off
echo Generating Gradle wrapper JAR...
echo This requires Gradle to be installed on your system.
echo Download from: https://gradle.org/releases/ (gradle-8.7-bin.zip)
echo After installing, add it to your PATH, then run this script.
echo.
echo If you have Gradle installed, press any key to continue.
pause

gradle wrapper --gradle-version=8.7 --distribution-type=bin
if %ERRORLEVEL% equ 0 (
    echo.
    echo SUCCESS! Gradle wrapper generated.
    echo You can now run: gradlew.bat assembleDebug
) else (
    echo.
    echo FAILED. Please install Gradle first.
    echo Alternative: Open the android/ folder in Android Studio.
    echo Android Studio will generate the wrapper automatically.
)
pause
