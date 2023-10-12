#include <jni.h>
#include <android/log.h>

#include <libimobiledevice/libimobiledevice.h>
#include <usbmuxd.h>

#include "ideviceinstaller.h"
#include "ideviceinfo.h"
#include "device_permission.h"

JNIEXPORT void JNICALL
Java_com_dimkasark_MobileDeviceApi_setSocketPath(
        JNIEnv *env,
        jobject thiz,
        jstring socket_path
) {
    const char *socket_path_raw = (*env)->GetStringUTFChars(env, socket_path, 0);
    set_usbmuxd_path(socket_path_raw);
    (*env)->ReleaseStringUTFChars(env, socket_path, socket_path_raw);
}

JNIEXPORT jint JNICALL
Java_com_dimkasark_MobileDeviceApi_phoneConnectionPermission(JNIEnv *env, jobject thiz) {
    idevice_t device = NULL;
    idevice_error_t res = idevice_new_with_options(&device, NULL, IDEVICE_LOOKUP_USBMUX);

    if (IDEVICE_E_SUCCESS != res) {
        __android_log_print(ANDROID_LOG_WARN, "LIB", "No device found.");
        return -100;
    }

    int result = checkDevicePermission(device);

    idevice_free(device);
    return result;
}

JNIEXPORT void JNICALL
Java_com_dimkasark_MobileDeviceApi_phoneDetails(JNIEnv *env, jobject thiz, jstring output_file) {
    idevice_t device = NULL;
    idevice_error_t res = idevice_new_with_options(&device, NULL, IDEVICE_LOOKUP_USBMUX);
    if (IDEVICE_E_SUCCESS != res) {
        __android_log_print(ANDROID_LOG_WARN, "LIB", "No device found.");
        return;
    }
    const char *output_file_raw = (*env)->GetStringUTFChars(env, output_file, 0);
    exportDeviceInfo(device, output_file_raw);
    (*env)->ReleaseStringUTFChars(env, output_file, output_file_raw);
}

//typedef void (*install_upload_callback)(long uploaded, long all);


JNIEnv *_env = NULL;
jmethodID _installAppSendCallback = NULL;
jobject _installThis = NULL;

void installAppSendCallback(uint32_t send, uint32_t all) {
    if (_env != NULL && _installAppSendCallback != NULL && _installThis != NULL) {
        (*_env)->CallVoidMethod(_env, _installThis, _installAppSendCallback, (jlong) send, (jlong) all);
    }
}

JNIEXPORT jint JNICALL
Java_com_dimkasark_MobileDeviceApi_installApp(JNIEnv *env, jobject thiz, jstring path) {

    idevice_t device = NULL;
    idevice_error_t res = idevice_new_with_options(&device, NULL, IDEVICE_LOOKUP_USBMUX);

    if (IDEVICE_E_SUCCESS != res) {
        return -1;
    }

    const char *path_raw = (*env)->GetStringUTFChars(env, path, 0);

    _env = env;
    jclass cls_foo = (*env)->GetObjectClass(env, thiz);
    _installAppSendCallback = (*env)->GetMethodID(env, cls_foo, "installAppSend", "(JJ)V");
    _installThis = thiz;

    int result = runAction(device, CMD_INSTALL, path_raw, installAppSendCallback);

    (*env)->ReleaseStringUTFChars(env, path, path_raw);

    _env = NULL;
    _installAppSendCallback = NULL;
    _installThis = NULL;
    return result;
}