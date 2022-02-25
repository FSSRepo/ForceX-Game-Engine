cmake_minimum_required(VERSION 3.4.1)

set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -std=c11 -Wall")
add_definitions(-DAL_ALEXT_PROTOTYPES -DARMEABI_V7A)
include_directories(src/main/cpp/include/)

add_library(
        fxaudio

        SHARED

        src/main/cpp/Alc/backends/base.c
        src/main/cpp/Alc/backends/opensl.c
        src/main/cpp/Alc/backends/loopback.c
        src/main/cpp/Alc/backends/wave.c
        src/main/cpp/Alc/backends/base.c
        src/main/cpp/Alc/backends/null.c
        src/main/cpp/Alc/ALc.c
        src/main/cpp/Alc/helpers.c
        src/main/cpp/Alc/bs2b.c
        src/main/cpp/Alc/alcRing.c
        src/main/cpp/Alc/effects/chorus.c
        src/main/cpp/Alc/effects/flanger.c
        src/main/cpp/Alc/effects/dedicated.c
        src/main/cpp/Alc/effects/reverb.c
        src/main/cpp/Alc/effects/distortion.c
        src/main/cpp/Alc/effects/autowah.c
        src/main/cpp/Alc/effects/equalizer.c
        src/main/cpp/Alc/effects/modulator.c
        src/main/cpp/Alc/effects/echo.c
        src/main/cpp/Alc/effects/compressor.c
        src/main/cpp/Alc/effects/null.c
        src/main/cpp/Alc/alcConfig.c
        src/main/cpp/Alc/ALu.c
        src/main/cpp/Alc/mixer_c.c
        src/main/cpp/Alc/panning.c
        src/main/cpp/Alc/hrtf.c
        src/main/cpp/Alc/mixer.c
        src/main/cpp/Alc/ambdec.c
        src/main/cpp/Alc/bformatdec.c
        src/main/cpp/Alc/bsinc.c
        src/main/cpp/Alc/uhjfilter.c
        src/main/cpp/common/uintmap.c
        src/main/cpp/common/almalloc.c
        src/main/cpp/common/atomic.c
        src/main/cpp/common/threads.c
        src/main/cpp/common/rwlock.c
        src/main/cpp/OpenAL32/alBuffer.c
        src/main/cpp/OpenAL32/alListener.c
        src/main/cpp/OpenAL32/alEffect.c
        src/main/cpp/OpenAL32/alExtension.c
        src/main/cpp/OpenAL32/alThunk.c
        src/main/cpp/OpenAL32/alAuxEffectSlot.c
        src/main/cpp/OpenAL32/alError.c
        src/main/cpp/OpenAL32/alFilter.c
        src/main/cpp/OpenAL32/alSource.c
        src/main/cpp/OpenAL32/alState.c
        src/main/cpp/OpenAL32/sample_cvt.c
        src/main/cpp/openal.c )

target_link_libraries(fxaudio
						log
                        -Wl
						-s
                      OpenSLES)
