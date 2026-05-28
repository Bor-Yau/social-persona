@echo off
chcp 65001 >nul
title Social Persona Engine - Quick Start
cls

echo.
echo   ==========================================
echo   Social Persona Engine - Quick Start
echo   ==========================================
echo.

REM Clean up old port 8080
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8080.*LISTENING"') do (
    if not "%%a"=="" (
        echo   Cleaning old port 8080 PID=%%a ...
        taskkill /f /pid %%a >nul 2>&1
    )
)

REM [1/6] Redis (optional)
if exist "%~dp0redis\redis-server.exe" (
    echo   [1/6] Starting Redis...
    start "Redis-AI" /min cmd /c "cd /d %~dp0redis && redis-server.exe"
    timeout /t 2 /nobreak >nul
) else (
    echo   [1/6] Redis not found (optional, Java will use in-memory cache)
)

REM [2/6] Python environment
where python >nul 2>&1
if errorlevel 1 (
    echo   [ERROR] Python not found in PATH!
    echo.
    echo   Please install Python 3.12+ and add it to PATH.
    echo   Download: https://www.python.org/downloads/
    echo.
    goto fatal
)
python --version >nul 2>&1
if errorlevel 1 (
    echo   [ERROR] Python found but not working! Run 'python --version' to check.
    goto fatal
)
echo   [2/6] Python found

REM Python deps install with mirror fallback
echo   [2/6] Checking Python dependencies...
cd /d "%~dp0python-core"

REM Try 1: default
echo          Trying default pip source...
call python -m pip install -q -r requirements.txt
if not errorlevel 1 goto python_deps_ok

REM Try 2: Tsinghua mirror
echo          Trying Tsinghua mirror...
call python -m pip install -q -r requirements.txt -i https://pypi.tuna.tsinghua.edu.cn/simple --trusted-host pypi.tuna.tsinghua.edu.cn
if not errorlevel 1 goto python_deps_ok

REM Try 3: Aliyun mirror
echo          Trying Aliyun mirror...
call python -m pip install -q -r requirements.txt -i https://mirrors.aliyun.com/pypi/simple --trusted-host mirrors.aliyun.com
if not errorlevel 1 goto python_deps_ok

cd /d "%~dp0"
echo   [ERROR] pip install failed on all sources!
echo   Please check your network and try again.
goto fatal

:python_deps_ok
cd /d "%~dp0"
echo          Done.

REM [3/6] Download / check embedding model
echo   [3/6] Checking embedding model...
cd /d "%~dp0python-core"
python startup_check.py
if errorlevel 1 (
    cd /d "%~dp0"
    echo.
    echo   ==========================================
    echo   Model download failed on first launch.
    echo   See console output above for manual steps.
    echo   ==========================================
    echo.
    goto fatal
)
cd /d "%~dp0"

REM [4/6] Start Python service
echo   [4/6] Starting Python...
start "Python-AI" cmd /k "cd /d %~dp0python-core && python -m uvicorn main:app --host 127.0.0.1 --port 8000"
timeout /t 4 /nobreak >nul

REM [5/6] Maven auto-detect + start Java
set MVN_CMD=
where mvn.cmd >nul 2>&1
if not errorlevel 1 set MVN_CMD=mvn.cmd
if "%MVN_CMD%"=="" (
    where mvn >nul 2>&1
    if not errorlevel 1 set MVN_CMD=mvn
)
if "%MVN_CMD%"=="" (
    echo   [ERROR] Maven not found in PATH!
    echo   Please install Maven 3.6+ and add it to PATH.
    echo   Download: https://maven.apache.org/
    goto fatal
)
echo   [5/6] Starting Java (first build may take 1~3 min)...
start "Java-AI" cmd /k "cd /d %~dp0java-manager && %MVN_CMD% spring-boot:run"
timeout /t 10 /nobreak >nul

REM [6/6] Node.js + Frontend
where node >nul 2>&1
if errorlevel 1 (
    echo   [ERROR] Node.js not found in PATH!
    echo   Please install Node.js 18+ and add it to PATH.
    echo   Download: https://nodejs.org/
    goto fatal
)

REM Frontend deps
if not exist "%~dp0frontend\node_modules" (
    echo   [6/6] Installing frontend dependencies...

    echo          Trying default npm registry...
    cd /d "%~dp0frontend"
    call npm install
    if not errorlevel 1 goto npm_deps_ok

    echo          Trying npmmirror.com...
    call npm install --registry=https://registry.npmmirror.com
    if not errorlevel 1 goto npm_deps_ok

    cd /d "%~dp0"
    echo   [ERROR] npm install failed on all sources!
    goto fatal

    :npm_deps_ok
    cd /d "%~dp0"
    echo          Done.
)

echo   [6/6] Starting Frontend...
start "Frontend-AI" cmd /k "cd /d %~dp0frontend && npm run dev"

timeout /t 5 /nobreak >nul
start "" http://localhost:5173

echo.
echo   ==========================================
echo   All services started!
echo   Frontend : http://localhost:5173
echo   Close each CMD window to stop its service.
echo   ==========================================
echo.
goto done

:fatal
echo.
echo   ==========================================
echo   Startup aborted. Please fix the issue above
echo   and double-click start.bat again.
echo   ==========================================
echo.

:done
pause
