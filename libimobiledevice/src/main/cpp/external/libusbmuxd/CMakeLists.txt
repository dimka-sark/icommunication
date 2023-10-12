# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.22.1)

# Declares and names the project.
project("usbmuxd")

include_directories(libusbmuxd/include ../libplist/libplist/include ../libimobiledevice-glue/libimobiledevice-glue/include)

file(
        GLOB usbmuxd_SRC
        "libusbmuxd/src/*.c"
        "libusbmuxd/src/*.h"
)

add_library(
        usbmuxd

        STATIC

        ${usbmuxd_SRC}
)

#add_definitions(-DHAVE_STRNDUP)

#add_library(
#        plist
#
#        STATIC
#
#        ${plist_SRC}
#)

add_definitions(-DHAVE_STPNCPY)
add_definitions(-DPACKAGE_STRING="2.0")

find_library( # Sets the name of the path variable.
        log-lib

        # Specifies the name of the NDK library that
        # you want CMake to locate.
        log)

target_link_libraries( # Specifies the target library.
        usbmuxd

        #plist
        ${log-lib}
        )
