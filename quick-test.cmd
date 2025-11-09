@echo off
REM ========================================
REM Quick Test Script - Phase P0 + P1
REM ========================================

echo.
echo ========================================
echo GameServer - Quick Test Script
echo Phase P0 + P1 Validation
echo ========================================
echo.

REM Set colors
setlocal enabledelayedexpansion

REM ========================================
REM PHASE 0: BUILD CHECK
REM ========================================

echo [PHASE 0] Checking JAR files...
echo.

set "MISSING=0"

if not exist "common-lib\target\common-lib-1.0.0.jar" (
    echo [X] common-lib JAR NOT FOUND
    set MISSING=1
) else (
    echo [OK] common-lib JAR found
)

if not exist "eureka-server\target\eureka-server-1.0.0.jar" (
    echo [X] eureka-server JAR NOT FOUND
    set MISSING=1
) else (
    echo [OK] eureka-server JAR found
)

if not exist "config-service\target\config-service-1.0.0.jar" (
    echo [X] config-service JAR NOT FOUND
    set MISSING=1
) else (
    echo [OK] config-service JAR found
)

if not exist "gateway-service\target\gateway-service-1.0.0.jar" (
    echo [X] gateway-service JAR NOT FOUND
    set MISSING=1
) else (
    echo [OK] gateway-service JAR found
)

if not exist "webSocket-server\target\webSocket-server-1.0.0.jar" (
    echo [X] webSocket-server JAR NOT FOUND
    set MISSING=1
) else (
    echo [OK] webSocket-server JAR found
)

if not exist "session-service\target\session-service-1.0.0.jar" (
    echo [X] session-service JAR NOT FOUND
    set MISSING=1
) else (
    echo [OK] session-service JAR found
)

echo.

if "%MISSING%"=="1" (
    echo ========================================
    echo ERROR: Some JAR files are missing!
    echo ========================================
    echo.
    echo You need to build the services first.
    echo.
    echo Option 1: Build all at once
    echo   mvn clean install -DskipTests -pl common-lib,eureka-server,config-service,gateway-service,webSocket-server,session-service
    echo.
    echo Option 2: Build individually
    echo   See: TESTING_GUIDE_STEP_BY_STEP.md
    echo.
    pause
    exit /b 1
)

echo ========================================
echo All required JARs found!
echo ========================================
echo.

REM ========================================
REM PHASE 1: START SERVICES
REM ========================================

echo.
echo ========================================
echo Starting Services (Phase P0)
echo ========================================
echo.
echo This will open 5 terminal windows.
echo Please wait for each service to start.
echo.
pause

echo [1/5] Starting Eureka Server (8761)...
start "Eureka-8761" cmd /k "cd /d %~dp0eureka-server && java -jar target\eureka-server-1.0.0.jar"
echo Waiting 30 seconds for Eureka...
timeout /t 30 /nobreak

echo.
echo [2/5] Starting Config Service (8091)...
start "Config-8091" cmd /k "cd /d %~dp0config-service && java -jar target\config-service-1.0.0.jar"
echo Waiting 20 seconds for Config...
timeout /t 20 /nobreak

echo.
echo [3/5] Starting Gateway Service (8080)...
start "Gateway-8080" cmd /k "cd /d %~dp0gateway-service && java -jar target\gateway-service-1.0.0.jar"
echo Waiting 15 seconds for Gateway...
timeout /t 15 /nobreak

echo.
echo [4/5] Starting WebSocket Server (8090)...
start "WebSocket-8090" cmd /k "cd /d %~dp0webSocket-server && java -jar target\webSocket-server-1.0.0.jar"
echo Waiting 10 seconds for WebSocket...
timeout /t 10 /nobreak

echo.
echo [5/5] Starting Session Service (8081)...
start "Session-8081" cmd /k "cd /d %~dp0session-service && java -jar target\session-service-1.0.0.jar"
echo Waiting 10 seconds for Session...
timeout /t 10 /nobreak

echo.
echo ========================================
echo All services started!
echo ========================================
echo.

REM ========================================
REM PHASE 2: HEALTH CHECKS
REM ========================================

echo.
echo ========================================
echo Running Health Checks...
echo ========================================
echo.

echo [Test 1] Gateway Health Check...
curl -s http://localhost:8080/actuator/health > nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo [OK] Gateway is UP
) else (
    echo [X] Gateway health check failed
)

echo.
echo [Test 2] Config Health Check...
curl -s http://localhost:8091/actuator/health > nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo [OK] Config is UP
) else (
    echo [X] Config health check failed
)

echo.
echo [Test 3] Session Health Check...
curl -s http://localhost:8081/actuator/health > nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo [OK] Session is UP
) else (
    echo [X] Session health check failed
)

echo.
echo [Test 4] Eureka Dashboard...
curl -s http://localhost:8761 > nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo [OK] Eureka Dashboard accessible
) else (
    echo [X] Eureka Dashboard not accessible
)

echo.
echo ========================================
echo Health Checks Complete
echo ========================================
echo.

REM ========================================
REM PHASE 3: FUNCTIONAL TESTS
REM ========================================

echo.
echo ========================================
echo Running Functional Tests...
echo ========================================
echo.

echo [Test 5] Testing Session Login Endpoint...
echo.
echo Request: POST /session-service/api/session/login
curl -X POST http://localhost:8080/session-service/api/session/login ^
  -H "Content-Type: application/json" ^
  -d "{\"username\":\"testuser\",\"password\":\"testpass\"}" ^
  2>nul

echo.
echo.
echo [Test 6] Testing Time Sync Endpoint...
echo.
echo Request: GET /session-service/api/session/timesync
curl http://localhost:8080/session-service/api/session/timesync 2>nul

echo.
echo.

REM ========================================
REM PHASE 4: SUMMARY
REM ========================================

echo.
echo ========================================
echo Test Summary
echo ========================================
echo.
echo Services Started:
echo   - Eureka Server    : http://localhost:8761
echo   - Config Service   : http://localhost:8091
echo   - Gateway Service  : http://localhost:8080
echo   - WebSocket Server : ws://localhost:8080/websocket-server/ws/game
echo   - Session Service  : http://localhost:8081
echo.
echo Next Steps:
echo   1. Open Eureka Dashboard: http://localhost:8761
echo      - Verify all 4-5 services registered
echo.
echo   2. Test WebSocket connection:
echo      - Open browser DevTools (F12)
echo      - See: TESTING_GUIDE_STEP_BY_STEP.md (Test 2.3)
echo.
echo   3. Update client configuration:
echo      - SERVER_URL: http://localhost:8080
echo      - WS_URL: ws://localhost:8080/websocket-server/ws/game
echo      - See: docs/CLIENT_DIRECT_CONNECTION_GUIDE.md
echo.
echo   4. Start client and test connection
echo      - See: TESTING_GUIDE_STEP_BY_STEP.md (Phase 4)
echo.
echo ========================================
echo.

REM Ask to open Eureka
set /p "OPEN_EUREKA=Open Eureka Dashboard now? (Y/N): "
if /i "%OPEN_EUREKA%"=="Y" start http://localhost:8761

echo.
echo ========================================
echo Testing script complete!
echo ========================================
echo.
echo All terminal windows will stay open.
echo Close them when you're done testing.
echo.
echo For detailed testing guide, see:
echo   TESTING_GUIDE_STEP_BY_STEP.md
echo.
pause

