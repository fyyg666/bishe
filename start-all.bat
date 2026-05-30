@echo off
title Library System - Quick Start

echo ==============================================
echo      Library Management System - Quick Start
echo ==============================================
echo.

set "REDIS_PATH=redis"
set "BACKEND_PATH=backend"
set "FRONTEND_PATH=frontend"

echo [1/4] Checking environment...

where java >nul 2>nul
if %errorlevel% neq 0 (
    echo ERROR: Java not found. Please install JDK 17+ first.
    pause
    exit /b 1
)
echo Java environment is ready

where node >nul 2>nul
if %errorlevel% neq 0 (
    echo ERROR: Node.js not found. Please install Node.js 18+ first.
    pause
    exit /b 1
)
echo Node.js environment is ready

where npm >nul 2>nul
if %errorlevel% neq 0 (
    echo ERROR: npm not found. Please install Node.js first.
    pause
    exit /b 1
)
echo npm is ready

echo.
echo [2/4] Starting Redis Server...
start "Redis Server" /min "%REDIS_PATH%\redis-server.exe" "%REDIS_PATH%\redis.windows.conf"
echo Redis server started
timeout /t 3 /nobreak >nul

echo.
echo [3/4] Starting Backend Server...
cd /d "%BACKEND_PATH%"
start "Backend Server" cmd /k "mvn spring-boot:run -Dmaven.test.skip=true"
cd /d ..
echo Backend server started
timeout /t 8 /nobreak >nul

echo.
echo [4/4] Starting Frontend Server...
cd /d "%FRONTEND_PATH%"
start "Frontend Server" cmd /k "npm run dev"
cd /d ..
echo Frontend server started

echo.
echo ==============================================
echo            All Services Started!
echo ==============================================
echo.
echo Service URLs:
echo   - Frontend: http://localhost:3000
echo   - Backend API: http://localhost:8080/api/v1
echo   - Redis: localhost:6379
echo   - Swagger UI: http://localhost:8080/api/v1/swagger-ui.html
echo.
echo Press any key to close this window (services continue running)...
pause >nul