# Plan: Extensión `rml-ui-gui` (RmlUi + Java bindings para ForceX)

## Objetivo

Crear una extensión `extensions/rml-ui-gui` que integre [RmlUi](https://github.com/mikke89/RmlUi) (HTML/CSS para UIs de juegos) al motor ForceX:

- API Java (`com.forcex.rmlui`) usable desde juegos como la GUI actual.
- Bindings JNI nativos sobre RmlUi 6.4 (estático, sin Lua/SVG/Lottie).
- RenderInterface propio que **reutiliza el contexto y pipeline GL del engine** (no crea ventana propia como el backend GLFW de referencia).
- FreeType compilado por **script propio** (Windows/Linux/Android) en lugar de `FetchContent` de CMake.
- Referencia de implementación: `C:\proyectos\cpp\rml-test` (RmlUi 6.4 + freetype 2.13.2 + backend GL3/GLFW).

---

## 1. Estructura del módulo

```
extensions/rml-ui-gui/
├── build.gradle                  # jar 'forcex-rmlui-gui', depende de :forcex
├── src/com/forcex/rmlui/         # API Java
│   ├── RmlUI.java                # facada: init/shutdown/loadFont/loadDocument/update/render
│   ├── RmlDocument.java          # wrapper de ElementDocument (show/hide/reload)
│   ├── RmlContext.java           # contexto por ventana/surface
│   ├── RmlEvent.java             # eventos RML (click, submit, etc.)
│   ├── RmlDataModel.java         # data model (bindings de datos RML)
│   └── RmlUIInput.java           # traducción InputListener ForceX → RmlUi
├── jni/
│   ├── rmlui/                    # fuente RmlUi 6.4 (copia o git submodule)
│   ├── bindings/
│   │   ├── rmlui_jni.cpp         # JNI: Java_com_forcex_rmlui_*
│   │   ├── fx_render_interface.cpp/.h  # RenderInterface sobre GL del engine
│   │   ├── fx_file_interface.cpp/.h    # FileInterface → delega a FX.fs (Java)
│   │   └── fx_system_interface.cpp/.h  # SystemInterface (tiempo/log)
│   └── Android.mk / CMakeLists.txt     # build nativo
└── src/main/resources/
    ├── fonts/LatoLatin-Regular.ttf     # fuente por defecto (de RmlUi assets)
    └── rml/                            # estilos base opcionales (.rml/.rcss)
```

Registro igual que `extensions:gui` y `extensions:gtasdk`: entrada en `settings.gradle` + bloque `project(":extensions:rml-ui-gui")` en el `build.gradle` raíz.

---

## 2. FreeType: script de build multiplataforma (sin CMake FetchContent)

**Problema**: la referencia usa `FetchContent` que descarga freetype en configure-time. Queremos control explícito y cacheado.

**Script**: `deps/freetype/build-freetype.sh` (Linux/Android) y `deps/freetype/build-freetype.bat` (Windows), o un único `build-freetype.sh` bash para los tres (Git Bash en Windows ya es requisito del repo por `build.sh`).

Versiones fijadas: `FREETYPE_VERSION=2.13.2`, URL `https://download.savannah.gnu.org/releases/freetype/freetype-2.13.2.tar.xz`, cache en `deps/freetype/src/` (no commiteado, `.gitignore`), artefactos en `deps/freetype/out/<plataforma>/`.

Flags comunes (como la referencia): `FT_DISABLE_ZLIB/BZIP2/PNG/HARFBUZZ/BROTLI = TRUE` → freetype autónomo, sin dependencias externas.

```bash
deps/freetype/build-freetype.sh [--platform windows|linux|android] [--ndk PATH] [--reconfig]
```

- **Linux**: `make setup ansi` / compilar con `gcc -fPIC` → `out/linux/libfreetype.a` + headers.
- **Windows**: compilar con el mismo toolchain del engine (`cl` o clang) → `out/windows/freetype.lib`.
- **Android**: cross-compilar las 4 ABIs (`armeabi-v7a arm64-v8a x86 x86_64`) con el toolchain del NDK (`--ndk-path` o `$ANDROID_NDK`) → `out/android/<abi>/libfreetype.a`.
- Nunca descargar dos veces: si `deps/freetype/src/freetype-2.13.2` existe, saltar descarga.

**CMake consumidor** (`extensions/rml-ui-gui/jni/CMakeLists.txt`): `find_library` / `IMPORTED` location contra `deps/freetype/out/...` según plataforma, sin `FetchContent`.

---

## 3. Natives: CMake y targets

Extender el `CMakeLists.txt` raíz del engine (igual que `fxcore`) con opción `FX_RMLUI` (default OFF):

- `add_subdirectory(extensions/rml-ui-gui/jni)` condicionado por `FX_RMLUI`.
- RmlUi estático: `set(BUILD_SHARED_LIBS OFF)`, `RMLUI_SAMPLES/LUA/LOTTIE/SVG OFF`, `RMLUI_FONT_ENGINE=freetype`.
- Target `fxrmlui` (SHARED):
  - compila `bindings/*.cpp` + linka `RmlUi::RmlUi` + freetype prebuilt.
  - salida junto a fxcore (`build-natives/<target>/bin`) para que el flujo de copiado a `android-backend/libs/<abi>` existente funcione sin cambios.
- Linux: include JNI (`JAVA_HOME/include/linux`); Windows: `win32`.

---

## 4. Renderer: reutilizar el pipeline del engine

La referencia crea su propia ventana GLFW + backend GL3. **Aquí no**: RmlUi renderiza en el contexto GL ya activo de ForceX.

`FxRenderInterface : public Rml::RenderInterface` (basado en `RmlUi_Renderer_GL2`, que es compatible GLES2 y funciona en Android):

- **Shaders**: el GL2 backend de RmlUi usa su propio shader de color/textura. Para "reusar el renderer", los uniforms/matrices se alimentan del estado del engine (viewport/proyección ortográfica con `FX.gpu.getWidth/Height`) y las texturas se generan con la convención del engine (`glGenTexture`, filter LINEAR, wrap CLAMP_TO_EDGE — mismo perfil que `Texture.load`).
- **Compiling shader GLSL**: versión 110 en desktop GL, `#version 100` + `precision mediump` en GLES (Android) — switch por `FX.gpu.isOpenGLES()` expuesto al nativo vía JNI o definida de compilación (`-DFX_GLES` en el target Android).
- **Render loop**: `RmlUI.render()` se llama desde el game loop de ForceX *después* de la escena 3D y *antes* del swap:
  - desktop: al final de `GLRenderer.loop()` o desde el `Game.render()` del usuario.
  - Android: mismo punto en `GLRenderer` de `android-backend`.
  - Estado GL: guardar/restaurar depth test, blend, scissor y viewport alrededor del render de RmlUi.
- **Texturas RML→engine**: `RenderInterface::LoadTexture` puede mapearse a `com.forcex.core.gpu.Texture` (vía JNI callback) para que compartan atlas/estilo del engine; fase 1: texturas internas del RenderInterface, fase 2: unificación.

---

## 5. Bindings Java ↔ JNI

Superficie mínima (crecer por demanda), estilo `CoreJni` del engine:

| Java (`RmlUI`) | JNI (`rmlui_jni.cpp`) | RmlUi |
|---|---|---|
| `init(int width, int height)` | `Java_..._RmlUI_init` | `Rml::Initialise`, crea Context + interfaces |
| `loadFontAsset(String path)` | `LoadFontFace` | via `FxFileInterface` → `FX.fs` |
| `loadDocument(String rml)` | `LoadDocument` | `context->LoadDocument` |
| `update()` / `render()` | `Update/Render` | `context->Update/Render` |
| `onTouch(x,y,type,pointer)` | `ProcessMouse...` | mapeo `EventType.TOUCH_*` → mouse events de RmlUi |
| `onKeyEvent(key,down)` | `ProcessKey` | traducción `com.forcex.app.Key` → `Rml::Input::KeyIdentifier` |
| `resize(w,h)` | `SetDimensions` | `context->SetDimensions` |
| `shutdown()` | `Rml::Shutdown` | |

- **Eventos**: `FxSystemInterface`/instancer de eventos → callback JNI a `RmlEvent` Java (listeners registrados en `RmlDocument`).
- **FileInterface**: `FxFileInterface` implementa `Open/Read/Seek/Tell/Close` haciendo callbacks JNI a `FX.fs.open(...)` — así `.rml/.rcss/.ttf` cargan desde `data/`, paquetes FX o assets Android sin duplicar lógica.
- El `.so`/`.dll` se carga en un `static { System.loadLibrary("fxrmlui"); }` en `RmlUI`.

---

## 6. Script de build integrado

- `build.sh` / `build.bat`: nueva opción `--rmlui` (y pregunta en el modo interactivo: "Compilar extensión RmlUi? [y/N]").
- Orden: `deps/freetype/build-freetype.sh <plataforma>` → CMake con `-DFX_RMLUI=on` → copiar `libfxrmlui.so` junto a fxcore (Android: a `android-backend/libs/<abi>`; desktop: `dist/<plataforma>/`).
- Gradle: `extensions:rml-ui-gui` se ensambla con el jar; si el juego usa la extensión, añade `implementation project(":extensions:rml-ui-gui")` (igual que `extensions:gui`).

---

## 7. Fases de implementación

1. **Setup**: estructura de carpetas, `build.gradle`, registro en settings/root build, copia de RmlUi 6.4 a `jni/rmlui`, `.gitignore` de deps.
2. **FreeType**: `build-freetype.sh` (descarga cacheada + Linux + Android ABIs; Windows vía Git Bash/`cl`) y verificación de artefactos.
3. **CMake nativo**: target `fxrmlui` estático→shared, freetype prebuilt, 3 plataformas compilando.
4. **Renderer + System/File interfaces** nativos (sin bindings aún), probados con un documento `.rml` hardcodeado.
5. **Bindings JNI + API Java** (tabla de la sección 5) + carga de librería.
6. **Input**: `RmlUIInput` traduciendo `InputListener` de ForceX (touch, teclado, scroll).
7. **Ejemplo**: pantalla demo en `examples/` con HUD `.rml/.rcss` estilo invaders de la referencia, fuente Lato.
8. **Integración build**: `--rmlui` en scripts, copiado de artefactos, modo interactivo.

## Decisiones tomadas

- RmlUi **6.4 estático** (misma versión que la referencia), plugins Lua/SVG/Lottie desactivados.
- FreeType **2.13.2**, dependencias internas desactivadas (zlib/bzip2/png/harfbuzz/brotli OFF).
- Backend de render basado en **GL2/GLES2** (no GL3) para que el mismo código sirva en Android GLES2 y desktop.
- Sin ventana propia: se dibuja sobre el contexto activo del engine (como el ejemplo `glfw_cube` de la referencia, pero sin GLFW).
