@echo off
REM Builds (if needed) and starts the server, loading settings from .env when present.
cd /d "%~dp0"

if exist .env (
  for /f "usebackq eol=# tokens=1,* delims==" %%A in (".env") do (
    if not "%%B"=="" set "%%A=%%B"
  )
)

if not exist target\darija-translator.jar (
  call mvn -q package -DskipTests || exit /b 1
)

java -jar target\darija-translator.jar
