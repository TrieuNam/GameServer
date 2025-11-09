@echo off
REM ========================================
REM GameServer - Complete Startup Script
REM No Cocos server needed
REM ========================================

echo.
echo ========================================
echo Starting GameServer Services
echo Direct client connection ready
echo ========================================
echo.

REM Check if JARs exist
echo Checking JAR files...
if not exist "eureka-server\target\eureka-server-1.0.0.jar" (
    echo ERROR: Eureka JAR not found! Run build first.
    pause
    exit /b 1
)

echo.
echo [1/5] Starting Eureka Server (Service Discovery)
echo Port: 8761
echo Dashboard: http://localhost:8761
start "Eureka-8761" cmd /k "cd /d %~dp0eureka-server && java -jar target\eureka-server-1.0.0.jar"
echo Waiting 30 seconds for Eureka to start...
timeout /t 30 /nobreak

echo.
echo [2/5] Starting Config Service (Configuration Management)
echo Port: 8091
echo Health: http://localhost:8091/actuator/health
start "Config-8091" cmd /k "cd /d %~dp0config-service && java -jar target\config-service-1.0.0.jar"
echo Waiting 20 seconds for Config to start...
timeout /t 20 /nobreak

echo.
echo [3/5] Starting Gateway Service (API Gateway + WebSocket Proxy)
echo Port: 8080
echo This is your CLIENT CONNECTION POINT
echo Health: http://localhost:8080/actuator/health
start "Gateway-8080" cmd /k "cd /d %~dp0gateway-service && java -jar target\gateway-service-1.0.0.jar"
echo Waiting 15 seconds for Gateway to start...
timeout /t 15 /nobreak

echo.
echo [4/5] Starting WebSocket Server (Real-time Binary Protocol)
echo Port: 8090
echo Access via Gateway: ws://localhost:8080/websocket-server/ws/game
start "WebSocket-8090" cmd /k "cd /d %~dp0webSocket-server && java -jar target\webSocket-server-1.0.0.jar"
echo Waiting 10 seconds for WebSocket to start...
timeout /t 10 /nobreak

echo.
echo [5/5] Starting Session Service (Authentication)
echo Port: 8081
echo Login: http://localhost:8080/session-service/api/session/login
start "Session-8081" cmd /k "cd /d %~dp0session-service && java -jar target\session-service-1.0.0.jar"
echo Waiting 10 seconds for Session to start...
timeout /t 10 /nobreak

echo.
echo ========================================
echo All GameServer Services Started!
echo ========================================
echo.
echo Infrastructure:
echo   - Eureka Dashboard: http://localhost:8761
echo   - Config Health:    http://localhost:8091/actuator/health
echo.
echo CLIENT CONNECTION POINTS:
echo   - REST API:    http://localhost:8080
echo   - WebSocket:   ws://localhost:8080/websocket-server/ws/game
echo   - Login:       http://localhost:8080/session-service/api/session/login
echo.
echo Example login:
echo   POST http://localhost:8080/session-service/api/session/login
echo   Body: {"username":"user","password":"pass"}
echo.
echo After login, use token to connect WebSocket:
echo   ws://localhost:8080/websocket-server/ws/game?token=YOUR_JWT_TOKEN
echo.
echo ========================================
echo.
echo Press any key to open Eureka dashboard...
pause
start http://localhost:8761

