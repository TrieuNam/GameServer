@echo off
REM Build all economy services (Phase P1)

echo ============================================
echo   Building Economy Services (Phase P1)
echo ============================================
echo.

echo [1/3] Building Item Service...
cd item-service
call mvn clean install -DskipTests
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Failed to build item-service
    cd ..
    pause
    exit /b 1
)
cd ..
echo [OK] Item Service built successfully
echo.

echo [2/3] Building Wallet Service...
cd wallet-service
call mvn clean install -DskipTests
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Failed to build wallet-service
    cd ..
    pause
    exit /b 1
)
cd ..
echo [OK] Wallet Service built successfully
echo.

echo [3/3] Building Bag Service...
cd bag-service
call mvn clean install -DskipTests
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Failed to build bag-service
    cd ..
    pause
    exit /b 1
)
cd ..
echo [OK] Bag Service built successfully
echo.

echo ============================================
echo   Build Complete!
echo ============================================
echo.
echo All economy services built successfully:
echo   - item-service-1.0.0.jar (Port 8220)
echo   - wallet-service-1.0.0.jar (Port 8210)
echo   - bag-service-1.0.0.jar (Port 8230)
echo.
pause

