# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.22.1)

# Declares and names the project.
project("plist")

include_directories(libplist/libcnary/include/ libplist/include/ libplist/src/)

file(
        GLOB cnary_SRC
        "libplist/libcnary/node.c"
        "libplist/libcnary/node_list.c"
        "libplist/libcnary/include/*.h"
)

file(
        GLOB plist_SRC
        "libplist/src/*.c"
        "libplist/src/*.h"
)

add_library(
        cnary

        STATIC

        ${cnary_SRC}
)

add_definitions(-DHAVE_STRNDUP)

add_library(
        plist

        STATIC

        ${plist_SRC}
)

target_link_libraries( # Specifies the target library.
        plist

        cnary
        )
