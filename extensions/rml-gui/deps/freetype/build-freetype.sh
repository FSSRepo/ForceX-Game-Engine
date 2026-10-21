#!/usr/bin/env bash
#
# Compila FreeType 2.13.2 para las plataformas soportadas por ForceX.
#
# Uso (Git Bash / Linux / macOS):
#   ./build-freetype.sh --platform windows
#   ./build-freetype.sh --platform linux
#   ./build-freetype.sh --platform android --ndk-path /ruta/al/ndk
#   ./build-freetype.sh --platform all
#
# Artefactos:
#   deps/freetype/out/<plataforma>[/<abi>]/include/freetype2/...
#   deps/freetype/out/<plataforma>[/<abi>]/lib/libfreetype.a | freetype.lib
#
# La descarga se cachea en deps/freetype/src y la compilación en
# deps/freetype/build; ambos quedan fuera del control de versiones.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../../.." && pwd)"
SRC_DIR="$SCRIPT_DIR/src"
BUILD_DIR="$SCRIPT_DIR/build"
OUT_DIR="$SCRIPT_DIR/out"

FREETYPE_VERSION="2.13.2"
FREETYPE_ARCHIVE="freetype-$FREETYPE_VERSION.tar.gz"
FREETYPE_DIR_NAME="freetype-$FREETYPE_VERSION"
FREETYPE_URLS=(
    "https://download.savannah.gnu.org/releases/freetype/$FREETYPE_ARCHIVE"
    "https://downloads.sourceforge.net/project/freetype/freetype2/$FREETYPE_VERSION/$FREETYPE_ARCHIVE"
)

ANDROID_ABIS_DEFAULT=(armeabi-v7a arm64-v8a x86 x86_64)
ANDROID_PLATFORM_DEFAULT="android-21"

PLATFORM=""
NDK_PATH="${ANDROID_NDK:-}"
ANDROID_PLATFORM="$ANDROID_PLATFORM_DEFAULT"
ANDROID_ABIS=()
WINDOWS_ARCH="x64"
CLEAN="FALSE"
RECONFIG="FALSE"

COMMON_FLAGS=(
    "-DCMAKE_BUILD_TYPE=Release"
    "-DBUILD_SHARED_LIBS=OFF"
    "-DFT_DISABLE_ZLIB=TRUE"
    "-DFT_DISABLE_BZIP2=TRUE"
    "-DFT_DISABLE_PNG=TRUE"
    "-DFT_DISABLE_HARFBUZZ=TRUE"
    "-DFT_DISABLE_BROTLI=TRUE"
    # FreeType declara un mínimo de CMake antiguo; permite configurarlo con CMake >= 4.
    "-DCMAKE_POLICY_VERSION_MINIMUM=3.5"
)

info() {
    echo "[freetype] $*"
}

error() {
    echo "[freetype] ERROR: $*" >&2
    exit 1
}

usage() {
    cat <<EOF
Uso: ./build-freetype.sh [opciones]

Opciones:
  --platform NAME    windows | linux | android | all (por defecto: plataforma actual)
  --ndk-path PATH    Ruta del NDK para Android (o variable ANDROID_NDK)
  --android-platform API  Plataforma Android (por defecto: $ANDROID_PLATFORM_DEFAULT)
  --abis "A B C"     ABIs Android a compilar (por defecto: ${ANDROID_ABIS_DEFAULT[*]})
  --arch ARCH        Arquitectura Windows (por defecto: x64)
  --reconfig         Fuerza la reconfiguración de CMake
  --clean            Elimina src/build/out antes de compilar
  --help             Muestra esta ayuda

Ejemplos:
  ./build-freetype.sh --platform windows
  ./build-freetype.sh --platform android --ndk-path "C:/Android/ndk"
  ./build-freetype.sh --platform all --reconfig
EOF
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        --platform|--platform=*)
            if [[ "$1" == *=* ]]; then
                PLATFORM="${1#*=}"
            else
                shift
                PLATFORM="${1:-}"
            fi
            ;;
        --ndk-path|--ndk|--ndk-path=*|--ndk=*)
            if [[ "$1" == *=* ]]; then
                NDK_PATH="${1#*=}"
            else
                shift
                NDK_PATH="${1:-}"
            fi
            ;;
        --android-platform|--android-platform=*)
            if [[ "$1" == *=* ]]; then
                ANDROID_PLATFORM="${1#*=}"
            else
                shift
                ANDROID_PLATFORM="${1:-}"
            fi
            ;;
        --abis|--abis=*)
            if [[ "$1" == *=* ]]; then
                IFS=', ' read -r -a ANDROID_ABIS <<< "${1#*=}"
            else
                shift
                IFS=', ' read -r -a ANDROID_ABIS <<< "${1:-}"
            fi
            ;;
        --arch|--arch=*)
            if [[ "$1" == *=* ]]; then
                WINDOWS_ARCH="${1#*=}"
            else
                shift
                WINDOWS_ARCH="${1:-}"
            fi
            ;;
        --reconfig) RECONFIG="TRUE" ;;
        --clean) CLEAN="TRUE" ;;
        --help|-h)
            usage
            exit 0
            ;;
        *)
            error "opción desconocida: $1 (usa --help)"
            ;;
    esac
    shift
