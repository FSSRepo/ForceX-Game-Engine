@echo off
setlocal EnableDelayedExpansion

rem ============================================
rem Default values
rem ============================================
set "android=FALSE"
set "clean=FALSE"
set "reconfig=FALSE"
set "help=FALSE"
set "rmlui=FALSE"
set "ninjapath=%~dp0ninja.exe"
set "platform="
set "oal="
set "src="

rem ============================================
rem Interactive mode when no arguments provided
rem ============================================
if "%~1"=="" (
    echo === ForceX Engine - Interactive build ===
    echo   1^) Desktop ^(Windows^) ^(default^)
    echo   2^) Android
    set /p "opt=Select target platform [1]: "
    if "!opt!"=="2" set "android=TRUE"

    set /p "rc=Force CMake reconfigure? [y/N]: "
    if /I "!rc!"=="y" set "reconfig=TRUE"

    set /p "rml=Build RmlUi extension (--rmlui)? [y/N]: "
    if /I "!rml!"=="y" set "rmlui=TRUE"

    if "!android!"=="TRUE" (
        set /p "ndkpath=Android NDK path [%ANDROID_NDK%]: "
        if "!ndkpath!"=="" set "ndkpath=%ANDROID_NDK%"
        set /p "platform=Android platform [latest]: "
        if "!platform!"=="" set "platform=latest"
        set /p "oal=Enable OpenAL audio (--oal) [off]: "
        if "!oal!"=="" set "oal=off"
    )
    echo ============================================
)

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
if /I "%~1"=="--rmlui" set "rmlui=TRUE"
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
    if exist desktop-backend\build\libs rmdir /s /q desktop-backend\build\libs
    echo Clean complete.
    if "%reconfig%"=="FALSE" if "%android%"=="FALSE" exit /b 0
)

rem ============================================
rem Android build
rem ============================================
if "%android%"=="TRUE" goto :BUILD_ANDROID

