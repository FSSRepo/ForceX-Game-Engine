# ForceX Game Engine

ForceX is a cross-platform 2D/3D game engine written in Java with native backends for **Windows** and **Android**. It provides a complete framework for game development with graphics, audio, collisions, animation, post-processing effects, and a full GUI system.

---

## Key Features

- **Cross-platform**: Windows (7+, 32/64 bits) and Android (4.1+)
- **3D Graphics**: OpenGL-based renderer with shaders, lighting, shadows, water, skybox, and billboards
- **Post-processing**: Bloom, blur, brightness/contrast, FXAA, normal mapping, framebuffer effects
- **Particle System**: 2D/3D particle engine
- **Animation**: Skeleton-based animation with bones and keyframe tracks
- **Full GUI**: Rich widgets (buttons, lists, editors, keyboards, joysticks, dialogs, toast, and more)
- **Collision Detection**: Bounding boxes, spheres, meshes, and triangles
- **Audio**: OpenAL support (optional on Android) and WAV playback
- **Asset Management**: Custom package formats, texture compression (DXT, ETC1)
- **Networking**: HTTP download manager and property files
- **Threading**: TaskPool system for background jobs
- **3D Math**: Vectors, matrices, quaternions, planes, rays, and geometric utilities
- **GTA Extension**: RenderWare DFF/IFP file support via `extensions/gtasdk`
- **Headless desktop mode**: run the full game loop and OpenGL rendering without ever showing a window (`FXAppConfig.headless` or `-Dforcex.headless=true`); useful for autotests, CI and offscreen rendering

---

## Project Structure

| Module | Description |
|--------|-------------|
| `forcex/` | Core engine library (Java) |
| `forcex/jni/` | Native C++ code (texture compression, JNI bridge) |
| `android-backend/` | Android platform backend (OpenGL ES, input, audio) |
| `desktop-backend/` | Desktop platform backend (LWJGL, OpenGL) |
| `extensions/gtasdk/` | SDK extension for GTA RenderWare files |
| `examples/` | Sample projects (RPG Car) |

---

## Requirements

### Windows Build
- CMake 3.5+
- JDK 17
- Ninja (included in the repository)

### Android Build
- CMake 3.5+
- JDK 17
- Android SDK and NDK
- Ninja (included in the repository)

---

## Building

The build script automatically generates distribution artifacts in the `dist/` folder.

### Windows (Desktop)

```bash
# Build everything (native + Java + distribution)
build.bat

# Clean and rebuild
build.bat --clean

# Force CMake reconfiguration
build.bat --reconfig
```

### Android

```bash
# If you have ANDROID_NDK set in your environment
build.bat --android

# Or explicitly specify the NDK path
build.bat --android --ndk-path C:\AndroidSDK\ndk\26.2.11394342

# Clean and rebuild for Android
build.bat --clean --android

# Enable OpenAL audio support (configure once)
build.bat --android --oal on --reconfig
build.bat --android
```

### Linux / macOS (via build.sh)

```bash
./build.sh
./build.sh --android --ndk-path /path/to/ndk
```

---

## Build Options

| Option | Description |
|--------|-------------|
| `--android` | Build for Android instead of Windows |
| `--ndk-path PATH` | Path to the Android NDK (or use the `ANDROID_NDK` environment variable) |
| `--platform API` | Android platform target (default: `latest`) |
| `--oal VALUE` | Enable OpenAL audio (`on`/`off`, default: `off`) |
| `--reconfig` | Force CMake reconfiguration |
| `--clean` | Clean build artifacts before building |
| `--help` | Show help message |

---

## Integration

After building, the distribution files are placed in `dist/`:

| Target | Folder | Produced by |
|--------|--------|-------------|
| Windows | `dist/desktop/` | `build.bat` |
| Linux | `dist/linux/` | `build.sh` |
| Android | `dist/android/` | `build.bat --android` / `build.sh --android` |

The engine native is loaded by `NativeLoader`, which supports **Windows, Linux and macOS** on `amd64` and `aarch64`. It first looks for the binary embedded in the jar under `natives/<os>/<arch>/`; when present, it is extracted to a temporary folder and loaded with `System.load(...)`, so **no `java.library.path` entry is required**. If the binary is not embedded (for example during development), it falls back to `System.loadLibrary(...)`, resolved through `java.library.path` and the working directory.

