#!/usr/bin/env bash
#
# Compila lunasvg + plutovg (plugin SVG de RmlUi) para las plataformas de ForceX.
#
# Uso (Git Bash / Linux):
#   ./build-lunasvg.sh --platform windows
#   ./build-lunasvg.sh --platform linux
#   ./build-lunasvg.sh --platform android --ndk-path /ruta/al/ndk
#
# Artefactos (por plataforma / ABI):
#   deps/lunasvg/out/<plataforma>[/<abi>]/include/lunasvg/lunasvg.h
#   deps/lunasvg/out/<plataforma>[/<abi>]/include/plutovg/plutovg.h
#   deps/lunasvg/out/<plataforma>[/<abi>]/lib/{liblunasvg.a,libplutovg.a}
#
# lunasvg 3.x consume plutovg como submódulo; el script descarga ambos tarballs
# fijados por versión y coloca plutovg dentro del árbol de lunasvg.

DEP_NAME="lunasvg"

source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/../common.sh"

LUNASVG_VERSION="3.5.0"
PLUTOVG_VERSION="1.3.0"

LUNASVG_ARCHIVE="lunasvg-$LUNASVG_VERSION.tar.gz"
PLUTOVG_ARCHIVE="plutovg-$PLUTOVG_VERSION.tar.gz"
LUNASVG_URL="https://github.com/sammycage/lunasvg/archive/refs/tags/v$LUNASVG_VERSION.tar.gz"
PLUTOVG_URL="https://github.com/sammycage/plutovg/archive/refs/tags/v$PLUTOVG_VERSION.tar.gz"

fx_init "$@"

LUNASVG_SRC="$FX_DEP_SRC/lunasvg-$LUNASVG_VERSION"
PLUTOVG_SRC="$FX_DEP_SRC/plutovg-$PLUTOVG_VERSION"

prepare_source() {
    fx_download "$LUNASVG_URL" "$FX_DEP_SRC/$LUNASVG_ARCHIVE"
    fx_download "$PLUTOVG_URL" "$FX_DEP_SRC/$PLUTOVG_ARCHIVE"
    fx_extract "$FX_DEP_SRC/$LUNASVG_ARCHIVE" "$LUNASVG_SRC"
    fx_extract "$FX_DEP_SRC/$PLUTOVG_ARCHIVE" "$PLUTOVG_SRC"

    if [[ -f "$LUNASVG_SRC/plutovg/CMakeLists.txt" ]]; then
        fx_info "plutovg ya presente en el árbol de lunasvg"
    else
        fx_info "incorporando plutovg $PLUTOVG_VERSION al árbol de lunasvg..."
        rm -rf "$LUNASVG_SRC/plutovg"
        cp -R "$PLUTOVG_SRC" "$LUNASVG_SRC/plutovg"
    fi
}
LUNASVG_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# En Android plutovg debe usar pthread (ver patch-plutovg-android.sh).
patch_plutovg_android() {
    bash "$LUNASVG_DIR/patch-plutovg-android.sh" "$LUNASVG_SRC/plutovg/source/plutovg-font.c"         "$PLUTOVG_SRC/source/plutovg-font.c"
}

CMAKE_OPTIONS=(
    -DUSE_SYSTEM_PLUTOVG=OFF
    -DLUNASVG_BUILD_EXAMPLES=OFF
    -DPLUTOVG_BUILD_EXAMPLES=OFF
    -DLUNASVG_DISABLE_LOAD_SYSTEM_FONTS=ON
)

lib_name() {
    if [[ "$1" == "windows" ]]; then echo "$2.lib"; else echo "lib$2.a"; fi
}

build_platform() {
    local platform="$1"
    local out_dir="$FX_DEP_OUT/$platform"

    fx_configure_and_build "lunasvg" "$FX_DEP_BUILD/$platform" "$out_dir" \
        "$LUNASVG_SRC" "${FX_NINJA_ARGS[@]}" "${CMAKE_OPTIONS[@]}"

    fx_verify_artifact "lunasvg" "$out_dir" "$(lib_name "$platform" lunasvg)"
    fx_verify_artifact "plutovg" "$out_dir" "$(lib_name "$platform" plutovg)"
    [[ -f "$out_dir/include/lunasvg/lunasvg.h" ]] || fx_error "faltan headers de lunasvg en $out_dir"
    [[ -f "$out_dir/include/plutovg/plutovg.h" ]] || fx_error "faltan headers de plutovg en $out_dir"
}

dep_build_windows() {
    fx_info "== lunasvg $LUNASVG_VERSION + plutovg $PLUTOVG_VERSION para Windows =="
    # Sin consola de Visual Studio no hay `cl` en PATH: usar el generador de VS.
    if [[ -z "${VCINSTALLDIR:-}" ]]; then
        FX_NINJA_ARGS=(-A x64)
    fi
    build_platform "windows"
}

dep_build_linux() {
    fx_info "== lunasvg $LUNASVG_VERSION + plutovg $PLUTOVG_VERSION para Linux =="
    build_platform "linux"
}

dep_build_android() {
    fx_android_toolchain
    fx_info "== lunasvg $LUNASVG_VERSION + plutovg $PLUTOVG_VERSION para Android =="
    fx_run_android_build "lunasvg" "$LUNASVG_SRC" "${CMAKE_OPTIONS[@]}"

    local abi
    for abi in "${FX_ANDROID_ABIS[@]}"; do
        fx_verify_artifact "lunasvg/$abi" "$FX_DEP_OUT/android/$abi" "liblunasvg.a"
        fx_verify_artifact "plutovg/$abi" "$FX_DEP_OUT/android/$abi" "libplutovg.a"
    done
}

prepare_source
patch_plutovg_android
fx_run
