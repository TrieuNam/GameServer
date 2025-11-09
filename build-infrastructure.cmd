@echo off
REM Build all infrastructure services

echo ============================================
echo   Building Infrastructure Services
echo ============================================
echo.

echo [1/4] Building Common Library...
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

echo [2/4] Building Eureka Server...
cd eureka-server
call mvn clean install -DskipTests
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Failed to build eureka-server
    cd ..
    pause
    exit /b 1
)
cd ..
echo [OK] Eureka Server built successfully
echo.

echo [3/4] Building Config Service...
cd config-service
call mvn clean install -DskipTests
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Failed to build config-service
    cd ..
    pause
    exit /b 1
)
cd ..
echo [OK] Config Service built successfully
echo.

echo [4/4] Building Gateway Service...
cd gateway-service
call mvn clean install -DskipTests
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Failed to build gateway-service
    cd ..
    pause
    exit /b 1
)
cd ..
echo [OK] Gateway Service built successfully
echo.

echo ============================================
echo   Build Complete!
echo ============================================
echo.
echo All infrastructure services built successfully.
echo You can now start them using: start-infrastructure.cmd
echo.
pause

