# ForceX-Game-Engine
This powerful engine will be a great project. You will be able to create decent games for multiple platforms. 

# Functions

Android 4.1 to up support.

Windows 7 to up support (32 bits, 64 bits).

# Requeriments and build

You need:

For desktop build (windows and linux):

- CMake
- JDK 8 or 11

For android build:

- CMake
- JDK 8 or 11
- Android SDK and NDK

## Build Desktop

To build this project just execute these commands:

```bash
# desktop
build
build --dist
```

## Build android

If you have ANDROID_NDK defined:

```bash
build --android
build --android --dist
```

If ANDROID_NDK environment variable doesn't exist you must provide the path to ndk `path/to/ndk/version`:

```bash
build --ndk-path C:/android-sdk/ndk/26.2.11394342 --android
```

To reconfigure cmake:

```bash
# you can add --android to reconfigure android cmake configs
build --reconfig
```

For android, if you need to use OpenAL for audio, you need to build the native library:

```bash
# run this command once to enable this, after you can just use 'build --android'
build --android --oal --reconfig
build --android --dist
```

# Integration

To integrate ForceX into your applications, you must have built the respective files.

### Windows and Linux

1. Copy the files from `dist/(windows|linux)/libs` to your libraries `.jar` directory of your app.

2. After you build your application jar, you have to copy the native libraries and `data` directory from `dist/(windows|linux)` to your .jar directory, like this:

```bash
# windows
outputs
    data
    app.jar
    forcex.dll
    lwjgl.dll

# linux
outputs
    data
    app.jar
    libforcex.so
    lwjgl.so
```

3. Import the libraries in your `build.gradle` in your dependencies block:

```java
dependencies {
    implementation files('libs/forcex-windows-backend.jar', 'libs/forcex.jar', 'libs/jinput.jar', 'libs/lwjgl_util_applet.jar', 'libs/lwjgl_util.jar', 'libs/lwjgl.jar', 'libs/lzma.jar')
}
```

### Android

1. Copy the files from `dist/android` to your libs directory of your app.

```bash
app
    src
        main
    libs
        forcex.jar
        android-backend-release.aar
    build.gradle
```

2. Import the libraries in your `build.gradle` in your dependencies block:

```java
dependencies {
    implementation files('libs/android-backend-release.aar', 'libs/forcex.jar')
}
```