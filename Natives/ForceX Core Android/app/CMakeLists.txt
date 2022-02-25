cmake_minimum_required(VERSION 3.4.1)

include_directories(src/main/cpp/)

add_library(
        fxcore

        SHARED

        src/main/cpp/dxt/alpha.cpp 
		src/main/cpp/dxt/clusterfit.cpp
		src/main/cpp/dxt/colourblock.cpp
		src/main/cpp/dxt/colourfit.cpp
		src/main/cpp/dxt/colourset.cpp
		src/main/cpp/dxt/maths.cpp
		src/main/cpp/dxt/rangefit.cpp
		src/main/cpp/dxt/singlecolourfit.cpp
		src/main/cpp/dxt/squish.cpp
		src/main/cpp/png/lodepng.cpp
		src/main/cpp/png/lodepng_util.cpp
		src/main/cpp/png/pngdetail.cpp
		src/main/cpp/main.cpp
		src/main/cpp/rg_etc1.cpp)
