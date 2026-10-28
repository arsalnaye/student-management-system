@echo off
REM ---------------------------------------------------------------
REM  Builds (if needed) and launches the Student Management System.
REM
REM  Usage:  run.bat                    use db.properties (SQLite by default)
REM          run.bat --db my.db         a different SQLite file
REM          run.bat --csv my.csv       the alternative CSV store
REM ---------------------------------------------------------------
setlocal
cd /d "%~dp0"

REM Always qualify the path: if NoDefaultCurrentDirectoryInExePath is set,
REM cmd refuses to run a script from the current directory unqualified.
call "%~dp0build.bat"
if errorlevel 1 exit /b 1

echo Starting Student Management System...
java -cp "out;lib\*" com.sms.Main %*
endlocal
