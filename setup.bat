@echo off
REM ---------------------------------------------------------------
REM  One-time setup: downloads the JDBC driver jars into lib\.
REM  Only needed if lib\ is empty (the jars ship with this project).
REM  Usage:  setup.bat
REM ---------------------------------------------------------------
setlocal
cd /d "%~dp0"

if not exist lib mkdir lib

echo Downloading the SQLite JDBC driver into lib\ ...
powershell -NoProfile -Command ^
  "$ErrorActionPreference='Stop';" ^
  "$base='https://repo1.maven.org/maven2';" ^
  "$files=@{" ^
  "  'sqlite-jdbc-3.46.1.3.jar'='org/xerial/sqlite-jdbc/3.46.1.3/sqlite-jdbc-3.46.1.3.jar';" ^
  "  'slf4j-api-2.0.16.jar'='org/slf4j/slf4j-api/2.0.16/slf4j-api-2.0.16.jar';" ^
  "  'slf4j-nop-2.0.16.jar'='org/slf4j/slf4j-nop/2.0.16/slf4j-nop-2.0.16.jar'" ^
  "};" ^
  "foreach ($n in $files.Keys) {" ^
  "  if (Test-Path \"lib\$n\") { Write-Host \"  already present: $n\"; continue }" ^
  "  Invoke-WebRequest -Uri \"$base/$($files[$n])\" -OutFile \"lib\$n\" -UseBasicParsing;" ^
  "  Write-Host \"  downloaded: $n\"" ^
  "}"

if errorlevel 1 (
    echo [ERROR] Download failed. Check your internet connection, or copy the
    echo         jars into lib\ by hand from https://repo1.maven.org/maven2
    exit /b 1
)

echo.
echo [OK] lib\ is ready. Run build.bat or run.bat next.
echo.
echo To use MySQL instead, also place mysql-connector-j-8.x.x.jar in lib\
echo and edit db.properties.
endlocal
