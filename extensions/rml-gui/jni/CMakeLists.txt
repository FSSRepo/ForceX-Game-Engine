cmake_minimum_required(VERSION 3.16)

# ============================================================================
#  fxrmlui: bindings nativos de RmlUi para ForceX
#
#  Las dependencias nativas NO se descargan ni compilan aquí: son artefactos
#  prebuilt creados por los scripts de deps/:
#    deps/freetype/build-freetype.sh
#    deps/lunasvg/build-lunasvg.sh   (lunasvg + plutovg -> plugin SVG)
#    deps/rlottie/build-rlottie.sh   (plugin Lottie)
# ============================================================================

set(FX_DEPS_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../deps")

if(ANDROID)
    set(FX_DEPS_SUBDIR "android/${ANDROID_ABI}")
elseif(WIN32)
    set(FX_DEPS_SUBDIR "windows")
else()
    set(FX_DEPS_SUBDIR "linux")
endif()

if(WIN32)
    set(FX_LIB_PREFIX "")
    set(FX_LIB_SUFFIX ".lib")
else()
    set(FX_LIB_PREFIX "lib")
    set(FX_LIB_SUFFIX ".a")
endif()

# fx_imported_static(<target> <librería> <directorio de includes> <script>)
macro(fx_imported_static target lib_path include_dir build_script)
    if(NOT EXISTS "${lib_path}")
        message(FATAL_ERROR
            "No se encontró la dependencia prebuilt: ${lib_path}\n"
            "Compílala primero con:\n"
            "  extensions/rml-gui/deps/${build_script}")
    endif()
    add_library(${target} STATIC IMPORTED GLOBAL)
    set_target_properties(${target} PROPERTIES
        IMPORTED_LOCATION "${lib_path}"
        INTERFACE_INCLUDE_DIRECTORIES "${include_dir}"
    )
endmacro()

# ----------------------------------------------------------------- FreeType ----

set(FX_FREETYPE_OUT "${FX_DEPS_DIR}/freetype/out/${FX_DEPS_SUBDIR}")
fx_imported_static(Freetype::Freetype
    "${FX_FREETYPE_OUT}/lib/${FX_LIB_PREFIX}freetype${FX_LIB_SUFFIX}"
    "${FX_FREETYPE_OUT}/include/freetype2"
    "freetype/build-freetype.sh --platform <windows|linux|android>")

# ------------------------------------------------------- lunasvg + plutovg ----

set(FX_LUNASVG_OUT "${FX_DEPS_DIR}/lunasvg/out/${FX_DEPS_SUBDIR}")

fx_imported_static(lunasvg::lunasvg
    "${FX_LUNASVG_OUT}/lib/${FX_LIB_PREFIX}lunasvg${FX_LIB_SUFFIX}"
    "${FX_LUNASVG_OUT}/include/lunasvg"
    "lunasvg/build-lunasvg.sh --platform <windows|linux|android>")

fx_imported_static(plutovg::plutovg
    "${FX_LUNASVG_OUT}/lib/${FX_LIB_PREFIX}plutovg${FX_LIB_SUFFIX}"
    "${FX_LUNASVG_OUT}/include/plutovg"
    "lunasvg/build-lunasvg.sh --platform <windows|linux|android>")

set_target_properties(lunasvg::lunasvg PROPERTIES
    INTERFACE_LINK_LIBRARIES plutovg::plutovg
    INTERFACE_COMPILE_DEFINITIONS LUNASVG_BUILD_STATIC)
set_target_properties(plutovg::plutovg PROPERTIES
    INTERFACE_COMPILE_DEFINITIONS PLUTOVG_BUILD_STATIC)

# ------------------------------------------------------------------ rlottie ----

set(FX_RLOTTIE_OUT "${FX_DEPS_DIR}/rlottie/out/${FX_DEPS_SUBDIR}")

fx_imported_static(rlottie::rlottie
    "${FX_RLOTTIE_OUT}/lib/${FX_LIB_PREFIX}rlottie${FX_LIB_SUFFIX}"
    "${FX_RLOTTIE_OUT}/include"
    "rlottie/build-rlottie.sh --platform <windows|linux|android>")

if(WIN32)
    # rlottie.h usa __declspec(dllimport) salvo que RLOTTIE_BUILD esté definido.
    set_target_properties(rlottie::rlottie PROPERTIES
        INTERFACE_COMPILE_DEFINITIONS RLOTTIE_BUILD)
elseif(NOT ANDROID)
    # En Linux/macOS pthread es una librería aparte; en Android lo aporta libc.
    set_target_properties(rlottie::rlottie PROPERTIES
        INTERFACE_LINK_LIBRARIES pthread)
endif()

# ------------------------------------------------------------------ RmlUi ----

# RmlUi se compila estático; sus .a no deben mezclarse con los .so del engine,
# por eso los archivos se emiten en un subdirectorio lib.
set(CMAKE_ARCHIVE_OUTPUT_DIRECTORY "${CMAKE_CURRENT_BINARY_DIR}/lib")

set(BUILD_SHARED_LIBS OFF CACHE BOOL "RmlUi estático dentro de fxrmlui" FORCE)
set(RMLUI_SAMPLES OFF CACHE BOOL "" FORCE)
set(RMLUI_FONT_ENGINE "freetype" CACHE STRING "" FORCE)
set(RMLUI_LUA_BINDINGS OFF CACHE BOOL "" FORCE)
set(RMLUI_HARFBUZZ_SAMPLE OFF CACHE BOOL "" FORCE)
set(RMLUI_SVG_PLUGIN ON CACHE BOOL "" FORCE)
set(RMLUI_LOTTIE_PLUGIN ON CACHE BOOL "" FORCE)

add_subdirectory("${CMAKE_CURRENT_SOURCE_DIR}/../RmlUi" "${CMAKE_CURRENT_BINARY_DIR}/rmlui")

# --------------------------------------------------------------- fxrmlui ----

add_library(fxrmlui SHARED
    bindings/fx_file_interface.cpp
    bindings/fx_gl.cpp
    bindings/fx_module.cpp
    bindings/fx_render_interface.cpp
    bindings/fx_system_interface.cpp
    bindings/rmlui_jni.cpp
)

target_compile_features(fxrmlui PRIVATE cxx_std_17)
set_target_properties(fxrmlui PROPERTIES OUTPUT_NAME "fxrmlui")

target_link_libraries(fxrmlui PRIVATE
    RmlUi::RmlUi
    Freetype::Freetype
    lunasvg::lunasvg
    rlottie::rlottie
)

# OpenGL del sistema: GLES 2.0 en Android (enlazado por el NDK) y el GL del
# escritorio en desktop. En Windows/Linux el renderer resuelve en runtime las
# funciones de OpenGL 2.0+ (shaders, VBO, VAO) con un contexto activo.
if(ANDROID)
    target_link_libraries(fxrmlui PRIVATE GLESv2 EGL log)
elseif(WIN32)
    target_link_libraries(fxrmlui PRIVATE opengl32)
else()
    find_library(FX_OPENGL_LIBRARY NAMES GL)
    if(FX_OPENGL_LIBRARY)
        target_link_libraries(fxrmlui PRIVATE ${FX_OPENGL_LIBRARY})
    else()
        message(WARNING "fxrmlui: no se encontró libGL; el renderer no podrá cargar funciones OpenGL")
    endif()
    target_link_libraries(fxrmlui PRIVATE ${CMAKE_DL_LIBS})
endif()

if(WIN32)
    # El generador de Visual Studio añade /Release a esta ruta, quedando junto a
    # fxcore.dll en build-natives/<target>/Release.
    set_target_properties(fxrmlui PROPERTIES
        RUNTIME_OUTPUT_DIRECTORY "${CMAKE_BINARY_DIR}"
        ARCHIVE_OUTPUT_DIRECTORY "${CMAKE_CURRENT_BINARY_DIR}/lib"
    )
else()
    set_target_properties(fxrmlui PROPERTIES
        POSITION_INDEPENDENT_CODE ON
    )
endif()
