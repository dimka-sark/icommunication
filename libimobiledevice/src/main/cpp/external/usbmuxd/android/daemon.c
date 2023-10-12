#include <string.h>
#include <unistd.h>
#include <errno.h>
#include <stdio.h>

#include <sys/un.h>
#include <sys/stat.h>
#include <sys/socket.h>

#include <android/log.h>

#include <libimobiledevice-glue/socket.h>

#include "daemon.h"

#include "../usbmuxd/src/utils.h"
#include "../usbmuxd/src/device.h"
#include "../usbmuxd/src/log.h"

int create_socket(const char *socket_path)
{
    int listenfd;
    const char* socket_addr = socket_path;
    char listen_addr_str[256];

    if (0) {
    } else {
        struct sockaddr_un bind_addr;

        if (strcmp(socket_addr, socket_path) != 0) {
            struct stat fst;
            if (stat(socket_addr, &fst) == 0) {
                if (!S_ISSOCK(fst.st_mode)) {
                    usbmuxd_log(LL_ERROR, "FATAL: File '%s' already exists and is not a socket file. Refusing to continue.", socket_addr);
                    return -1;
                }
            }
        }

        if (unlink(socket_addr) == -1 && errno != ENOENT) {
            usbmuxd_log(LL_ERROR,  "%s: unlink(%s) failed: %s", __func__, socket_addr, strerror(errno));
            return -1;
        }

        listenfd = socket(AF_UNIX, SOCK_STREAM, 0);
        if (listenfd == -1) {
            usbmuxd_log(LL_ERROR,  "socket() failed: %s", strerror(errno));
            return -1;
        }

        bzero(&bind_addr, sizeof(bind_addr));
        bind_addr.sun_family = AF_UNIX;
        strncpy(bind_addr.sun_path, socket_addr, sizeof(bind_addr.sun_path));
        bind_addr.sun_path[sizeof(bind_addr.sun_path) - 1] = '\0';

        if (bind(listenfd, (struct sockaddr*)&bind_addr, sizeof(bind_addr)) != 0) {
            usbmuxd_log(LL_ERROR, "bind() failed: %s", strerror(errno));
            return -1;
        }
        chmod(socket_addr, 0666);

        snprintf(listen_addr_str, sizeof(listen_addr_str), "%s", socket_addr);
    }

    int flags = fcntl(listenfd, F_GETFL, 0);
    if (flags < 0) {
        usbmuxd_log(LL_ERROR, "ERROR: Could not get flags for socket");
    } else {
        if (fcntl(listenfd, F_SETFL, flags | O_NONBLOCK) < 0) {
            usbmuxd_log(LL_ERROR, "ERROR: Could not set socket to non-blocking");
        }
    }

    // Start listening
    if (listen(listenfd, 256) != 0) {
        usbmuxd_log(LL_ERROR, "listen() failed: %s", strerror(errno));
        return -1;
    }

    usbmuxd_log(LL_INFO, "Listening on %s", listen_addr_str);

    return listenfd;
}

static int should_exit = 0;

int loop_main(int listenfd) {
    int to, cnt, i, dto;
    struct fdlist pollfds;
    struct timespec tspec;

    sigset_t empty_sigset;
    sigemptyset(&empty_sigset);

    fdlist_create(&pollfds);

    should_exit = 0;
    while(!should_exit) {
        usbmuxd_log(LL_FLOOD, "main_loop iteration");
        to = usb_get_timeout();
        usbmuxd_log(LL_FLOOD, "USB timeout is %d ms", to);
        dto = device_get_timeout();
        usbmuxd_log(LL_FLOOD, "Device timeout is %d ms", dto);
        if(dto < to)
            to = dto;

        fdlist_reset(&pollfds);
        fdlist_add(&pollfds, FD_LISTEN, listenfd, POLLIN);
        usb_get_fds(&pollfds);
        client_get_fds(&pollfds);
        usbmuxd_log(LL_FLOOD, "fd count is %d", pollfds.count);

        tspec.tv_sec = to / 1000;
        tspec.tv_nsec = (to % 1000) * 1000000;
        cnt = ppoll(pollfds.fds, pollfds.count, &tspec, &empty_sigset);
        usbmuxd_log(LL_FLOOD, "poll() returned %d", cnt);

        if(should_exit) {
            usbmuxd_log(LL_INFO, "Stop after event received");
            break;
        }

        if(cnt == -1) {
            if(errno == EINTR) {
                if(should_exit) {
                    usbmuxd_log(LL_INFO, "Event processing interrupted");
                    break;
                }
            }
        } else if(cnt == 0) {
            if(usb_process() < 0) {
                usbmuxd_log(LL_ERROR, "usb_process() failed");
                fdlist_free(&pollfds);
                return -1;
            }
            device_check_timeouts();
        } else {
            int done_usb = 0;
            for(i=0; i<pollfds.count; i++) {
                if(pollfds.fds[i].revents) {
                    if(!done_usb && pollfds.owners[i] == FD_USB) {
                        if(usb_process() < 0) {
                            usbmuxd_log(LL_ERROR, "usb_process() failed");
                            fdlist_free(&pollfds);
                            return -1;
                        }
                        done_usb = 1;
                    }
                    if(pollfds.owners[i] == FD_LISTEN) {
                        if(client_accept(listenfd) < 0) {
                            usbmuxd_log(LL_ERROR, "client_accept() failed");
                            fdlist_free(&pollfds);
                            return -1;
                        }
                    }
                    if(pollfds.owners[i] == FD_CLIENT) {
                        client_process(pollfds.fds[i].fd, pollfds.fds[i].revents);
                    }
                }
            }
        }
    }
    usbmuxd_log(LL_FLOOD, "main_loop exit");
    fdlist_free(&pollfds);
    return 0;
}

void stop_loop(const char *socket_path) {
    usbmuxd_log(LL_FLOOD, "stop_loop start");
    should_exit = 1;


    // Try to connect to socket for force check daemon dead
    int sfd = socket(PF_UNIX, SOCK_STREAM, 0);
    if(sfd  < 0){
        usbmuxd_log(LL_FLOOD, "stop_loop error socket create");
    }

    struct sockaddr_un name;
    name.sun_family = AF_UNIX;
    strncpy(name.sun_path, socket_path, sizeof(name.sun_path));
    name.sun_path[sizeof(name.sun_path) - 1] = 0;

    if (connect(sfd, (struct sockaddr*)&name, sizeof(name)) != -1) {
        socket_close(sfd);
    } else {
        usbmuxd_log(LL_FLOOD, "stop_loop error socket connect");
    }
    usbmuxd_log(LL_FLOOD, "stop_loop end");
}
