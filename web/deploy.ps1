# deploy.ps1
# SimpleSchedule One-Click Cloud Deployment Script (Upload only, compilation is skipped)

# ==================== Configuration ====================
$SERVER_IP = "60.205.212.144"  # Enter your Alibaba Cloud server public IP here
$SSH_USER = "root"               # SSH Username (usually root)
$REMOTE_DIR = "/www/wwwroot/www.lingflame.cn" # Baota site root directory
# =======================================================

# Dynamically resolve project root directory
$PROJECT_ROOT = $PSScriptRoot
if (-not (Test-Path "$PROJECT_ROOT/app") -and (Test-Path "$PROJECT_ROOT/../app")) {
    $PROJECT_ROOT = Resolve-Path "$PROJECT_ROOT/.."
}

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "🚀 Starting deployment flow (skipping compilation)..." -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan

# 1. Locate local APK
$APK_PATHS = @(
    "$PROJECT_ROOT/app/release/app-release.apk",
    "$PROJECT_ROOT/app/build/outputs/apk/release/app-release.apk"
)

$SELECTED_APK = $null
foreach ($path in $APK_PATHS) {
    if (Test-Path $path) {
        if ($null -eq $SELECTED_APK) {
            $SELECTED_APK = $path
        } else {
            # If both exist, pick the newer one
            $time1 = (Get-Item $SELECTED_APK).LastWriteTime
            $time2 = (Get-Item $path).LastWriteTime
            if ($time2 -gt $time1) {
                $SELECTED_APK = $path
            }
        }
    }
}

if ($null -eq $SELECTED_APK) {
    Write-Host "❌ APK not found! Please build the release APK first in Android Studio:" -ForegroundColor Red
    Write-Host "   Build -> Build Bundle(s) / APK(s) -> Build APK(s)" -ForegroundColor Red
    Write-Host "   or Generate Signed APK" -ForegroundColor Red
    exit
}

Write-Host "✅ Found APK to upload: $SELECTED_APK" -ForegroundColor Green

# 2. Upload files
Write-Host "Uploading files to server..." -ForegroundColor Yellow

# Upload files in web/
Write-Host "-> Uploading web pages and scripts..." -ForegroundColor Gray
scp "$PROJECT_ROOT/web/index.php" "$PROJECT_ROOT/web/download.php" "$PROJECT_ROOT/web/show.php" "$PROJECT_ROOT/web/update.json" "${SSH_USER}@${SERVER_IP}:${REMOTE_DIR}/"

if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Failed to upload web files! Please check your network or passwordless SSH settings." -ForegroundColor Red
    exit
}

# Upload APK
Write-Host "-> Uploading APK ($SELECTED_APK)..." -ForegroundColor Gray
scp $SELECTED_APK "${SSH_USER}@${SERVER_IP}:${REMOTE_DIR}/app-release.apk"

if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Failed to upload APK!" -ForegroundColor Red
    exit
}

Write-Host "==========================================" -ForegroundColor Green
Write-Host "🎉 Success! Latest web files, configurations, and APK have been deployed!" -ForegroundColor Green
Write-Host "==========================================" -ForegroundColor Green
