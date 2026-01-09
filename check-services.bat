@echo off
REM ===================================================================
REM Servis Durumu Kontrol Scripti
REM ===================================================================

echo.
echo ========================================
echo Servis Durumu Kontrol Ediliyor...
echo ========================================
echo.

echo [1] Port Durumları:
echo.
netstat -ano | findstr :8081
if %errorlevel% equ 0 (
    echo   [OK] Port 8081 (Collector Service) - ACIK
) else (
    echo   [X] Port 8081 (Collector Service) - KAPALI
)

netstat -ano | findstr :8082
if %errorlevel% equ 0 (
    echo   [OK] Port 8082 (Streaming Service) - ACIK
) else (
    echo   [X] Port 8082 (Streaming Service) - KAPALI
)

netstat -ano | findstr :8083
if %errorlevel% equ 0 (
    echo   [OK] Port 8083 (AI Service) - ACIK
) else (
    echo   [X] Port 8083 (AI Service) - KAPALI
)

netstat -ano | findstr :8084
if %errorlevel% equ 0 (
    echo   [OK] Port 8084 (Gateway API) - ACIK
) else (
    echo   [X] Port 8084 (Gateway API) - KAPALI
)

echo.
echo [2] Docker Servisleri:
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" | findstr toplanti
if %errorlevel% neq 0 (
    echo   Docker servisleri calismiyor
)

echo.
echo [3] Gateway API Test:
echo   Test ediliyor: http://localhost:8084/api/v1/dashboard/stats
powershell -Command "try { $response = Invoke-WebRequest -Uri 'http://localhost:8084/api/v1/dashboard/stats' -Method GET -TimeoutSec 5 -UseBasicParsing; Write-Host '  [OK] Gateway API yanit veriyor - HTTP' $response.StatusCode } catch { Write-Host '  [X] Gateway API yanit vermiyor:' $_.Exception.Message }"

echo.
pause
