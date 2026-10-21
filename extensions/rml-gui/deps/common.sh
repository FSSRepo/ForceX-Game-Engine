#!/usr/bin/env bash
#
# Shared utilities for the scripts in extensions/rml-gui/deps/*.
# It is loaded with `source`; the script that uses it must define DEP_NAME and the
# dep_build_windows / dep_build_linux / dep_build_android functions.
#
# Common options accepted:
#   --platform windows|linux|android|all
#   --ndk-path PATH  (or ANDROID_NDK variable)
#   --android-platform API
#   --abis "a,b,c"
#   --reconfig / --clean / --help

set -euo pipefail

DEPS_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$DEPS_DIR/../../.." && pwd)"

FX_PLATFORM=""
FX_NDK_PATH="${ANDROID_NDK:-}"
FX_ANDROID_PLATFORM="android-21"
FX_ANDROID_ABIS=(armeabi-v7a arm64-v8a x86 x86_64)
FX_RECONFIG="FALSE"
FX_CLEAN="FALSE"
FX_NINJA_ARGS=()

FX_DEP_SRC=""
FX_DEP_BUILD=""
FX_DEP_OUT=""

fx_info() {
    echo "[$DEP_NAME] $*"
}

fx_error() {
    echo "[$DEP_NAME] ERROR: $*" >&2
    exit 1
}

fx_detect_platform() {
    case "$(uname -s)" in
        MINGW*|MSYS*|CYGWIN*|Windows_NT*|Windows*) echo "windows" ;;
        *) echo "linux" ;;
    esac
}

fx_usage() {
    cat <<EOF
Uso: ./build-$DEP_NAME.sh [opciones]

Opciones:
  --platform NAME         windows | linux | android | all (por defecto: plataforma actual)
  --ndk-path PATH         Ruta del NDK para Android (o variable ANDROID_NDK)
  --android-platform API  Plataforma Android (por defecto: $FX_ANDROID_PLATFORM)
  --abis "A,B,C"          ABIs Android (por defecto: ${FX_ANDROID_ABIS[*]})
  --reconfig              Fuerza la reconfiguración de CMake
  --clean                 Elimina build/out antes de compilar
  --help                  Muestra esta ayuda
EOF
}

fx_parse_args() {
    while [[ $# -gt 0 ]]; do
        case "$1" in
            --platform|--platform=*)
                if [[ "$1" == *=* ]]; then FX_PLATFORM="${1#*=}"; else shift; FX_PLATFORM="${1:-}"; fi
                ;;
            --ndk-path|--ndk|--ndk-path=*|--ndk=*)
                if [[ "$1" == *=* ]]; then FX_NDK_PATH="${1#*=}"; else shift; FX_NDK_PATH="${1:-}"; fi
                ;;
            --android-platform|--android-platform=*)
                if [[ "$1" == *=* ]]; then FX_ANDROID_PLATFORM="${1#*=}"; else shift; FX_ANDROID_PLATFORM="${1:-}"; fi
                ;;
            --abis|--abis=*)
                if [[ "$1" == *=* ]]; then
                    IFS=', ' read -r -a FX_ANDROID_ABIS <<< "${1#*=}"
                else
                    shift
                    IFS=', ' read -r -a FX_ANDROID_ABIS <<< "${1:-}"
                fi
                ;;
            --reconfig) FX_RECONFIG="TRUE" ;;
            --clean) FX_CLEAN="TRUE" ;;
            --help|-h) fx_usage; exit 0 ;;
            *) fx_error "opción desconocida: $1 (usa --help)" ;;
        esac
        shift
    done
}

fx_init() {
    : "${DEP_NAME:?DEP_NAME no definido antes de fx_init}"

    fx_parse_args "$@"

    if [[ -z "$FX_PLATFORM" ]]; then
        FX_PLATFORM="$(fx_detect_platform)"
    fi

    case "$FX_PLATFORM" in
        windows|linux|android|all) ;;
        *) fx_error "plataforma no soportada: $FX_PLATFORM" ;;
    esac

    # Ninja: the one from PATH is preferred and, if it does not exist, the one that ships with the repo.
    FX_NINJA_ARGS=()
    if command -v ninja >/dev/null 2>&1; then
        FX_NINJA_ARGS=(-G Ninja)
    elif [[ -f "$REPO_ROOT/ninja.exe" ]]; then
        FX_NINJA_ARGS=(-G Ninja "-DCMAKE_MAKE_PROGRAM=$REPO_ROOT/ninja.exe")
    fi

    FX_DEP_SRC="$DEPS_DIR/$DEP_NAME/src"
    FX_DEP_BUILD="$DEPS_DIR/$DEP_NAME/build"
    FX_DEP_OUT="$DEPS_DIR/$DEP_NAME/out"
    mkdir -p "$FX_DEP_SRC" "$FX_DEP_BUILD" "$FX_DEP_OUT"
}

