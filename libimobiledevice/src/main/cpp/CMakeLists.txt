# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.22.1)

add_subdirectory(external/libplist)
add_subdirectory(external/libimobiledevice-glue)
add_subdirectory(external/usbmuxd)
add_subdirectory(external/libusbmuxd)
#add_subdirectory(external/libusbmuxd-android)
#add_subdirectory(external/libssl)
add_subdirectory(external/libimobiledevice)

# Declares and names the project.

project("icommunication")

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.

#include_directories(../../../../external/libplist/libcnary/include/ include/)

file(
        GLOB icommunication_SRC
        "src/ideviceinstaller.c"
        "src/ideviceinstaller.h"
        "src/ideviceinfo.c"
        "src/ideviceinfo.h"
        "src/device_permission.h"
        "src/device_permission.c"
        "src/native-lib.c"
)

add_library(
        icommunication

        SHARED

        ${icommunication_SRC}
)


# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

find_library( # Sets the name of the path variable.
        log-lib

        # Specifies the name of the NDK library that
        # you want CMake to locate.
        log)



add_library(
        zip
        STATIC
        IMPORTED
)

get_filename_component(
        lib_build_DIR
        "external/libzip"
        ABSOLUTE
)

set_target_properties( # Specifies the target library.
        zip

        # Specifies the parameter you want to define.
        PROPERTIES IMPORTED_LOCATION

        # Provides the path to the library you want to import.
        ${lib_build_DIR}/${ANDROID_ABI}/lib/libzip.a
        )

include_directories(external/libimobiledevice/libimobiledevice/include external/libplist/libplist/include external/libssl/include external/libzip/includes external/libusbmuxd/libusbmuxd/include)

target_link_libraries( # Specifies the target library.
        icommunication

        imobiledevice

        zip

        z

        # Links the target library to the log library
        # included in the NDK.
        ${log-lib})
