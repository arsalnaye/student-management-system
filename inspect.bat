@echo off
REM ---------------------------------------------------------------
REM  Prints the contents of the database: driver, table definition,
REM  every row and a few aggregates.
REM
REM  Usage:  inspect.bat                 the database from db.properties
REM          inspect.bat --db other.db   a specific SQLite file
REM ---------------------------------------------------------------
setlocal
cd /d "%~dp0"

REM Always qualify the path: if NoDefaultCurrentDirectoryInExePath is set,
REM cmd refuses to run a script from the current directory unqualified.
call "%~dp0build.bat"
if errorlevel 1 exit /b 1

java -cp "out;lib\*" com.sms.db.DbInspector %*
endlocal