done

detect_platform() {
    case "$(uname -s)" in
        MINGW*|MSYS*|CYGWIN*|Windows_NT*|Windows*) echo "windows" ;;
        Darwin*) echo "macos" ;;
        *) echo "linux" ;;
    esac
}

if [[ -z "$PLATFORM" ]]; then
    PLATFORM="$(detect_platform)"
fi

if [[ ${#ANDROID_ABIS[@]} -eq 0 ]]; then
    ANDROID_ABIS=("${ANDROID_ABIS_DEFAULT[@]}")
fi

case "$PLATFORM" in
    windows|linux|android|all) ;;
    *) error "plataforma no soportada: $PLATFORM" ;;
esac

# Ninja: se prefiere el del PATH y, si no existe, el que acompaña al repo.
NINJA_ARGS=()
if command -v ninja >/dev/null 2>&1; then
    NINJA_ARGS=(-G Ninja)
elif [[ -f "$REPO_ROOT/ninja.exe" ]]; then
    NINJA_ARGS=(-G Ninja "-DCMAKE_MAKE_PROGRAM=$REPO_ROOT/ninja.exe")
fi

# ---------------------------------------------------------------- descarga ----

download() {
    local archive="$SRC_DIR/$FREETYPE_ARCHIVE"

    if [[ -f "$archive" ]]; then
        info "archivo ya descargado: $archive"
        return
    fi

    mkdir -p "$SRC_DIR"
    info "descargando FreeType $FREETYPE_VERSION..."

    local url
    for url in "${FREETYPE_URLS[@]}"; do
        info "  -> $url"
        if curl -fL --retry 3 --connect-timeout 30 -o "$archive.part" "$url"; then
            mv "$archive.part" "$archive"
            return
        fi
        rm -f "$archive.part"
    done

    error "no se pudo descargar $FREETYPE_ARCHIVE"
}

extract() {
    local target="$SRC_DIR/$FREETYPE_DIR_NAME"

    if [[ -d "$target" ]]; then
        info "fuente ya extraída: $target"
        return
    fi

    info "extrayendo $FREETYPE_ARCHIVE..."
    tar -xzf "$SRC_DIR/$FREETYPE_ARCHIVE" -C "$SRC_DIR"
    [[ -d "$target" ]] || error "la extracción no generó $target"
}

prepare_source() {
    download
    extract
}

# ------------------------------------------------------------------ build ----

# configure_and_build <nombre> <directorio-build> <directorio-salida> [args cmake...]
configure_and_build() {
    local name="$1"
    local build_dir="$2"
    local out_dir="$3"
    shift 3

    local source_dir="$SRC_DIR/$FREETYPE_DIR_NAME"

    if [[ "$CLEAN" == "TRUE" ]]; then
        rm -rf "$build_dir" "$out_dir"
    fi

    if [[ "$RECONFIG" == "TRUE" ]]; then
        rm -rf "$build_dir"
    fi

    if [[ ! -f "$build_dir/CMakeCache.txt" ]]; then
        info "configurando $name..."
        cmake -S "$source_dir" -B "$build_dir" \
            "${COMMON_FLAGS[@]}" \
            "$@"
    else
        info "reutilizando configuración de $name (usa --reconfig para regenerarla)"
    fi

    info "compilando $name..."
    cmake --build "$build_dir" --config Release --parallel

    info "instalando $name en $out_dir..."
    cmake --install "$build_dir" --config Release --prefix "$out_dir"
}

verify_artifact() {
    local name="$1"
    local out_dir="$2"
    local lib="$3"

    [[ -f "$out_dir/lib/$lib" ]] || error "no se generó $out_dir/lib/$lib"
    [[ -d "$out_dir/include/freetype2" ]] || error "no se generaron los headers en $out_dir/include/freetype2"
    info "OK $name -> $out_dir/lib/$lib"
}

build_windows() {
    info "== FreeType para Windows ($WINDOWS_ARCH) =="
    local build_dir="$BUILD_DIR/windows"
    local out_dir="$OUT_DIR/windows"
    local generator_args=()

    # Fuera de una consola de Visual Studio no hay `cl` en PATH, así que se usa el
    # generador de Visual Studio (el mismo que emplea build.bat para fxcore).
    if [[ -n "${VCINSTALLDIR:-}" ]] && command -v cl >/dev/null 2>&1 && [[ ${#NINJA_ARGS[@]} -gt 0 ]]; then
        generator_args=("${NINJA_ARGS[@]}")
    else
        generator_args=(-A "$WINDOWS_ARCH")
    fi

    configure_and_build "windows" "$build_dir" "$out_dir" "${generator_args[@]}"
    verify_artifact "windows" "$out_dir" "freetype.lib"
}

build_linux() {
    info "== FreeType para Linux =="
    local build_dir="$BUILD_DIR/linux"
    local out_dir="$OUT_DIR/linux"
    local generator_args=()

    generator_args=("${NINJA_ARGS[@]}")

    configure_and_build "linux" "$build_dir" "$out_dir" "${generator_args[@]}"
    verify_artifact "linux" "$out_dir" "libfreetype.a"
}

build_android() {
    [[ -n "$NDK_PATH" ]] || error "Android requiere --ndk-path o la variable ANDROID_NDK"

    local toolchain="$NDK_PATH/build/cmake/android.toolchain.cmake"
    [[ -f "$toolchain" ]] || error "no se encontró el toolchain del NDK en $toolchain"

    info "== FreeType para Android (NDK: $NDK_PATH) =="

    local abi
    for abi in "${ANDROID_ABIS[@]}"; do
        info "-- ABI $abi --"
        local build_dir="$BUILD_DIR/android/$abi"
        local out_dir="$OUT_DIR/android/$abi"
        local generator_args=("${NINJA_ARGS[@]}")

        if [[ ${#generator_args[@]} -eq 0 ]]; then
            error "Android requiere Ninja (colócalo en PATH o deja ninja.exe en la raíz del repo)"
        fi

        configure_and_build "android/$abi" "$build_dir" "$out_dir" \
            "${generator_args[@]}" \
            "-DCMAKE_TOOLCHAIN_FILE=$toolchain" \
            "-DANDROID_NDK=$NDK_PATH" \
            "-DANDROID_ABI=$abi" \
            "-DANDROID_PLATFORM=$ANDROID_PLATFORM"

        verify_artifact "android/$abi" "$out_dir" "libfreetype.a"
    done
}

mkdir -p "$SRC_DIR" "$BUILD_DIR" "$OUT_DIR"

case "$PLATFORM" in
    windows) prepare_source; build_windows ;;
    linux)   prepare_source; build_linux ;;
    android) prepare_source; build_android ;;
    all)
        prepare_source
        case "$(detect_platform)" in
            windows) build_windows ;;
            linux)   build_linux ;;
        esac
        if [[ -n "$NDK_PATH" ]]; then
            build_android
        else
            info "ANDROID_NDK no definido: se omite la compilación para Android"
        fi
        ;;
esac

info "listo."
