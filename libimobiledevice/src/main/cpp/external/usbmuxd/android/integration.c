#include <jni.h>
#include <libusb.h>
#include <android/log.h>

#include "daemon.h"
#include "../usbmuxd/src/device.h"
#include "../usbmuxd/src/conf.h"

int no_preflight = 0;

JNIEXPORT jint JNICALL
Java_com_dimkasark_usbmux_UsbMuxDaemon_prepareDaemon(
        JNIEnv *env,
        jobject thiz,
        jint usb_descriptor,
        jstring config_path,
        jstring socket_path
) {
    libusb_context *ctx;
    libusb_device_handle *devh;
    // for not root device usage. WARNING CHANGE EMPTY CONTEXT TO DIRECTLY NULL
    libusb_set_option(NULL, LIBUSB_OPTION_NO_DEVICE_DISCOVERY, NULL);
    libusb_init(&ctx);
    libusb_wrap_sys_device(NULL, (intptr_t) usb_descriptor, &devh);
    libusb_device *device = libusb_get_device(devh);


    device_init();
    int res = public_usb_device_add(device, devh);
    __android_log_print(ANDROID_LOG_WARN, "LIB", "Result usb devices: %d", res);

    const char* config_path_raw = (*env)->GetStringUTFChars(env, config_path, 0);
    set_base_config_path(config_path_raw);
    (*env)->ReleaseStringUTFChars(env, config_path, config_path_raw);

    const char* socket_path_raw = (*env)->GetStringUTFChars(env, socket_path, 0);
    int listenfd = create_socket(socket_path_raw);
    (*env)->ReleaseStringUTFChars(env, socket_path, socket_path_raw);

    return listenfd;
}


JNIEXPORT void JNICALL
Java_com_dimkasark_usbmux_UsbMuxDaemon_startDaemon(JNIEnv *env, jobject thiz, jint descriptor) {
    __android_log_print(ANDROID_LOG_INFO, "LIB", "Initializing USB");
    loop_main(descriptor);
}

JNIEXPORT void JNICALL
Java_com_dimkasark_usbmux_UsbMuxDaemon_stopDaemon(JNIEnv *env, jobject thiz, jstring socket_path) {
    const char* socket_path_raw = (*env)->GetStringUTFChars(env, socket_path, 0);
    stop_loop(socket_path_raw);
    (*env)->ReleaseStringUTFChars(env, socket_path, socket_path_raw);
}

JNIEXPORT void JNICALL
Java_com_dimkasark_usbmux_UsbMuxDaemon_freeDaemonResources(JNIEnv *env, jobject thiz) {
    device_kill_connections();
    usb_shutdown();
    device_shutdown();
}

