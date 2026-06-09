@echo off
setlocal EnableDelayedExpansion

rem ============================================
rem Default values
rem ============================================
set "android=FALSE"
set "clean=FALSE"
set "reconfig=FALSE"
set "help=FALSE"
set "ninjapath=%~dp0ninja.exe"
set "platform="
set "oal="
set "src="

rem ============================================
rem Argument parsing
rem ============================================
:GETOPS
if "%~1"=="" goto :CHECK_ARGS
if /I "%~1"=="--ndk-path" (
    if "%~2"=="" (
        echo Error: --ndk-path requires a value.
        exit /b 1
    )
    set "ndkpath=%~2"
    shift
    goto :SHIFT_AND_NEXT
)
if /I "%~1"=="--platform" (
    if "%~2"=="" (
        echo Error: --platform requires a value.
        exit /b 1
    )
    set "platform=%~2"
    shift
    goto :SHIFT_AND_NEXT
)
if /I "%~1"=="--oal" (
    if "%~2"=="" (
        echo Error: --oal requires a value.
        exit /b 1
    )
    set "oal=%~2"
    shift
    goto :SHIFT_AND_NEXT
)
if /I "%~1"=="--reconfig" set "reconfig=TRUE"
if /I "%~1"=="--android" set "android=TRUE"
if /I "%~1"=="--clean" set "clean=TRUE"
if /I "%~1"=="--help" set "help=TRUE"
:SHIFT_AND_NEXT
shift
goto :GETOPS

:CHECK_ARGS
if "%help%"=="TRUE" goto :SHOW_HELP

rem ============================================
rem Validate dependencies
rem ============================================
where cmake >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo Error: cmake not found in PATH.
    exit /b 1
)

if not exist "%ninjapath%" (
    echo Error: Ninja not found at "%ninjapath%".
    exit /b 1
)

where jar >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo Error: jar command not found in PATH. Ensure JDK is installed.
    exit /b 1
)

rem ============================================
rem Create base directories
rem ============================================
if not exist build-natives mkdir build-natives
if not exist dist mkdir dist

rem ============================================
rem Clean mode
rem ============================================
if "%clean%"=="TRUE" (
    echo Cleaning build artifacts...
    if exist build-natives rmdir /s /q build-natives
    if exist android-backend\libs rmdir /s /q android-backend\libs
    if exist forcex\build\libs rmdir /s /q forcex\build\libs
    if exist windows-backend\build\libs rmdir /s /q windows-backend\build\libs
    echo Clean complete.
    if "%reconfig%"=="FALSE" if "%android%"=="FALSE" exit /b 0
)

rem ============================================
rem Android build
rem ============================================
if "%android%"=="TRUE" goto :BUILD_ANDROID

rem ============================================
rem Windows build (default)
rem ============================================
goto :BUILD_WINDOWS

rem ============================================
rem Help
rem ============================================
:SHOW_HELP
echo Usage: build.bat [options]
echo.
echo Options:
echo   --android              Build for Android
echo   --ndk-path PATH        Android NDK path (or set ANDROID_NDK env var)
echo   --platform API         Android platform target (default: latest)
echo   --oal VALUE            OpenAL option (default: off)
echo   --reconfig             Force reconfigure CMake
echo   --clean                Clean build artifacts before building
echo   --help                 Show this help message
echo.
echo Examples:
echo   build.bat --android --ndk-path C:\Android\ndk --platform android-34
echo   build.bat --clean --reconfig
echo   build.bat
exit /b 0

rem ============================================
rem Android build logic
rem ============================================
:BUILD_ANDROID
if not "%ANDROID_NDK%"=="" (
    set "ndkpath=%ANDROID_NDK%"
)

if "%ndkpath%"=="" (
    echo Error: ANDROID_NDK environment variable not detected. Please specify --ndk-path.
    exit /b 1
)

if not exist "%ndkpath%\build\cmake\android.toolchain.cmake" (
    echo Error: Android NDK not found at "%ndkpath%".
    exit /b 1
)

if "%platform%"=="" set "platform=latest"
if "%oal%"=="" set "oal=off"

set "abis=armeabi-v7a arm64-v8a x86 x86_64"

echo ============================================
echo Android Build Configuration
echo ============================================
echo NDK Path:      %ndkpath%
echo Ninja Path:    %ninjapath%
echo Platform:      %platform%
echo OAL:           %oal%
echo.

if not exist android-backend\libs mkdir android-backend\libs

