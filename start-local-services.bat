@echo off
REM ===================================================================
REM Kafka Tabanlı Toplantı Özetleyici - Local Servis Başlatma Scripti
REM Tüm mikroservisleri local olarak başlatır
REM ===================================================================

echo.
echo ========================================
echo Local Mikroservisler Başlatılıyor...
echo ========================================
echo.

REM Port çakışması kontrolü
echo [0/7] Port çakışması kontrol ediliyor...
netstat -ano | findstr :8081 >nul 2>&1
if %errorlevel% equ 0 (
    echo UYARI: Port 8081 zaten kullanımda! Docker container'ları durdurun.
    echo stop-docker-services.bat çalıştırarak Docker'ı durdurabilirsiniz.
    pause
)

netstat -ano | findstr :8082 >nul 2>&1
if %errorlevel% equ 0 (
    echo UYARI: Port 8082 zaten kullanımda! Docker container'ları durdurun.
    echo stop-docker-services.bat çalıştırarak Docker'ı durdurabilirsiniz.
    pause
)

netstat -ano | findstr :8083 >nul 2>&1
if %errorlevel% equ 0 (
    echo UYARI: Port 8083 zaten kullanımda!
    pause
)

netstat -ano | findstr :8084 >nul 2>&1
if %errorlevel% equ 0 (
    echo UYARI: Port 8084 zaten kullanımda!
    pause
)

echo [1/7] Docker servisleri kontrol ediliyor...
docker ps >nul 2>&1
if %errorlevel% neq 0 (
    echo HATA: Docker çalışmıyor! Lütfen Docker Desktop'ı başlatın.
    pause
    exit /b 1
)

echo [2/8] Docker altyapı servisleri başlatılıyor (MySQL, Kafka, Zookeeper, Kafka UI)...
docker-compose up -d mysql zookeeper kafka kafka-ui
timeout /t 10 /nobreak >nul

echo [3/8] Collector Service başlatılıyor (Port 8081)...
start "Collector Service" cmd /k "cd collector_service && gradlew.bat bootRun"
timeout /t 5 /nobreak >nul

echo [4/8] AI Service başlatılıyor (Port 8083)...
start "AI Service" cmd /k "cd ai_service && gradlew.bat bootRun"
timeout /t 5 /nobreak >nul

echo [5/8] Gateway API başlatılıyor (Port 8084)...
start "Gateway API" cmd /k "cd gateway_api && gradlew.bat bootRun"
timeout /t 5 /nobreak >nul

echo [6/8] Meeting Streaming Service başlatılıyor (Port 8082)...
start "Streaming Service" cmd /k "cd meeting_streaming_service && gradlew.bat bootRun"
timeout /t 5 /nobreak >nul

echo [7/8] Servisler başlatıldı!
echo [8/8] Kafka UI hazır!

echo.
echo ========================================
echo Tüm servisler başlatıldı!
echo ========================================
echo.
echo Servis Portları:
echo   - Collector Service:  http://localhost:8081
echo   - AI Service:          http://localhost:8083
echo   - Gateway API:         http://localhost:8084
echo   - Streaming Service:  http://localhost:8082
echo   - MySQL:              localhost:3308
echo   - Kafka:              localhost:9092
echo   - Kafka UI:           http://localhost:8090
echo.
echo Servislerin başlamasını bekleyin (30-60 saniye)...
echo Logları görmek için açılan pencerelere bakın.
echo.
pause