### Headless mode (no visible window)

The desktop backend can create its OpenGL context on a window that is never shown. Nothing else changes: `create()`, `render(dt)`, framebuffer rendering, audio and input polling keep running.

```java
FXAppConfig config = new FXAppConfig();
config.headless = true;   // or run with -Dforcex.headless=true
```

In headless mode the window is created hidden, unfocused and undecorated, `glfwShowWindow` is never called, fullscreen and the window icon are ignored, and `isVisible()` reports `true` so the app is not treated as being in the background. `ForceXApp.isHeadless()` tells you which mode is active. An OpenGL-capable GPU/driver is still required (the context is real); this is "no visible window", not software rendering.

### Resizable window

```java
FXAppConfig config = new FXAppConfig();
config.resizable = true;
config.min_width = 1280;    // 0 = no limit
config.min_height = 720;
```

The user can drag the edges or maximize; the backend calls `Game.resize(width, height)` with the new framebuffer size and updates `FX.gpu.getWidth()/getHeight()`. A minimized window (0x0) is ignored. Ignored in fullscreen and headless modes. The default stays non-resizable.

### Desktop Integration

1. Copy the platform libraries from the distribution folder to your project's `libs/`:

   - Windows: `dist/desktop/libs/`
   - Linux: `dist/linux/libs/`

2. Package the engine native **inside your application jar** so `NativeLoader` can find and load it:

   | OS | Arch | Resource path inside the jar |
   |----|------|------------------------------|
   | Windows | amd64 | `natives/windows/amd64/fxcore.dll` |
   | Linux | amd64 | `natives/linux/amd64/libfxcore.so` |
   | Linux | aarch64 | `natives/linux/aarch64/libfxcore.so` |
   | macOS | amd64 | `natives/macos/amd64/libfxcore.dylib` |

   You can also keep the binary loose next to the jar (`fxcore.dll` / `libfxcore.so`); it is then resolved through `java.library.path`, which includes the current directory by default.

3. Copy the engine `data/` folder (fonts, gui, shaders) from the distribution folder alongside your application jar.

```
your-app/
├── app.jar          # embeds natives/<os>/<arch>/fxcore(.dll|.so)
├── data/
│   ├── fonts/
│   ├── gui/
│   └── shaders/
└── openpl64.dll     # (optional) third-party natives, resolved via System.loadLibrary
```

4. Add the required JARs to your `build.gradle`:

```groovy
dependencies {
    implementation files(
        'libs/forcex-desktop-backend.jar',
        'libs/forcex.jar',
        'libs/lwjgl.jar',
        'libs/lwjgl-opengl.jar',
        'libs/lwjgl-glfw.jar',
        'libs/lwjgl-openal.jar'
    )
}
```

> **Third-party natives**: libraries that call `System.loadLibrary(...)` themselves (for example `openpl64`) cannot be embedded, because the JVM resolves them by name through `java.library.path`. Ship them loose in the working directory instead — `openpl64.dll` on Windows, `libopenpl64.so` on Linux. LWJGL's own natives are unaffected: LWJGL extracts and loads them by absolute path from its `lwjgl-*-natives-*.jar` files.

### Android Integration

1. Copy the files from `dist/android/` to your app's `libs/` folder:

```
app/
└── libs/
    ├── forcex.jar
    └── android-backend-release.aar
```

2. Add to your `build.gradle`:

```groovy
dependencies {
    implementation files('libs/android-backend-release.aar', 'libs/forcex.jar')
}
```

---

## Examples

The repository includes sample projects to help you get started:

| Example | Description |
|---------|-------------|
| `examples/yaml-ui` | UI showcase: YAML-driven widgets (buttons, edit text, progress bar) |
| `examples/rml-ui` | RmlUi showcase: HTML/CSS UI, inline SVG, Lottie, `backdrop-filter` layers and Java events |

Each example contains `core/`, `desktop/`, and `android/` submodules.

### Running the desktop example

After running `build.bat` (Windows) or `build.sh` (Linux) once to produce `dist/`:

```bash
gradlew :examples:yaml-ui:desktop:run
gradlew :examples:rml-ui:desktop:run    # requires the RmlUi native (fxrmlui.dll / libfxrmlui.so)
```

`run` copies the engine native and the example assets into `desktop/build/libs/`, which is used
as the working directory so the native is resolved through `java.library.path=.`.