for %%a in (%abis%) do (
    echo ----------------------------------------
    echo Building ABI: %%a
    echo ----------------------------------------

    set "build_dir=build-natives\build-%%a"
    set "lib_dir=android-backend\libs\%%a"

    if not exist "!build_dir!" mkdir "!build_dir!"
    if not exist "!lib_dir!" mkdir "!lib_dir!"

    if "%reconfig%"=="TRUE" (
        if exist "!build_dir!" rmdir /s /q "!build_dir!"
        mkdir "!build_dir!"
    )

    if not exist "!build_dir!\build.ninja" (
        cmake -S . -B "!build_dir!" -DFX_OAL=%oal% -DCMAKE_TOOLCHAIN_FILE="%ndkpath%/build/cmake/android.toolchain.cmake" -DANDROID_ABI=%%a -DANDROID_NDK="%ndkpath%" -DANDROID_PLATFORM=%platform% -DCMAKE_MAKE_PROGRAM="%ninjapath%" -GNinja
        if !ERRORLEVEL! neq 0 (
            echo Error: CMake configuration failed for %%a.
            exit /b 1
        )
    )

    cmake --build "!build_dir!" --config Release
    if !ERRORLEVEL! neq 0 (
        echo Error: Build failed for %%a.
        exit /b 1
    )

    for /f "delims=" %%f in ('dir /a-d /b /s "!build_dir!\bin\*.so"') do (
        copy /V "%%f" "!lib_dir!\" >nul 2>&1
    )
)

echo ============================================
echo Running Gradle assembleRelease...
echo ============================================
gradlew.bat android-backend:assembleRelease
if %ERRORLEVEL% neq 0 (
    echo Error: Gradle build failed.
    exit /b 1
)

echo ============================================
echo Packaging Android distribution...
echo ============================================
if not exist dist\android mkdir dist\android

for /f "delims=" %%f in ('dir /a-d /b /s "android-backend\build\outputs\*-release.aar"') do (
    copy /V "%%f" "dist\android" >nul 2>&1
)

if exist "forcex\build\libs\forcex.jar" (
    copy /V "forcex\build\libs\forcex.jar" "dist\android" >nul 2>&1
) else (
    echo Warning: forcex.jar not found. Skipping copy.
)
exit /b 0

rem ============================================
rem Windows build logic
rem ============================================
:BUILD_WINDOWS
if not exist build-natives\windows mkdir build-natives\windows
if not exist dist\windows mkdir dist\windows
if not exist dist\windows\data mkdir dist\windows\data
if not exist dist\windows\libs mkdir dist\windows\libs

echo ============================================
echo Windows Build Configuration
echo ============================================
echo.

if "%reconfig%"=="TRUE" (
    if exist build-natives\windows rmdir /s /q build-natives\windows
    mkdir build-natives\windows
)

if not exist build-natives\windows\build.ninja (
    cmake -S . -B build-natives\windows
    if %ERRORLEVEL% neq 0 (
        echo Error: CMake configuration failed.
        exit /b 1
    )
)

cmake --build build-natives\windows --config Release
if %ERRORLEVEL% neq 0 (
    echo Error: Native build failed.
    exit /b 1
)

copy build-natives\windows\Release\fxcore.dll dist\windows >nul 2>&1

for /f "delims=" %%f in ('dir /a-d /b /s "windows-backend\libs\*.jar"') do (
    copy /V "%%f" "dist\windows\libs" >nul 2>&1
)

echo ============================================
echo Running Gradle assemble...
echo ============================================
gradlew.bat windows-backend:assemble
if %ERRORLEVEL% neq 0 (
    echo Error: Gradle build failed.
    exit /b 1
)

echo ============================================
echo Packaging Windows distribution...
echo ============================================
if not exist "forcex\build\libs\forcex.jar" (
    echo forcex.jar not found. Creating from compiled classes...
    if not exist "forcex\build\libs" mkdir "forcex\build\libs"
    if not exist "forcex\build\classes" (
        echo Error: Compiled classes not found at forcex\build\classes.
        exit /b 1
    )
    jar cvf "forcex\build\libs\forcex.jar" -C "forcex\build\classes" .
    if %ERRORLEVEL% neq 0 (
        echo Error: Failed to create forcex.jar.
        exit /b 1
    )
)

copy /V "forcex\build\libs\forcex.jar" "dist\windows\libs" >nul 2>&1

if exist "windows-backend\build\libs\forcex-windows-backend.jar" (
    copy /V "windows-backend\build\libs\forcex-windows-backend.jar" "dist\windows\libs" >nul 2>&1
) else (
    echo Warning: forcex-windows-backend.jar not found. Skipping copy.
)

echo ============================================
echo Running Gradle copyAssets...
echo ============================================
gradlew.bat windows-backend:copyAssets
if %ERRORLEVEL% neq 0 (
    echo Error: Gradle copyAssets failed.
    exit /b 1
)
exit /b 0
