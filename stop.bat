@echo off
title AI Social Persona - Stop

echo.
echo   Stopping all services...
echo.

for %%p in (8080 8000 5173 6379) do (
    for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":%%p.*LISTENING"') do (
        if not "%%a"=="" taskkill /f /pid %%a >nul 2>&1
    )
)

echo.
echo   All services stopped.
echo.
pause