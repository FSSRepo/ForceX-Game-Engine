#!/bin/bash

android=FALSE
dist=FALSE
ninjapath="$(dirname "$0")/ninja"
reconfig=FALSE
src=""

while [[ "$1" != "" ]]; do
    case $1 in
        --ndk-path ) shift
                    ndkpath=$1
                    ;;
        --platform ) shift
                    platform=$1
                    ;;
        --oal ) shift
                 oal=$1
                 ;;
        --reconfig ) reconfig=TRUE
                    ;;
        --android ) android=TRUE
                    ;;
        --dist ) dist=TRUE
                    ;;
    esac
    shift
done

mkdir -p build-natives\mkdir -p dist

if [[ "$android" == "TRUE" ]]; then
    if [[ -n "$ANDROID_NDK" ]]; then
        ndkpath="$ANDROID_NDK"
    elif [[ -z "$ndkpath" ]]; then
        echo "Error: ANDROID_NDK environment variable not detected, please specify it as an argument"
        exit 1
    fi

    echo "Android NDK Path: $ndkpath"
    echo "Ninja Generator Path: $ninjapath"

    mkdir -p android-backend/libs
    platform=${platform:-android-21}
    oal=${oal:-off}
    echo "Android Platform Target: $platform"

    abis=(armeabi-v7a arm64-v8a x86 x86_64)

    if [[ "$dist" == "FALSE" ]]; then
        for abi in "${abis[@]}"; do
            echo "Android ABI path: build/build-$abi"
            mkdir -p "build-natives/build-$abi"
            mkdir -p "android-backend/libs/$abi"

            if [[ ! -f "build-natives/build-$abi/build.ninja" ]] || [[ "$reconfig" == "TRUE" ]]; then
                rm -rf "build-natives/build-$abi"
                cmake -S . -B "build-natives/build-$abi" -DFX_OAL="$oal" \
                    -DCMAKE_TOOLCHAIN_FILE="$ndkpath/build/cmake/android.toolchain.cmake" \
                    -DANDROID_ABI="$abi" -DANDROID_NDK="$ndkpath" -DANDROID_PLATFORM="$platform" \
                    -DCMAKE_MAKE_PROGRAM="$ninjapath" -GNinja
            fi

            cmake --build "build-natives/build-$abi" --config Release
            find "build-natives/build-$abi/bin" -name "*.so" -exec cp -v {} "android-backend/libs/$abi/" \;
        done
        ./gradlew android-backend:assembleRelease
    else
        mkdir -p dist/android
        find android-backend/build/outputs -name "*.aar" -exec cp -v {} dist/android \;
        cp -v forcex/build/libs/forcex.jar dist/android || true
    fi
else
    mkdir -p build-natives/windows dist/windows dist/windows/data dist/windows/libs

    if [[ ! -f "build-natives/windows/build.ninja" ]] || [[ "$reconfig" == "TRUE" ]]; then
        cmake -S . -B build-natives/windows
    fi
    
    if [[ "$dist" == "TRUE" ]]; then
        if [[ ! -f "forcex/build/libs/forcex.jar" ]]; then
            echo "forcex.jar not found. Creating with compiled files."
            mkdir -p "forcex/build/libs"
            jar cvf "forcex/build/libs/forcex.jar" -C "forcex/build/classes" .
            if [[ $? -ne 0 ]]; then
                echo "Error creating forcex.jar"
                exit 1
            fi
        fi
        cp -v forcex/build/libs/forcex.jar dist/windows/libs || true
        cp -v windows-backend/build/libs/forcex-windows-backend.jar dist/windows/libs || true
        ./gradlew windows-backend:copyAssets
    else
        cmake --build build-natives/windows --config Release
        cp -v build-natives/windows/Release/fxcore.dll dist/windows || true
        find windows-backend/libs -name "*.jar" -exec cp -v {} dist/windows/libs \;
        ./gradlew windows-backend:assemble
    fi
fi
