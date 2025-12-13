@echo off
setlocal EnableDelayedExpansion

rem Mirror the GitHub Actions build matrix locally
set "REPO_DIR=%~dp0"
set "GRADLEW=%REPO_DIR%gradlew.bat"
if not exist "%GRADLEW%" (
    echo Error: gradlew.bat not found next to test-all-versions.bat.
    exit /b 1
)

set "PROPS_FILE=%REPO_DIR%gradle.properties"
if not exist "%PROPS_FILE%" (
    echo Error: gradle.properties not found at %PROPS_FILE%.
    exit /b 1
)

set "PYTHON_CMD="
for %%P in (python3 python py) do (
    if not defined PYTHON_CMD (
        where %%P >nul 2>&1
        if not errorlevel 1 set "PYTHON_CMD=%%P"
    )
)
if not defined PYTHON_CMD (
    echo Error: No suitable Python interpreter found (python3, python, py).
    exit /b 1
)

set "MIN_VERSION="
for /f "tokens=1,* delims== " %%A in ('type "%PROPS_FILE%" ^| findstr /B /C:"minPaperVersion="') do (
    set "MIN_VERSION=%%B"
)
if not defined MIN_VERSION set "MIN_VERSION=1.16"
set "MIN_VERSION=!MIN_VERSION: =!"
if "!MIN_VERSION!"=="" set "MIN_VERSION=1.16"

set "VERSIONS_JSON="
for /f "usebackq delims=" %%I in (`"%PYTHON_CMD%" "%REPO_DIR%.github\scripts\paper_versions.py" "!MIN_VERSION!" 2^>nul`) do (
    set "VERSIONS_JSON=%%I"
)
if not defined VERSIONS_JSON (
    echo Warning: Failed to resolve Paper versions, falling back to !MIN_VERSION!.
    set "VERSIONS_JSON=[^"!MIN_VERSION!^"]"
)

set "VERSIONS=!VERSIONS_JSON!"
set "VERSIONS=!VERSIONS:[=!"
set "VERSIONS=!VERSIONS:]=!"
set "VERSIONS=!VERSIONS:^"=!"
set "VERSIONS=!VERSIONS:,= !"
echo Resolved Paper versions: !VERSIONS!

echo.
echo Running Velocity build...
call "%GRADLEW%" clean :velocity:build
if errorlevel 1 (
    echo Velocity build failed.
    exit /b 1
)

for %%V in (!VERSIONS!) do (
    if "%%V"=="" (
        rem Skip empty tokens that might appear if parsing failed.
        continue
    )
    echo.
    echo Running Paper build for version %%V...
    call "%GRADLEW%" clean :paper:build -Pminecraft_version=%%V
    if errorlevel 1 (
        echo Paper build failed for version %%V.
        exit /b 1
    )
)

echo.
echo All builds completed successfully.
exit /b 0
