@echo off
REM Stop all Java services

echo ============================================
echo   Stopping All Java Services
echo ============================================
echo.

echo Killing all Java processes...
taskkill /F /IM java.exe /T 2>nul

if %ERRORLEVEL% EQU 0 (
    echo All Java services stopped successfully.
) else (
    echo No Java processes found or error stopping services.
)

echo.
pause

