#include "device_permission.h"
#include "libimobiledevice/lockdown.h"

#define TOOL_NAME "checkDevicePermission"

int checkDevicePermission(idevice_t device) {
    lockdownd_client_t client = NULL;
    lockdownd_error_t ldret = LOCKDOWN_E_UNKNOWN_ERROR;

    if (LOCKDOWN_E_SUCCESS != (ldret = lockdownd_client_new_with_handshake(device, &client, TOOL_NAME))) {
        return ldret;
    }

    // handshake success permission granted
    lockdownd_client_free(client);
    return 0;
}