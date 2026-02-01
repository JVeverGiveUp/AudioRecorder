# =====================================================
# CMakeLists.txt
# Support Android 15+ (16KB memory page size)
# =====================================================

cmake_minimum_required(VERSION 3.22.1)

project(mp3recorder)

# -----------------------------------------------------
# Global linker flags - REQUIRED for Android 15+
# -----------------------------------------------------
set(CMAKE_SHARED_LINKER_FLAGS
        "${CMAKE_SHARED_LINKER_FLAGS} -Wl,-z,max-page-size=16384")

# -----------------------------------------------------
# Path config
# -----------------------------------------------------
set(LIBMP3_DIR ${CMAKE_SOURCE_DIR}/libs)

# -----------------------------------------------------
# Prebuilt libmp3lame
# -----------------------------------------------------
add_library(mp3lame SHARED IMPORTED)

set_target_properties(mp3lame PROPERTIES
        IMPORTED_LOCATION
        ${LIBMP3_DIR}/${ANDROID_ABI}/libmp3lame.so
)

# -----------------------------------------------------
# Include headers
# -----------------------------------------------------
include_directories(
        ${CMAKE_SOURCE_DIR}/libs/include
)

# -----------------------------------------------------
# Build mp3recorder
# -----------------------------------------------------
add_library(mp3recorder SHARED
        src/main/cpp/lame_util.c
)

# -----------------------------------------------------
# Android system libs
# -----------------------------------------------------
find_library(log-lib log)

# -----------------------------------------------------
# Link libraries
# -----------------------------------------------------
target_link_libraries(mp3recorder
        mp3lame
        ${log-lib}
)

# -----------------------------------------------------
# Target-level linker flag (double safety)
# -----------------------------------------------------
target_link_options(mp3recorder PRIVATE
        -Wl,-z,max-page-size=16384
)
