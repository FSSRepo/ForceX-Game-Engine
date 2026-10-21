#!/usr/bin/env bash
#
# Compila rlottie (plugin Lottie de RmlUi) para las plataformas de ForceX.
#
# Uso (Git Bash / Linux):
#   ./build-rlottie.sh --platform windows
#   ./build-rlottie.sh --platform linux
#   ./build-rlottie.sh --platform android --ndk-path /ruta/al/ndk
#
# Artefactos (por plataforma / ABI):
#   deps/rlottie/out/<plataforma>[/<abi>]/include/rlottie.h
#   deps/rlottie/out/<plataforma>[/<abi>]/lib/{librlottie.a,rlottie.lib}
#
# Se usa la versión 0.2 (API `loadFromData(std::string, key, resourcePath)` que
# consume RmlUi 6.4). El módulo de carga de imágenes por dlopen se desactiva:
# RmlUi sólo necesita Animation.

DEP_NAME="rlottie"

source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/../common.sh"

RLOTTIE_VERSION="0.2"
RLOTTIE_ARCHIVE="rlottie-$RLOTTIE_VERSION.tar.gz"
RLOTTIE_URL="https://github.com/Samsung/rlottie/archive/refs/tags/v$RLOTTIE_VERSION.tar.gz"

fx_init "$@"

RLOTTIE_SRC="$FX_DEP_SRC/rlottie-$RLOTTIE_VERSION"

CMAKE_OPTIONS=(
    -DLIB_INSTALL_DIR=lib
    -DLOTTIE_TEST=OFF
    -DLOTTIE_MODULE=OFF
    -DLOTTIE_CCACHE=OFF
    # rlottie declara CMake 3.3 como mínimo; CMake 4 rechaza versiones < 3.5.
    -DCMAKE_POLICY_VERSION_MINIMUM=3.5
)

prepare_source() {
    fx_download "$RLOTTIE_URL" "$FX_DEP_SRC/$RLOTTIE_ARCHIVE"
    fx_extract "$FX_DEP_SRC/$RLOTTIE_ARCHIVE" "$RLOTTIE_SRC"

    # rlottie sólo instala inc/rlottie*.h en <prefix>/include; el include que
    # espera RmlUi (`#include <rlottie.h>`) queda cubierto.
    [[ -f "$RLOTTIE_SRC/inc/rlottie.h" ]] || fx_error "fuente de rlottie incompleta en $RLOTTIE_SRC"
}

lib_name() {
    if [[ "$1" == "windows" ]]; then echo "rlottie.lib"; else echo "librlottie.a"; fi
}

build_platform() {
    local platform="$1"
    local out_dir="$FX_DEP_OUT/$platform"

    fx_configure_and_build "rlottie" "$FX_DEP_BUILD/$platform" "$out_dir" \
        "$RLOTTIE_SRC" "${FX_NINJA_ARGS[@]}" "${CMAKE_OPTIONS[@]}"

    fx_verify_artifact "rlottie" "$out_dir" "$(lib_name "$platform")"
    [[ -f "$out_dir/include/rlottie.h" ]] || fx_error "falta rlottie.h en $out_dir/include"
}

dep_build_windows() {
    fx_info "== rlottie $RLOTTIE_VERSION para Windows =="
    # Sin consola de Visual Studio no hay `cl` en PATH: usar el generador de VS.
    if [[ -z "${VCINSTALLDIR:-}" ]]; then
        FX_NINJA_ARGS=(-A x64)
    fi
    # MSVC >= 14.40 inicializa std::mutex en el constructor (constexpr), algo que
    # el msvcp140.dll del JDK (< 14.40) no soporta: _Mtx_lock falla con una
    # mutex sin inicializar. Con esta macro el constructor usa _Mtx_init_in_situ.
    CMAKE_OPTIONS+=("-DCMAKE_CXX_FLAGS=/D_DISABLE_CONSTEXPR_MUTEX_CONSTRUCTOR")
    build_platform "windows"
}

dep_build_linux() {
    fx_info "== rlottie $RLOTTIE_VERSION para Linux =="
    build_platform "linux"
}

dep_build_android() {
    fx_android_toolchain
    fx_info "== rlottie $RLOTTIE_VERSION para Android =="

    local abi
    for abi in "${FX_ANDROID_ABIS[@]}"; do
        fx_info "-- rlottie / $abi --"
        [[ ${#FX_NINJA_ARGS[@]} -gt 0 ]] || fx_error "Android requiere Ninja (PATH o ninja.exe en la raíz del repo)"
        fx_android_cmake_args "$abi"

        # El NDK 26 obliga a NEON en armeabi-v7a y rlottie referencia asm de
        # pixman que no compila con el ensamblador de clang. Se desactiva la
        # macro __ARM_NEON__ en esa ABI para usar el camino escalar.
        local abi_arg=()
        if [[ "$abi" == "armeabi-v7a" ]]; then
            abi_arg=("-DCMAKE_CXX_FLAGS=-U__ARM_NEON__")
        fi

        fx_configure_and_build "rlottie/$abi" "$FX_DEP_BUILD/android/$abi" "$FX_DEP_OUT/android/$abi" \
            "$RLOTTIE_SRC" "${FX_NINJA_ARGS[@]}" "${FX_ANDROID_ARGS[@]}" "${abi_arg[@]}" "${CMAKE_OPTIONS[@]}"

        fx_verify_artifact "rlottie/$abi" "$FX_DEP_OUT/android/$abi" "librlottie.a"
        [[ -f "$FX_DEP_OUT/android/$abi/include/rlottie.h" ]] || fx_error "falta rlottie.h en android/$abi"
    done
}

prepare_source
fx_run
