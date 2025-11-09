@echo off
REM Build all remaining economy services after common-lib update

echo ============================================
echo   Building Common Library with Bag DTOs
echo ============================================
cd common-lib
call mvn clean install -DskipTests
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Failed to build common-lib
    cd ..
    pause
    exit /b 1
)
cd ..
echo [OK] Common Library built successfully
echo.

echo ============================================
echo   Building Remaining Economy Services
echo ============================================

echo [1/5] Building Shop Service...
cd shop-service
call mvn clean install -DskipTests
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Failed to build shop-service
    cd ..
    pause
    exit /b 1
)
cd ..
echo [OK] Shop Service built successfully
echo.

echo [2/5] Building Equip Service...
cd equip-service
call mvn clean install -DskipTests
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Failed to build equip-service
    cd ..
    pause
    exit /b 1
)
cd ..
echo [OK] Equip Service built successfully
echo.

echo [3/5] Building Drop Service...
cd drop-service
call mvn clean install -DskipTests
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Failed to build drop-service
    cd ..
    pause
    exit /b 1
)
cd ..
echo [OK] Drop Service built successfully
echo.

echo [4/5] Building Gift Service...
cd gift-service
call mvn clean install -DskipTests
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Failed to build gift-service
    cd ..
    pause
    exit /b 1
)
cd ..
echo [OK] Gift Service built successfully
echo.

echo [5/5] Building Box Service...
cd box-service
call mvn clean install -DskipTests
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Failed to build box-service
    cd ..
    pause
    exit /b 1
)
cd ..
echo [OK] Box Service built successfully
echo.

echo ============================================
echo   Build Complete!
echo ============================================
echo.
echo All remaining economy services built successfully:
echo   - shop-service-1.0.0.jar (Port 8260)
echo   - equip-service-1.0.0.jar (Port 8240)
echo   - drop-service-1.0.0.jar (Port 8250)
echo   - gift-service-1.0.0.jar (Port 8270)
echo   - box-service-1.0.0.jar (Port 8290)
echo.
echo Phase P1 - ALL Economy Services Complete!
echo.
pause

