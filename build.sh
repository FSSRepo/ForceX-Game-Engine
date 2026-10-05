#!/bin/bash

android=FALSE
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
    esac
    shift
done

# Interactive mode when no CLI arguments are provided
if [[ $# -eq 0 ]]; then
    echo "=== ForceX Engine - Interactive build ==="
    echo "  1) Linux (default)"
    echo "  2) Android"
    read -r -p "Select target platform [1]: " opt
    if [[ "$opt" == "2" ]]; then
        android=TRUE
    fi
    read -r -p "Force CMake reconfigure? [y/N]: " rc
    if [[ "$rc" =~ ^[Yy]$ ]]; then
        reconfig=TRUE
    fi
    if [[ "$android" == "TRUE" ]]; then
        read -r -p "Android NDK path [${ANDROID_NDK:-not set}]: " ndk
        ndkpath="${ndk:-$ANDROID_NDK}"
        read -r -p "Android platform [latest]: " platform
        platform="${platform:-latest}"
        read -r -p "Enable OpenAL audio (--oal) [off]: " oal_in
        oal="${oal_in:-off}"
    fi
    echo "=========================================="
fi

mkdir -p build-natives
mkdir -p dist

if [[ "$android" == "TRUE" ]]; then
    if [[ -n "$ANDROID_NDK" ]]; then
        ndkpath="$ANDROID_NDK"
    elif [[ -z "$ndkpath" ]]; then
        echo "Error: ANDROID_NDK environment variable not detected, please specify it as an argument --ndk-path"
        exit 1
    fi

    echo "Android NDK Path: $ndkpath"
    echo "Ninja Generator Path: $ninjapath"

    mkdir -p android-backend/libs
    platform=${platform:-latest}
    oal=${oal:-off}
    echo "Android Platform Target: $platform"

    abis=(armeabi-v7a arm64-v8a x86 x86_64)

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

    echo "Packaging Android distribution..."
    mkdir -p dist/android
    find android-backend/build/outputs -name "*.aar" -exec cp -v {} dist/android \;
    cp -v forcex/build/libs/forcex.jar dist/android || true
else
    # Linux desktop build
    if [[ -z "$JAVA_HOME" ]]; then
        echo "Error: JAVA_HOME environment variable not detected"
        exit 1
    fi

    mkdir -p build-natives/linux dist/linux/data dist/linux/libs

    if [[ ! -f "build-natives/linux/build.ninja" ]] || [[ "$reconfig" == "TRUE" ]]; then
        cmake -S . -B build-natives/linux \
            -DCMAKE_BUILD_TYPE=Release \
            -DCMAKE_CXX_FLAGS="-I$JAVA_HOME/include -I$JAVA_HOME/include/linux" \
            -DCMAKE_MAKE_PROGRAM="$ninjapath" -GNinja
    fi

    cmake --build build-natives/linux --config Release
    find build-natives/linux -name "*.so" -exec cp -v {} dist/linux \;

    find desktop-backend/libs -name "*.jar" -exec cp -v {} dist/linux/libs \;

    ./gradlew desktop-backend:assemble

    if [[ ! -f "forcex/build/libs/forcex.jar" ]]; then
        echo "forcex.jar not found. Creating with compiled files."
        mkdir -p "forcex/build/libs"
        jar cvf "forcex/build/libs/forcex.jar" -C "forcex/build/classes" .
        if [[ $? -ne 0 ]]; then
            echo "Error creating forcex.jar"
            exit 1
        fi
    fi

    echo "Packaging Linux distribution..."
    cp -v forcex/build/libs/forcex.jar dist/linux/libs || true
    cp -v desktop-backend/build/libs/forcex-desktop-backend.jar dist/linux/libs || true
    cp -rv forcex/src/main/resources/* dist/linux/data/ || true
fi
