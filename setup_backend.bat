@echo off
echo ============================================
echo  SAHU SALES - BACKEND SETUP
echo ============================================
echo.

cd /d "%~dp0backend"

echo [1/4] Checking Node.js...
node --version >NUL 2>&1 || (echo ERROR: Node.js not found. Install from https://nodejs.org && pause && exit /b 1)
node --version

echo.
echo [2/4] Installing npm dependencies...
call npm install
if %ERRORLEVEL% neq 0 (echo FAILED: npm install && pause && exit /b 1)

echo.
echo [3/4] Checking Vercel CLI...
vercel --version >NUL 2>&1 || (
    echo Installing Vercel CLI...
    call npm install -g vercel
)
vercel --version

echo.
echo [4/4] Ready to deploy!
echo.
echo ============================================
echo  NEXT STEPS:
echo ============================================
echo.
echo 1. Get your Neon DATABASE_URL from https://neon.tech (free tier)
echo    - Create project, copy the connection string
echo.
echo 2. Edit backend\.env.local and replace REPLACE_WITH_NEON_CONNECTION_STRING
echo    with your actual Neon URL
echo.
echo 3. Run the schema push:
echo    cd backend
echo    set DATABASE_URL=your_neon_url_here
echo    npm run db:push
echo.
echo 4. Login to Vercel (if not already):
echo    vercel login
echo.
echo 5. Deploy:
echo    cd backend
echo    vercel link
echo    vercel env add DATABASE_URL production
echo    vercel env add JWT_SECRET production
echo    vercel deploy --prod
echo.
echo JWT_SECRET already set in .env.local:
type .env.local | findstr JWT_SECRET
echo.
pause
