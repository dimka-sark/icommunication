# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.22.1)

# Declares and names the project.
project("imobiledevice")

file(
        GLOB imobiledevice_common_SRC
        "libimobiledevice/common/*.c"
        "libimobiledevice/common/*.h"
)

add_library(
        imobiledevice-common

        STATIC

        ${imobiledevice_common_SRC}
)


file(
        GLOB imobiledevice_SRC
        "libimobiledevice/src/*.c"
        "libimobiledevice/src/*.h"
)
include_directories(
        libimobiledevice/include
        ../libplist/libplist/include
        ../libimobiledevice-glue/libimobiledevice-glue/include
        libimobiledevice
        ../libssl/include
        ../libusbmuxd/libusbmuxd/include
)

add_library(
        imobiledevice

        STATIC

        ${imobiledevice_SRC}
)

add_definitions(-DHAVE_OPENSSL -DHAVE_VASPRINTF -DHAVE_ASPRINTF)

add_library(
        ssl
        STATIC
        IMPORTED
)

get_filename_component(
        lib_build_DIR
        "../libssl"
        ABSOLUTE
)

set_target_properties( # Specifies the target library.
        ssl

        # Specifies the parameter you want to define.
        PROPERTIES IMPORTED_LOCATION

        # Provides the path to the library you want to import.
        ${lib_build_DIR}/${ANDROID_ABI}/libssl.a
        )

add_library(
        crypto
        STATIC
        IMPORTED
)

set_target_properties( # Specifies the target library.
        crypto

        # Specifies the parameter you want to define.
        PROPERTIES IMPORTED_LOCATION

        # Provides the path to the library you want to import.
        ${lib_build_DIR}/${ANDROID_ABI}/libcrypto.a
        )


find_library( # Sets the name of the path variable.
        log-lib

        # Specifies the name of the NDK library that
        # you want CMake to locate.
        log)


target_link_libraries( # Specifies the target library.
        imobiledevice

        plist

        imobiledevice-glue

        imobiledevice-common

        usbmuxd

        ssl

        crypto

        ${log-lib}
        )



