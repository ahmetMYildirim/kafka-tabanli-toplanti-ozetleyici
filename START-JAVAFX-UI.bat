@echo off
chcp 65001 >nul 2>&1
setlocal EnableDelayedExpansion

echo.
echo ========================================
echo   JAVAFX DASHBOARD UI BASLATILIYOR
echo ========================================
echo.

:: [1/5] JavaFX projesi kontrolü
echo [1/5] JavaFX projesi kontrol ediliyor...
if not exist meeting_dashboard_ui (
    echo [HATA] meeting_dashboard_ui klasoru bulunamadi!
    pause
    exit /b 1
)
echo [OK] JavaFX projesi mevcut

:: [2/5] Gradle wrapper kontrolü
echo [2/5] Gradle wrapper kontrol ediliyor...
if not exist meeting_dashboard_ui\gradlew.bat (
    echo [HATA] Gradle wrapper bulunamadi!
    pause
    exit /b 1
)
echo [OK] Gradle wrapper mevcut

:: [3/5] Gateway API kontrolü
echo [3/5] Gateway API kontrol ediliyor...
set "GATEWAY_URL=http://localhost:8084/api/v1/meetings"
echo Baglanilacak API: %GATEWAY_URL%
echo.

curl -s -o nul -w "%%{http_code}" --max-time 5 %GATEWAY_URL%/actuator/health >temp_code.txt 2>&1
set /p HEALTH_CODE=<temp_code.txt
del temp_code.txt 2>nul

if not "!HEALTH_CODE!"=="200" (
    echo [UYARI] Gateway API'ye ulasilamiyor (HTTP !HEALTH_CODE!^)
    echo.
    echo Docker servislerini baslatmak icin:
    echo   START-DOCKER.bat
    echo.
    choice /C YN /M "Yine de UI'yi baslatmak istiyor musunuz"
    if errorlevel 2 exit /b 1
)
echo [OK] Gateway API hazir

:: [4/5] JavaFX UI build
echo [4/5] JavaFX UI build ediliyor...
cd meeting_dashboard_ui
call gradlew.bat clean build -x test --console=plain >nul 2>&1
if errorlevel 1 (
    echo [HATA] Build basarisiz!
    cd ..
    pause
    exit /b 1
)
echo [OK] Build basarili

:: [5/5] JavaFX UI başlat
echo [5/5] JavaFX UI baslatiliyor...
echo.
echo ========================================
echo   DASHBOARD ACILIYOR
echo ========================================
echo.
echo Demo kullanici bilgileri:
echo   - Admin: admin / admin123
echo   - User:  user / user123
echo.

start "JavaFX Dashboard" cmd /c "gradlew.bat run"

cd ..

echo.
echo UI baslatildi!
echo.
echo Eger UI acilmazsa:
echo   1. Gateway API'nin calistigini kontrol edin (TEST-SERVICES.bat)
echo   2. meeting_dashboard_ui\build\libs klasorunu kontrol edin
echo.
pause
