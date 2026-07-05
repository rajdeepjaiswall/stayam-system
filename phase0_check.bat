@echo off
echo ============================================
echo   SAHU SALES SOLUTION - ENVIRONMENT CHECK
echo ============================================
echo.
echo === Node.js ===
node --version 2>NUL || echo NOT FOUND - Install from https://nodejs.org (need v18+)
echo.
echo === NPM ===
npm --version 2>NUL || echo NOT FOUND
echo.
echo === Vercel CLI ===
vercel --version 2>NUL || echo NOT FOUND - Will install automatically
echo.
echo === Java ===
java -version 2>&1 || echo NOT FOUND - Install JDK 17 from https://adoptium.net
echo.
echo === Javac ===
javac -version 2>&1 || echo NOT FOUND
echo.
echo === Git ===
git --version 2>NUL || echo NOT FOUND - Install from https://git-scm.com
echo.
echo === ANDROID_HOME ===
if "%ANDROID_HOME%"=="" (echo NOT SET) else (echo %ANDROID_HOME%)
echo.
echo === ANDROID_SDK_ROOT ===
if "%ANDROID_SDK_ROOT%"=="" (echo NOT SET) else (echo %ANDROID_SDK_ROOT%)
echo.
echo === Android SDK in default path ===
if exist "%LOCALAPPDATA%\Android\Sdk" (
  echo FOUND at %LOCALAPPDATA%\Android\Sdk
) else (
  echo NOT FOUND at %LOCALAPPDATA%\Android\Sdk
  echo Install Android Studio from https://developer.android.com/studio
)
echo.
echo ============================================
echo   Results saved to phase0_results.txt
echo ============================================
pause
