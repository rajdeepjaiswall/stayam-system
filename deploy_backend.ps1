# Sahu Sales Solution - Backend Deploy Script
# Run this after you have your DATABASE_URL from Neon.

param(
    [Parameter(Mandatory=$true)]
    [string]$DatabaseUrl,

    [Parameter(Mandatory=$false)]
    [string]$JwtSecret = ""
)

$backendDir = "$PSScriptRoot\backend"
$envFile = "$backendDir\.env.local"

Write-Host "============================================" -ForegroundColor Cyan
Write-Host " SAHU SALES - BACKEND DEPLOY" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan

# Use existing JWT secret from .env.local or generate awareness
if ($JwtSecret -eq "") {
    $existing = Get-Content $envFile | Where-Object { $_ -match "JWT_SECRET=" }
    if ($existing) {
        $JwtSecret = ($existing -split "=", 2)[1]
        Write-Host "Using existing JWT_SECRET from .env.local" -ForegroundColor Green
    } else {
        Write-Host "ERROR: No JWT_SECRET provided and none found in .env.local" -ForegroundColor Red
        exit 1
    }
}

# Update .env.local
Write-Host ""
Write-Host "[1/5] Updating .env.local..." -ForegroundColor Yellow
$envContent = @"
DATABASE_URL=$DatabaseUrl
JWT_SECRET=$JwtSecret
"@
Set-Content -Path $envFile -Value $envContent
Write-Host "Done." -ForegroundColor Green

# npm install
Write-Host ""
Write-Host "[2/5] Installing dependencies..." -ForegroundColor Yellow
Set-Location $backendDir
& npm install
if ($LASTEXITCODE -ne 0) { Write-Host "npm install failed" -ForegroundColor Red; exit 1 }

# Run schema push
Write-Host ""
Write-Host "[3/5] Pushing schema to Neon..." -ForegroundColor Yellow
$env:DATABASE_URL = $DatabaseUrl
& npm run db:push
if ($LASTEXITCODE -ne 0) { Write-Host "Schema push failed" -ForegroundColor Red; exit 1 }
Write-Host "Schema applied!" -ForegroundColor Green

# Vercel deploy
Write-Host ""
Write-Host "[4/5] Deploying to Vercel..." -ForegroundColor Yellow
Write-Host "(Make sure you ran: vercel login)" -ForegroundColor Gray

& vercel link --yes
& vercel env rm DATABASE_URL production --yes 2>$null
& vercel env rm JWT_SECRET production --yes 2>$null
Write-Host $DatabaseUrl | & vercel env add DATABASE_URL production
Write-Host $JwtSecret | & vercel env add JWT_SECRET production
& vercel deploy --prod

if ($LASTEXITCODE -ne 0) { Write-Host "Vercel deploy failed" -ForegroundColor Red; exit 1 }

Write-Host ""
Write-Host "[5/5] Getting deployment URL..." -ForegroundColor Yellow
$url = & vercel --prod 2>&1 | Select-String "https://" | Select-Object -Last 1
Write-Host ""
Write-Host "============================================" -ForegroundColor Green
Write-Host " DEPLOYED SUCCESSFULLY!" -ForegroundColor Green
Write-Host "============================================" -ForegroundColor Green
Write-Host ""
Write-Host "Your backend URL: $url" -ForegroundColor Cyan
Write-Host ""
Write-Host "Next step: Update android/app/src/main/java/.../core/Config.kt"
Write-Host "  const val BASE_URL = `"YOUR_URL_HERE`""
Write-Host ""
