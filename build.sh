#!/bin/bash
set -e

android=FALSE
ninjapath="${NINJA_PATH:-}"
cmakepath="${CMAKE_PATH:-}"
reconfig=FALSE
rmlui=FALSE
src=""
has_args=FALSE

while [[ "$1" != "" ]]; do
    has_args=TRUE
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
        --rmlui ) rmlui=TRUE
                  ;;
    esac
    shift
done

# Interactive mode when no CLI arguments are provided
if [[ "$has_args" == "FALSE" ]]; then
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
    read -r -p "Build RmlUi extension (--rmlui)? [y/N]: " rm
    if [[ "$rm" =~ ^[Yy]$ ]]; then
        rmlui=TRUE
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

# Prefer the Ninja binary bundled with the Android SDK's CMake package. For
# desktop builds, or SDKs without CMake installed, use an explicit NINJA_PATH
# or Ninja from PATH. This also avoids trying to execute the Windows-only
# ninja.exe that may be present in a source checkout.
resolve_ninja() {
    if [[ -n "$ninjapath" ]]; then
        if [[ ! -x "$ninjapath" ]]; then
            echo "Error: NINJA_PATH is not executable: $ninjapath"
            exit 1
        fi
    fi

    local sdk_root="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"
    local candidate
    if [[ -z "$ninjapath" && "$android" == "TRUE" && -n "$sdk_root" && -d "$sdk_root/cmake" ]]; then
        for candidate in "$sdk_root"/cmake/*/bin/ninja; do
            if [[ -x "$candidate" ]]; then
                ninjapath="$candidate"
                if [[ -z "$cmakepath" && -x "${candidate%/ninja}/cmake" ]]; then
                    cmakepath="${candidate%/ninja}/cmake"
                fi
            fi
        done
    fi

    if [[ -z "$ninjapath" ]] && command -v ninja >/dev/null 2>&1; then
        ninjapath="$(command -v ninja)"
    elif [[ -z "$ninjapath" && -x "$(dirname "$0")/ninja" ]]; then
        ninjapath="$(dirname "$0")/ninja"
    elif [[ -z "$ninjapath" ]]; then
        echo "Error: Ninja not found. Install Android SDK CMake or Ninja, or set NINJA_PATH."
        exit 1
    fi

    if [[ -n "$cmakepath" && ! -x "$cmakepath" ]]; then
        echo "Error: CMAKE_PATH is not executable: $cmakepath"
        exit 1
    elif [[ -z "$cmakepath" ]]; then
        cmakepath="$(command -v cmake)"
    fi
    if [[ -z "$cmakepath" || ! -x "$cmakepath" ]]; then
        echo "Error: CMake not found. Install CMake or set CMAKE_PATH."
        exit 1
    fi
}

resolve_ninja
echo "CMake: $cmakepath"
echo "Ninja: $ninjapath"

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

    if [[ "$rmlui" == "TRUE" ]]; then
        echo "Building RmlUi native dependencies..."
        abi_list="$(IFS=,; echo "${abis[*]}")"
        for dep in freetype lunasvg rlottie; do
            bash "extensions/rml-gui/deps/$dep/build-$dep.sh" --platform android \
                --ndk-path "$ndkpath" --android-platform "$platform" --abis "$abi_list" || exit 1
        done
    fi

    for abi in "${abis[@]}"; do
        echo "Android ABI path: build/build-$abi"
        mkdir -p "build-natives/build-$abi"
        mkdir -p "android-backend/libs/$abi"

        if [[ ! -f "build-natives/build-$abi/build.ninja" ]] || [[ "$reconfig" == "TRUE" ]]; then
            rm -rf "build-natives/build-$abi"
            "$cmakepath" -S . -B "build-natives/build-$abi" -DCMAKE_BUILD_TYPE=Release -DFX_OAL="$oal" -DFX_RMLUI="$rmlui" \
                -DCMAKE_TOOLCHAIN_FILE="$ndkpath/build/cmake/android.toolchain.cmake" \
                -DANDROID_ABI="$abi" -DANDROID_NDK="$ndkpath" -DANDROID_PLATFORM="$platform" \
                -DCMAKE_MAKE_PROGRAM="$ninjapath" -GNinja
        fi

        "$cmakepath" --build "build-natives/build-$abi" --config Release
        find "build-natives/build-$abi/bin" -name "*.so" -exec cp -v {} "android-backend/libs/$abi/" \;
    done

    bash ./gradlew android-backend:assembleRelease

    if [[ "$rmlui" == "TRUE" ]]; then
        bash ./gradlew :extensions:rml-gui:jar
    fi

    echo "Packaging Android distribution..."
    mkdir -p dist/android
    find android-backend/build/outputs -name "*.aar" -exec cp -v {} dist/android \;
    if [[ "$rmlui" == "TRUE" ]]; then
        cp -v extensions/rml-gui/build/libs/forcex-rmlui-gui.jar dist/android || true
    fi
    cp -v forcex/build/libs/forcex.jar dist/android || true
else
    # Linux desktop build
    if [[ -z "$JAVA_HOME" ]]; then
        echo "Error: JAVA_HOME environment variable not detected"
        exit 1
    fi

    mkdir -p build-natives/linux dist/linux/data dist/linux/libs

    if [[ "$rmlui" == "TRUE" ]]; then
        echo "Building RmlUi native dependencies..."
        for dep in freetype lunasvg rlottie; do
            bash "extensions/rml-gui/deps/$dep/build-$dep.sh" --platform linux || exit 1
        done
    fi

    if [[ ! -f "build-natives/linux/build.ninja" ]] || [[ "$reconfig" == "TRUE" ]]; then
        "$cmakepath" -S . -B build-natives/linux \
            -DCMAKE_BUILD_TYPE=Release \
            -DFX_RMLUI="$rmlui" \
            -DCMAKE_CXX_FLAGS="-I$JAVA_HOME/include -I$JAVA_HOME/include/linux" \
            -DCMAKE_MAKE_PROGRAM="$ninjapath" -GNinja
    fi

    "$cmakepath" --build build-natives/linux --config Release
    find build-natives/linux -name "*.so" -exec cp -v {} dist/linux \;

    find desktop-backend/libs -name "*.jar" -exec cp -v {} dist/linux/libs \;

    bash ./gradlew desktop-backend:assemble

    if [[ "$rmlui" == "TRUE" ]]; then
        bash ./gradlew :extensions:rml-gui:jar
    fi

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
    if [[ "$rmlui" == "TRUE" ]]; then
        cp -v extensions/rml-gui/build/libs/forcex-rmlui-gui.jar dist/linux/libs || true
    fi
    cp -rv forcex/src/main/resources/* dist/linux/data/ || true
fi
