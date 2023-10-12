#include <libimobiledevice/libimobiledevice.h>

enum cmd_mode {
    CMD_NONE = 0,
    CMD_LIST_APPS,
    CMD_INSTALL,
    CMD_UNINSTALL,
    CMD_UPGRADE,
    CMD_LIST_ARCHIVES,
    CMD_ARCHIVE,
    CMD_RESTORE,
    CMD_REMOVE_ARCHIVE
};

typedef void (*install_upload_callback)(uint32_t uploaded, uint32_t all);

int runAction(idevice_t device, enum cmd_mode cmd, const char *cmdarg, install_upload_callback callback);
