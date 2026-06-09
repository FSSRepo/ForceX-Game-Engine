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
- **Ray Tracing Engine**: RTEngine module for real-time ray tracing
- **Audio**: OpenAL support (optional on Android) and WAV playback
- **Asset Management**: Custom package formats, texture compression (DXT, ETC1)
- **Networking**: HTTP download manager and property files
- **Threading**: TaskPool system for background jobs
- **3D Math**: Vectors, matrices, quaternions, planes, rays, and geometric utilities
- **GTA Extension**: RenderWare DFF/IFP file support via `extensions/gtasdk`

---

## Project Structure

| Module | Description |
|--------|-------------|
| `forcex/` | Core engine library (Java) |
| `forcex/jni/` | Native C++ code (texture compression, JNI bridge) |
| `android-backend/` | Android platform backend (OpenGL ES, input, audio) |
| `windows-backend/` | Windows platform backend (LWJGL, OpenGL) |
| `extensions/gtasdk/` | SDK extension for GTA RenderWare files |
| `examples/` | Sample projects (RPG Car, Super AI) |

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

After building, all distribution files are placed in `dist/`.

### Windows Integration

1. Copy the contents of `dist/windows/libs/` to your project's library folder.
2. Copy `dist/windows/fxcore.dll` and the `dist/windows/data/` folder alongside your application's JAR.

```
your-app/
├── app.jar
├── fxcore.dll
├── lwjgl.dll
└── data/
    ├── fonts/
    ├── gui/
    └── shaders/
```

3. Add the required JARs to your `build.gradle`:

```groovy
dependencies {
    implementation files(
        'libs/forcex-windows-backend.jar',
        'libs/forcex.jar',
        'libs/lwjgl.jar',
        'libs/lwjgl-opengl.jar',
        'libs/lwjgl-glfw.jar',
        'libs/lwjgl-openal.jar',
        'libs/lwjgl_util.jar'
    )
}
```

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
| `examples/rpg-car` | RPG-style car game with physics |
| `examples/super-ai` | AI simulation demo |

Each example contains `core/`, `desktop/`, and `android/` submodules.
