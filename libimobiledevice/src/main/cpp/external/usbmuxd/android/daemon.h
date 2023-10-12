#include "libusb.h"

int create_socket(const char *socket_path);

int loop_main(int listenfd);

void stop_loop(const char *socket_path);
