@echo off
setlocal enabledelayedexpansion

cd /d "%~dp0"

REM 加载 .env 文件（如果存在）
if exist ".env" (
    echo [INFO] Loading configuration from .env file...
    for /f "usebackq tokens=1,2 delims==" %%a in (".env") do (
        echo %%a | findstr /r "^[#^]" >nul
        if errorlevel 1 (
            set "%%a=%%b"
        )
    )
)

echo =========================================
echo   LiSuan Database Configuration
echo =========================================
echo.
echo [INFO] ENVIRONMENT: %ENVIRONMENT%
echo [INFO] Launching database configuration tool...
echo.
REM Find executable fat JAR. It contains the installer and all runtime dependencies.
set "JAR_FILE="
for %%f in (target\lisuan-fx-*-jar-with-dependencies.jar) do (
    set "JAR_FILE=%%f"
    goto :jar_found
)
:jar_found

if "%JAR_FILE%"=="" (
    echo [ERROR] Application JAR not found
    echo Please run: mvn clean package
    pause
    exit /b 1
)

echo [INFO] Found application JAR: %JAR_FILE%
java -cp "%JAR_FILE%" com.cashier.installer.DatabaseConfigDialog

if errorlevel 1 (
    echo.
    echo [ERROR] Configuration tool failed
    echo.
    echo Make sure the project is built: mvn clean package
    pause
    exit /b 1
)

echo.
echo [INFO] Configuration complete
echo You can now run start.bat to launch the application.
echo.
pause
