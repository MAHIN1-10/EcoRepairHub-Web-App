@echo off
setlocal enabledelayedexpansion
title EcoRepair Hub

rem ============ settings ============
set "XAMPP=C:\xampp"
set "PORT=8082"
rem ==================================

cd /d "%~dp0EcoBackend" || (echo Could not find the EcoBackend folder. & pause & exit /b 1)

echo ==========================================
echo   EcoRepair Hub
echo ==========================================

rem ---- Java ----
where java >nul 2>nul
if errorlevel 1 (
  echo [ERROR] Java was not found. Install JDK 17 or newer and try again.
  pause & exit /b 1
)

rem ---- Already running? just open the browser ----
netstat -ano | findstr /R /C:":%PORT% .*LISTENING" >nul
if not errorlevel 1 (
  echo The app is already running on port %PORT%.
  start "" "http://localhost:%PORT%/"
  exit /b 0
)

rem ---- MySQL (XAMPP) ----
netstat -ano | findstr /R /C:":3306 .*LISTENING" >nul
if errorlevel 1 (
  if not exist "%XAMPP%\mysql\bin\mysqld.exe" (
    echo [ERROR] MySQL is not running and XAMPP was not found at %XAMPP%.
    echo         Start MySQL from the XAMPP Control Panel, or fix the XAMPP path at the top of run.bat.
    pause & exit /b 1
  )
  echo Starting MySQL...
  start "MySQL" /min "%XAMPP%\mysql\bin\mysqld.exe" --defaults-file="%XAMPP%\mysql\bin\my.ini" --standalone
  set /a tries=0
  :wait_mysql
  timeout /t 1 /nobreak >nul
  netstat -ano | findstr /R /C:":3306 .*LISTENING" >nul
  if errorlevel 1 (
    set /a tries+=1
    if !tries! lss 30 goto wait_mysql
    echo [ERROR] MySQL did not start. Start it from the XAMPP Control Panel and run this file again.
    pause & exit /b 1
  )
)
echo MySQL is running.

rem ---- Database (safe to run every time) ----
echo Preparing database...
"%XAMPP%\mysql\bin\mysql.exe" -u root < "database\ecorepairhub.sql"
if errorlevel 1 (
  echo [ERROR] Could not import database\ecorepairhub.sql
  echo         If your MySQL root user has a password, update application.properties and import the file manually.
  pause & exit /b 1
)

rem ---- Build ----
where mvn >nul 2>nul
if errorlevel 1 (
  if exist "target\EcoBackend-0.0.1-SNAPSHOT.jar" (
    echo Maven not found - using the existing build.
  ) else (
    echo [ERROR] Maven was not found and there is no built jar. Install Maven and try again.
    pause & exit /b 1
  )
) else (
  echo Building...
  call mvn -q -DskipTests package
  if errorlevel 1 (
    echo [ERROR] Build failed.
    pause & exit /b 1
  )
)

rem ---- Run (opens the browser once the server is up) ----
echo.
echo Starting the app on http://localhost:%PORT%/  (close this window to stop it)
start "" /b cmd /c "for /l %%i in (1,1,60) do (timeout /t 1 /nobreak >nul & netstat -ano | findstr /R /C:":%PORT% .*LISTENING" >nul && (start "" http://localhost:%PORT%/ & exit /b))"
java -jar "target\EcoBackend-0.0.1-SNAPSHOT.jar"

echo.
echo The app has stopped.
pause
