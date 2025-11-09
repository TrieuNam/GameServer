@echo off
echo ========================================
echo Building All Services - Phase P0 and P1
echo ========================================
echo.

echo [1/9] Building common-lib...
cd /d "%~dp0common-lib"
call mvn clean install -DskipTests
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: common-lib build failed!
    pause
    exit /b 1
)
echo common-lib: SUCCESS
echo.

echo [2/9] Building shop-service...
cd /d "%~dp0shop-service"
call mvn clean install -DskipTests
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: shop-service build failed!
    pause
    exit /b 1
)
echo shop-service: SUCCESS
echo.

echo [3/9] Building equip-service...
cd /d "%~dp0equip-service"
call mvn clean install -DskipTests
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: equip-service build failed!
    pause
    exit /b 1
)
echo equip-service: SUCCESS
echo.

echo [4/9] Building drop-service...
cd /d "%~dp0drop-service"
call mvn clean install -DskipTests
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: drop-service build failed!
    pause
    exit /b 1
)
echo drop-service: SUCCESS
echo.

echo [5/9] Building gift-service...
cd /d "%~dp0gift-service"
call mvn clean install -DskipTests
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: gift-service build failed!
    pause
    exit /b 1
)
echo gift-service: SUCCESS
echo.

echo [6/9] Building box-service...
cd /d "%~dp0box-service"
call mvn clean install -DskipTests
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: box-service build failed!
    pause
    exit /b 1
)
echo box-service: SUCCESS
echo.

echo ========================================
echo Build Summary
echo ========================================
echo All Phase P1 Economy Services built successfully!
echo.
echo Services built:
echo   - common-lib
echo   - shop-service
echo   - equip-service
echo   - drop-service
echo   - gift-service
echo   - box-service
echo.
echo Next: Run start-all-services.cmd to start all services
echo ========================================
pause

