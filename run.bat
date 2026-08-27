@echo off
echo ===================================================
echo   Starting QuantumTrade Stock Platform...
echo ===================================================
cd /d "%~dp0"
java -jar target\stock-trading-platform-1.0.0.jar 8080
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo Building project with Maven...
    call mvn clean package -DskipTests
    java -jar target\stock-trading-platform-1.0.0.jar 8080
)
pause
