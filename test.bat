@echo off
REM ---------------------------------------------------------------
REM  Compiles src\ + test\ and runs the dependency-free test suite.
REM  Usage:  test.bat
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
    echo [ERROR] The JDBC driver is missing from lib\. Run setup.bat once.
    exit /b 1
)

if not exist out-test mkdir out-test

REM Relative, forward-slash paths - see the note in build.bat.
set "ROOT=%CD%\"
if exist "out-test\sources.txt" del "out-test\sources.txt"
for /r "src" %%f in (*.java) do (
    set "P=%%f"
    set "P=!P:%ROOT%=!"
    echo !P:\=/!>>"out-test\sources.txt"
)
for /r "test" %%f in (*.java) do (
    set "P=%%f"
    set "P=!P:%ROOT%=!"
    echo !P:\=/!>>"out-test\sources.txt"
)

echo Compiling tests...
javac -encoding UTF-8 -cp "lib\*" -d out-test "@out-test\sources.txt"
if errorlevel 1 (
    echo [ERROR] Compilation failed.
    exit /b 1
)

java -cp "out-test;lib\*" com.sms.AppTests
endlocal
