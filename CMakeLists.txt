cmake_minimum_required(VERSION 3.4.1)

project(AppTest)

option(FX_OAL           OFF)

if(WIN32)
        set(INCLUDES_EXTRAS
                $ENV{JAVA_HOME}/include
                $ENV{JAVA_HOME}/include/win32)
elseif(CMAKE_SYSTEM_NAME MATCHES "Android")
        set(CMAKE_ARCHIVE_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/bin)
        set(CMAKE_LIBRARY_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/bin)
        set(CMAKE_RUNTIME_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/bin)
        if(FX_OAL)
                message("ForceX Open AL Audio Engine enabled")
                add_subdirectory(forcex/jni/oal)
        endif()
endif()

include_directories(forcex/jni ${INCLUDES_EXTRAS})

add_library(
        fxcore

        SHARED

        forcex/jni/dxt/alpha.cpp
        forcex/jni/dxt/clusterfit.cpp
        forcex/jni/dxt/colourblock.cpp
        forcex/jni/dxt/colourfit.cpp
        forcex/jni/dxt/colourset.cpp
        forcex/jni/dxt/maths.cpp
        forcex/jni/dxt/rangefit.cpp
        forcex/jni/dxt/singlecolourfit.cpp
        forcex/jni/dxt/squish.cpp
        forcex/jni/main.cpp
        forcex/jni/rg_etc1.cpp)