rem ============================================
rem Desktop (Windows) build (default)
rem ============================================
goto :BUILD_DESKTOP

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
echo   --rmlui                Build the RmlUi GUI extension (and its dependencies)
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
rem RmlUi native dependencies (FreeType, lunasvg, rlottie)
rem Se compilan directamente con CMake desde las fuentes ya extraídas en
rem deps/*/src, sin depender de bash (misma configuración que los scripts .sh).
rem ============================================
:BUILD_RMLUI_DEPS
if "%rmlui%"=="FALSE" exit /b 0

echo ============================================
echo Building RmlUi native dependencies...
echo ============================================
if /I "%~1"=="android" (
    call :BUILD_RMLUI_DEPS_ANDROID
) else (
    call :BUILD_RMLUI_DEPS_WINDOWS
)
if %ERRORLEVEL% neq 0 (
    echo Error: failed to build the RmlUi native dependencies.
    exit /b 1
)
exit /b 0

:BUILD_RMLUI_DEPS_WINDOWS
set "dep_base=extensions\rml-gui\deps"

echo -- FreeType 2.13.2 --
cmake -S "%dep_base%\freetype\src\freetype-2.13.2" -B "%dep_base%\freetype\build\windows" -A x64 ^
    -DCMAKE_BUILD_TYPE=Release -DBUILD_SHARED_LIBS=OFF ^
    -DFT_DISABLE_ZLIB=TRUE -DFT_DISABLE_BZIP2=TRUE -DFT_DISABLE_PNG=TRUE ^
    -DFT_DISABLE_HARFBUZZ=TRUE -DFT_DISABLE_BROTLI=TRUE ^
    -DCMAKE_POLICY_VERSION_MINIMUM=3.5
if %ERRORLEVEL% neq 0 exit /b 1
cmake --build "%dep_base%\freetype\build\windows" --config Release --parallel
if %ERRORLEVEL% neq 0 exit /b 1
cmake --install "%dep_base%\freetype\build\windows" --config Release --prefix "%dep_base%\freetype\out\windows"
if %ERRORLEVEL% neq 0 exit /b 1

echo -- lunasvg 3.5.0 + plutovg 1.3.0 --
cmake -S "%dep_base%\lunasvg\src\lunasvg-3.5.0" -B "%dep_base%\lunasvg\build\windows" -A x64 ^
    -DCMAKE_BUILD_TYPE=Release -DBUILD_SHARED_LIBS=OFF ^
    -DUSE_SYSTEM_PLUTOVG=OFF -DLUNASVG_BUILD_EXAMPLES=OFF -DPLUTOVG_BUILD_EXAMPLES=OFF ^
    -DLUNASVG_DISABLE_LOAD_SYSTEM_FONTS=ON
if %ERRORLEVEL% neq 0 exit /b 1
cmake --build "%dep_base%\lunasvg\build\windows" --config Release --parallel
if %ERRORLEVEL% neq 0 exit /b 1
cmake --install "%dep_base%\lunasvg\build\windows" --config Release --prefix "%dep_base%\lunasvg\out\windows"
if %ERRORLEVEL% neq 0 exit /b 1

echo -- rlottie 0.2 --
cmake -S "%dep_base%\rlottie\src\rlottie-0.2" -B "%dep_base%\rlottie\build\windows" -A x64 ^
    -DCMAKE_BUILD_TYPE=Release -DBUILD_SHARED_LIBS=OFF ^
    -DLIB_INSTALL_DIR=lib -DLOTTIE_TEST=OFF -DLOTTIE_MODULE=OFF -DLOTTIE_CCACHE=OFF ^
    -DCMAKE_POLICY_VERSION_MINIMUM=3.5 ^
    "-DCMAKE_CXX_FLAGS=/D_DISABLE_CONSTEXPR_MUTEX_CONSTRUCTOR"
if %ERRORLEVEL% neq 0 exit /b 1
cmake --build "%dep_base%\rlottie\build\windows" --config Release --parallel
if %ERRORLEVEL% neq 0 exit /b 1
cmake --install "%dep_base%\rlottie\build\windows" --config Release --prefix "%dep_base%\rlottie\out\windows"
if %ERRORLEVEL% neq 0 exit /b 1
exit /b 0

:BUILD_RMLUI_DEPS_ANDROID
if "%ndkpath%"=="" (
    echo Error: --ndk-path is required to build the RmlUi dependencies for Android.
    exit /b 1
)
for %%a in (armeabi-v7a arm64-v8a x86 x86_64) do (
    call :BUILD_RMLUI_DEPS_ANDROID_ABI %%a
    if !ERRORLEVEL! neq 0 exit /b 1
)
exit /b 0

:BUILD_RMLUI_DEPS_ANDROID_ABI
setlocal
set "dep_base=extensions\rml-gui\deps"
set "abi=%~1"
set "ndk_args=-DCMAKE_TOOLCHAIN_FILE=%ndkpath%/build/cmake/android.toolchain.cmake -DANDROID_NDK=%ndkpath% -DANDROID_ABI=%abi% -DANDROID_PLATFORM=%platform% -DCMAKE_MAKE_PROGRAM=%ninjapath% -GNinja"

echo -- FreeType / %abi% --
cmake -S "%dep_base%\freetype\src\freetype-2.13.2" -B "%dep_base%\freetype\build\android\%abi%" -DCMAKE_BUILD_TYPE=Release -DBUILD_SHARED_LIBS=OFF -DFT_DISABLE_ZLIB=TRUE -DFT_DISABLE_BZIP2=TRUE -DFT_DISABLE_PNG=TRUE -DFT_DISABLE_HARFBUZZ=TRUE -DFT_DISABLE_BROTLI=TRUE -DCMAKE_POLICY_VERSION_MINIMUM=3.5 %ndk_args%
if !ERRORLEVEL! neq 0 ( endlocal & exit /b 1 )
cmake --build "%dep_base%\freetype\build\android\%abi%" --config Release --parallel
if !ERRORLEVEL! neq 0 ( endlocal & exit /b 1 )
cmake --install "%dep_base%\freetype\build\android\%abi%" --config Release --prefix "%dep_base%\freetype\out\android\%abi%"
if !ERRORLEVEL! neq 0 ( endlocal & exit /b 1 )

echo -- lunasvg / %abi% --
cmake -S "%dep_base%\lunasvg\src\lunasvg-3.5.0" -B "%dep_base%\lunasvg\build\android\%abi%" -DCMAKE_BUILD_TYPE=Release -DBUILD_SHARED_LIBS=OFF -DUSE_SYSTEM_PLUTOVG=OFF -DLUNASVG_BUILD_EXAMPLES=OFF -DPLUTOVG_BUILD_EXAMPLES=OFF -DLUNASVG_DISABLE_LOAD_SYSTEM_FONTS=ON %ndk_args%
if !ERRORLEVEL! neq 0 ( endlocal & exit /b 1 )
cmake --build "%dep_base%\lunasvg\build\android\%abi%" --config Release --parallel
if !ERRORLEVEL! neq 0 ( endlocal & exit /b 1 )
cmake --install "%dep_base%\lunasvg\build\android\%abi%" --config Release --prefix "%dep_base%\lunasvg\out\android\%abi%"
if !ERRORLEVEL! neq 0 ( endlocal & exit /b 1 )

echo -- rlottie / %abi% --
cmake -S "%dep_base%\rlottie\src\rlottie-0.2" -B "%dep_base%\rlottie\build\android\%abi%" -DCMAKE_BUILD_TYPE=Release -DBUILD_SHARED_LIBS=OFF -DLIB_INSTALL_DIR=lib -DLOTTIE_TEST=OFF -DLOTTIE_MODULE=OFF -DLOTTIE_CCACHE=OFF -DCMAKE_POLICY_VERSION_MINIMUM=3.5 %ndk_args%
if !ERRORLEVEL! neq 0 ( endlocal & exit /b 1 )
cmake --build "%dep_base%\rlottie\build\android\%abi%" --config Release --parallel
if !ERRORLEVEL! neq 0 ( endlocal & exit /b 1 )
cmake --install "%dep_base%\rlottie\build\android\%abi%" --config Release --prefix "%dep_base%\rlottie\out\android\%abi%"
if !ERRORLEVEL! neq 0 ( endlocal & exit /b 1 )

endlocal & exit /b 0

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

call :BUILD_RMLUI_DEPS android
if %ERRORLEVEL% neq 0 exit /b 1

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
        cmake -S . -B "!build_dir!" -DCMAKE_BUILD_TYPE=Release -DFX_OAL=%oal% -DFX_RMLUI=%rmlui% -DCMAKE_TOOLCHAIN_FILE="%ndkpath%/build/cmake/android.toolchain.cmake" -DANDROID_ABI=%%a -DANDROID_NDK="%ndkpath%" -DANDROID_PLATFORM=%platform% -DCMAKE_MAKE_PROGRAM="%ninjapath%" -GNinja
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
call gradlew.bat android-backend:assembleRelease
if %ERRORLEVEL% neq 0 (
    echo Error: Gradle build failed.
    exit /b 1
)

if "%rmlui%"=="TRUE" (
    call gradlew.bat :extensions:rml-gui:jar
    if !ERRORLEVEL! neq 0 (
        echo Error: Gradle build of the RmlUi extension failed.
        exit /b 1
    )
)

echo ============================================
echo Packaging Android distribution...
echo ============================================
if not exist dist\android mkdir dist\android

for /f "delims=" %%f in ('dir /a-d /b /s "android-backend\build\outputs\*-release.aar"') do (
    copy /V "%%f" "dist\android" >nul 2>&1
)

if "%rmlui%"=="TRUE" (
    copy /V "extensions\rml-gui\build\libs\forcex-rmlui-gui.jar" "dist\android" >nul 2>&1
)

if exist "forcex\build\libs\forcex.jar" (
    copy /V "forcex\build\libs\forcex.jar" "dist\android" >nul 2>&1
) else (
    echo Warning: forcex.jar not found. Skipping copy.
)
exit /b 0

rem ============================================
rem Desktop build logic
rem ============================================
:BUILD_DESKTOP
if not exist build-natives\windows mkdir build-natives\windows
if not exist dist\desktop mkdir dist\desktop
if not exist dist\desktop\data mkdir dist\desktop\data
if not exist dist\desktop\libs mkdir dist\desktop\libs

echo ============================================
echo Desktop Build Configuration
echo ============================================
echo.

if "%reconfig%"=="TRUE" (
    if exist build-natives\windows rmdir /s /q build-natives\windows
    mkdir build-natives\windows
)

call :BUILD_RMLUI_DEPS windows
if %ERRORLEVEL% neq 0 exit /b 1

if not exist build-natives\windows\build.ninja (
    cmake -S . -B build-natives\windows -DFX_RMLUI=%rmlui%
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

copy build-natives\windows\Release\fxcore.dll dist\desktop >nul 2>&1
if "%rmlui%"=="TRUE" copy build-natives\windows\Release\fxrmlui.dll dist\desktop >nul 2>&1

for /f "delims=" %%f in ('dir /a-d /b /s "desktop-backend\libs\*.jar"') do (
    copy /V "%%f" "dist\desktop\libs" >nul 2>&1
)

echo ============================================
echo Running Gradle assemble...
echo ============================================
call gradlew.bat desktop-backend:assemble
if %ERRORLEVEL% neq 0 (
    echo Error: Gradle build failed.
    exit /b 1
)

if "%rmlui%"=="TRUE" (
    call gradlew.bat :extensions:rml-gui:jar
    if !ERRORLEVEL! neq 0 (
        echo Error: Gradle build of the RmlUi extension failed.
        exit /b 1
    )
)

echo ============================================
echo Packaging Desktop distribution...
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

copy /V "forcex\build\libs\forcex.jar" "dist\desktop\libs" >nul 2>&1

if exist "desktop-backend\build\libs\forcex-desktop-backend.jar" (
    copy /V "desktop-backend\build\libs\forcex-desktop-backend.jar" "dist\desktop\libs" >nul 2>&1
) else (
    echo Warning: forcex-desktop-backend.jar not found. Skipping copy.
)

if "%rmlui%"=="TRUE" (
    copy /V "extensions\rml-gui\build\libs\forcex-rmlui-gui.jar" "dist\desktop\libs" >nul 2>&1
)

echo ============================================
echo Running Gradle copyAssets...
echo ============================================
call gradlew.bat desktop-backend:copyAssets
if %ERRORLEVEL% neq 0 (
    echo Error: Gradle copyAssets failed.
    exit /b 1
)
exit /b 0
