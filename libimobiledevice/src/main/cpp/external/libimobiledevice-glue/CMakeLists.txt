# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.22.1)

# Declares and names the project.
project("imobiledevice-glue")

include_directories(libimobiledevice-glue/include/ ../libplist/libplist/include)

file(
        GLOB imobiledevice_glue_SRC
        "libimobiledevice-glue/src/*.c"
        "libimobiledevice-glue/src/*.h"
)

add_library(
        imobiledevice-glue

        STATIC

        ${imobiledevice_glue_SRC}
)

#add_definitions(-DHAVE_STRNDUP)

#add_library(
#        plist
#
#        STATIC
#
#        ${plist_SRC}
#)

add_definitions(-DHAVE_GETIFADDRS)

find_library( # Sets the name of the path variable.
        log-lib

        # Specifies the name of the NDK library that
        # you want CMake to locate.
        log)

target_link_libraries( # Specifies the target library.
        imobiledevice-glue

        plist

        ${log-lib}
        )