# fx_download <url> <target-file>: downloads only if the file does not exist.
fx_download() {
    local url="$1"
    local target="$2"

    if [[ -f "$target" ]]; then
        fx_info "archivo ya descargado: $(basename "$target")"
        return
    fi

    fx_info "descargando $(basename "$target")..."
    if curl -fL --retry 3 --connect-timeout 30 -o "$target.part" "$url"; then
        mv "$target.part" "$target"
    else
        rm -f "$target.part"
        fx_error "no se pudo descargar $url"
    fi
}

# fx_extract <tar.gz> <expected-target-directory>
fx_extract() {
    local archive="$1"
    local expected="$2"

    if [[ -d "$expected" ]]; then
        fx_info "fuente ya extraída: $(basename "$expected")"
        return
    fi

    fx_info "extrayendo $(basename "$archive")..."
    tar -xzf "$archive" -C "$(dirname "$expected")"
    [[ -d "$expected" ]] || fx_error "la extracción no generó $expected"
}

# fx_configure_and_build <name> <build-dir> <out-dir> [cmake args...]
fx_configure_and_build() {
    local name="$1"
    local build_dir="$2"
    local out_dir="$3"
    local source_dir="$4"
    shift 4

    if [[ "$FX_CLEAN" == "TRUE" ]]; then
        rm -rf "$build_dir" "$out_dir"
    fi
    if [[ "$FX_RECONFIG" == "TRUE" ]]; then
        rm -rf "$build_dir"
    fi

    if [[ ! -f "$build_dir/CMakeCache.txt" ]]; then
        fx_info "configurando $name..."
        cmake -S "$source_dir" -B "$build_dir" \
            -DCMAKE_BUILD_TYPE=Release \
            -DBUILD_SHARED_LIBS=OFF \
            -DCMAKE_POSITION_INDEPENDENT_CODE=ON \
            "$@"
    else
        fx_info "reutilizando configuración de $name (usa --reconfig para regenerarla)"
    fi

    fx_info "compilando $name..."
    cmake --build "$build_dir" --config Release --parallel

    fx_info "instalando $name en $out_dir..."
    cmake --install "$build_dir" --config Release --prefix "$out_dir"
}

fx_verify_artifact() {
    local name="$1"
    local out_dir="$2"
    local lib="$3"

    [[ -f "$out_dir/lib/$lib" ]] || fx_error "no se generó $out_dir/lib/$lib"
    fx_info "OK $name -> $out_dir/lib/$lib"
}

# fx_run: runs the dep_build_* functions according to --platform.
fx_run() {
    case "$FX_PLATFORM" in
        windows) dep_build_windows ;;
        linux)   dep_build_linux ;;
        android) dep_build_android ;;
        all)
            case "$(fx_detect_platform)" in
                windows) dep_build_windows ;;
                *)       dep_build_linux ;;
            esac
            if [[ -n "$FX_NDK_PATH" ]]; then
                dep_build_android
            else
                fx_info "ANDROID_NDK no definido: se omite la compilación para Android"
            fi
            ;;
    esac
    fx_info "listo."
}

# fx_android_toolchain: validates the NDK and leaves the toolchain path in FX_ANDROID_TOOLCHAIN.
fx_android_toolchain() {
    [[ -n "$FX_NDK_PATH" ]] || fx_error "Android requiere --ndk-path o la variable ANDROID_NDK"
    FX_ANDROID_TOOLCHAIN="$FX_NDK_PATH/build/cmake/android.toolchain.cmake"
    [[ -f "$FX_ANDROID_TOOLCHAIN" ]] || fx_error "no se encontró el toolchain del NDK en $FX_ANDROID_TOOLCHAIN"
}

fx_android_cmake_args() {
    FX_ANDROID_ARGS=(
        "-DCMAKE_TOOLCHAIN_FILE=$FX_ANDROID_TOOLCHAIN"
        "-DANDROID_NDK=$FX_NDK_PATH"
        "-DANDROID_ABI=$1"
        "-DANDROID_PLATFORM=$FX_ANDROID_PLATFORM"
    )
}

# fx_run_android_build <name> <source-dir> [extra cmake args...]
fx_run_android_build() {
    local name="$1"
    local source_dir="$2"
    shift 2

    local abi
    for abi in "${FX_ANDROID_ABIS[@]}"; do
        fx_info "-- $name / $abi --"
        [[ ${#FX_NINJA_ARGS[@]} -gt 0 ]] || fx_error "Android requiere Ninja (PATH o ninja.exe en la raíz del repo)"
        fx_android_cmake_args "$abi"
        fx_configure_and_build "$name/$abi" "$FX_DEP_BUILD/android/$abi" "$FX_DEP_OUT/android/$abi" \
            "$source_dir" "${FX_NINJA_ARGS[@]}" "${FX_ANDROID_ARGS[@]}" "$@"
    done
}
