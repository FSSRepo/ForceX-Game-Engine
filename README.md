# ForceX-Game-Engine
This powerful engine will be a great project. You will be able to create decent games for multiple platforms. 

## Functions and capabilities

Android 4.1 to up support.

Windows 7 to up support (32 bits, 64 bits).

## Requeriments and build

You need:

For desktop build (windows and linux):

    - CMake
    - JDK 8 or 11

For android build:
    - CMake
    - JDK 8 or 11
    - Android SDK and NDK

To build this project just execute this commands:

```bash
# desktop
build
build --dist
```

### Build android

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

## Integration


