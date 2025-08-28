@echo off
set "android=FALSE"
set "dist=FALSE"
set ninjapath=%~dp0ninja
set "reconfig=FALSE"
set "src="

:GETOPS
 if /I "%~1" == "--ndk-path" set ndkpath=%2& shift
 if /I "%~1" == "--platform" set platform=%2& shift
 if /I "%~1" == "--oal" set oal=%2& shift
 if /I "%~1" == "--reconfig" set "reconfig=TRUE"
 if /I "%~1" == "--android" set "android=TRUE"
 if /I "%~1" == "--dist" set "dist=TRUE"
 shift
if not (%1)==() goto GETOPS

if not exist build-natives (
    mkdir build-natives
)

if not exist dist (
    mkdir dist
)

if "%android%"=="TRUE" (
    if not "%ANDROID_NDK%"=="" (
        set "ndkpath=%ANDROID_NDK%"
    ) else (
        if "%ndkpath%"=="" (
            echo Error: ANDROID_NDK environment variable not detected, please specify it as argument --ndk-path
            exit /b
        )
    )

    echo Android NDK Path: %ndkpath%
    echo Ninja Generator Path: %ninjapath%
    
    if not exist android-backend\libs (
        mkdir android-backend\libs
    )

    if "%platform%"=="" (
        set platform=android-35
    )

    if "%oal%"=="" (
        set oal=off
    )

    echo Android Platform Target: %platform%

    set "abis=armeabi-v7a arm64-v8a x86 x86_64"

    if "%dist%" == "FALSE" (
        for %%a in (%abis%) do (
            echo Android ABI path: build\build-%%a
            if not exist build-natives\build-%%a (
                mkdir build-natives\build-%%a
            )

            if not exist android-backend\libs\%%a (
                mkdir android-backend\libs\%%a
            )

            if not exist build-natives\build-%%a\build.ninja (
                cmake -S . -B build-natives\build-%%a -DFX_OAL=%oal% -DCMAKE_TOOLCHAIN_FILE=%ndkpath%/build/cmake/android.toolchain.cmake -DANDROID_ABI=%%a -DANDROID_NDK=%ndkpath% -DANDROID_PLATFORM=%platform% -DCMAKE_MAKE_PROGRAM=%ninjapath% -GNinja
            )

            if "%reconfig%" == "TRUE" (
                del /q /s build-natives\build-%%a
                cmake -S . -B build-natives\build-%%a -DFX_OAL=%oal% -DCMAKE_TOOLCHAIN_FILE=%ndkpath%/build/cmake/android.toolchain.cmake -DANDROID_ABI=%%a -DANDROID_NDK=%ndkpath% -DANDROID_PLATFORM=%platform% -DCMAKE_MAKE_PROGRAM=%ninjapath% -GNinja
            )

            cmake --build build-natives\build-%%a --config Release

            for /f "delims=" %%f in ('dir /a-d /b /s "build-natives\build-%%a\bin\*.so"') do (
                copy /V "%%f" "android-backend\libs\%%a\" 2>nul
            )
        )

        gradlew android-backend:assembleRelease
    ) else (
        if not exist dist\android (
            mkdir dist\android
        )

        for /f "delims=" %%f in ('dir /a-d /b /s "android-backend\build\outputs\*.aar"') do (
            copy /V "%%f" "dist\android" 2>nul
        )

        copy /V "forcex\build\libs\forcex.jar" "dist\android" 2>nul
    )
) else (
    if not exist build-natives\windows (
        mkdir build-natives\windows
    )

    if not exist dist\windows (
        mkdir dist\windows
    )

    if not exist dist\windows\data (
        mkdir dist\windows\data
    )

    if not exist dist\windows\libs (
        mkdir dist\windows\libs
    )

    if not exist build-natives\windows\build.ninja (
        cmake -S . -B build-natives\windows
    )

    if "%reconfig%" == "TRUE" (
        cmake -S . -B build-natives\windows
    )
    
    if "%dist%" == "TRUE" (
        if not exist "forcex\build\libs\forcex.jar" (
            echo forcex.jar not found. creating with compiled files
            :: Crea la carpeta si no existe
            if not exist "forcex\build\libs" mkdir "forcex\build\libs"
            :: Crea el JAR con los archivos compilados
            jar cvf "forcex\build\libs\forcex.jar" -C "forcex\build\classes" .
            if %ERRORLEVEL% neq 0 (
                echo Error creating forcex.jar
                exit /b 1
            )
        )
        copy /V "forcex\build\libs\forcex.jar" "dist\windows\libs" 2>nul
        copy /V "windows-backend\build\libs\forcex-windows-backend.jar" "dist\windows\libs" 2>nul
        gradlew windows-backend:copyAssets
    ) else (
        cmake --build build-natives\windows --config Release
        copy build-natives\windows\Release\fxcore.dll dist\windows

        for /f "delims=" %%f in ('dir /a-d /b /s "windows-backend\libs\*.jar"') do (
            copy /V "%%f" "dist\windows\libs" 2>nul
        )

        gradlew windows-backend:assemble
    )
)
