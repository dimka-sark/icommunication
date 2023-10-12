# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.22.1)

# Declares and names the project.
project("usbmuxd-android")

include_directories(../libplist/libplist/include ../libimobiledevice-glue/libimobiledevice-glue/include ../libusb/include)

file(
        GLOB usbmuxd-android_SRC
        "usbmuxd/src/*.c"
        "usbmuxd/src/*.h"
        "android/*.c"
        "android/*.h"
)

get_filename_component(full_path_main_c ${CMAKE_CURRENT_SOURCE_DIR}/usbmuxd/src/main.c ABSOLUTE)

list(REMOVE_ITEM usbmuxd-android_SRC "${full_path_main_c}")


add_library(
        usbmuxd-android

        SHARED

        ${usbmuxd-android_SRC}
)

#add_definitions(-DHAVE_STRNDUP)

#add_library(
#        plist
#
#        STATIC
#
#        ${plist_SRC}
#)

add_definitions(-DHAVE_CLOCK_GETTIME)
#add_definitions(-DPACKAGE_STRING="2.0")

find_library( # Sets the name of the path variable.
        log-lib

        # Specifies the name of the NDK library that
        # you want CMake to locate.
        log)

add_library(
        usb
        SHARED
        IMPORTED
)


get_filename_component(
        lib_build_DIR
        "../libusb"
        ABSOLUTE
)

add_definitions(
        -DPACKAGE_STRING="2.0"
        -DPACKAGE_NAME="usbmuxd"
        -DPACKAGE_URL
        -DHAVE_PPOLL
)

set_target_properties( # Specifies the target library.
        usb

        # Specifies the parameter you want to define.
        PROPERTIES IMPORTED_LOCATION

        # Provides the path to the library you want to import.
        ${lib_build_DIR}/libs/${ANDROID_ABI}/libusb1.0.so
        )


target_link_libraries( # Specifies the target library.
        usbmuxd-android

        plist

        imobiledevice-glue

        usb

        ${log-lib}
        )
