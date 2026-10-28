@echo off
REM ---------------------------------------------------------------
REM  Compiles every source file under src\ into out\
REM  Usage:  build.bat
REM ---------------------------------------------------------------
setlocal enabledelayedexpansion
cd /d "%~dp0"

where javac >nul 2>nul
if errorlevel 1 (
    echo [ERROR] javac was not found on your PATH.
    echo         Install a JDK 8 or newer and re-open this terminal.
    exit /b 1
)

if not exist "lib\sqlite-jdbc-3.46.1.3.jar" (
    echo [ERROR] The JDBC driver is missing from lib\.
    echo         Run setup.bat once to download it.
    exit /b 1
)

if not exist out mkdir out

REM Build the argfile from paths that are RELATIVE to this script and use
REM forward slashes. Absolute paths would break javac's argfile parser twice
REM over: it splits on the spaces in "Jane Doe", and it treats a backslash
REM as an escape character.
set "ROOT=%CD%\"
if exist "out\sources.txt" del "out\sources.txt"
for /r "src" %%f in (*.java) do (
    set "P=%%f"
    set "P=!P:%ROOT%=!"
    echo !P:\=/!>>"out\sources.txt"
)

echo Compiling...
javac -encoding UTF-8 -cp "lib\*" -d out "@out\sources.txt"
if errorlevel 1 (
    echo [ERROR] Compilation failed.
    exit /b 1
)

echo [OK] Classes written to out\
endlocal
