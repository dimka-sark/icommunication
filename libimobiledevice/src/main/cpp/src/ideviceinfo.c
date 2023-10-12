/*
 * ideviceinfo.c
 * Simple utility to show information about an attached device
 *
 * Copyright (c) 2010-2019 Nikias Bassen, All Rights Reserved.
 * Copyright (c) 2009 Martin Szulecki All Rights Reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

#ifdef HAVE_CONFIG_H
#include <config.h>
#endif

#define TOOL_NAME "ideviceinfo"

#include <stdio.h>
#include <string.h>
#include <errno.h>
#include <stdlib.h>
#include <getopt.h>
#ifndef WIN32
#include <signal.h>
#endif

#include <libimobiledevice/libimobiledevice.h>
#include <libimobiledevice/lockdown.h>
#include <plist/plist.h>

#include <android/log.h>
#include "ideviceinfo.h"

static const char *domains[] = {
	"com.apple.disk_usage",
	"com.apple.disk_usage.factory",
	"com.apple.mobile.battery",
/* FIXME: For some reason lockdownd segfaults on this, works sometimes though
	"com.apple.mobile.debug",. */
	"com.apple.iqagent",
	"com.apple.purplebuddy",
	"com.apple.PurpleBuddy",
	"com.apple.mobile.chaperone",
	"com.apple.mobile.third_party_termination",
	"com.apple.mobile.lockdownd",
	"com.apple.mobile.lockdown_cache",
	"com.apple.xcode.developerdomain",
	"com.apple.international",
	"com.apple.mobile.data_sync",
	"com.apple.mobile.tethered_sync",
	"com.apple.mobile.mobile_application_usage",
	"com.apple.mobile.backup",
	"com.apple.mobile.nikita",
	"com.apple.mobile.restriction",
	"com.apple.mobile.user_preferences",
	"com.apple.mobile.sync_data_class",
	"com.apple.mobile.software_behavior",
	"com.apple.mobile.iTunes.SQLMusicLibraryPostProcessCommands",
	"com.apple.mobile.iTunes.accessories",
	"com.apple.mobile.internal", /**< iOS 4.0+ */
	"com.apple.mobile.wireless_lockdown", /**< iOS 4.0+ */
	"com.apple.fairplay",
	"com.apple.iTunes",
	"com.apple.mobile.iTunes.store",
	"com.apple.mobile.iTunes",
	"com.apple.fmip",
	"com.apple.Accessibility",
	NULL
};

static int is_domain_known(const char *domain)
{
	int i = 0;
	while (domains[i] != NULL) {
		if (strstr(domain, domains[i++])) {
			return 1;
		}
	}
	return 0;
}

int exportDeviceInfo(idevice_t device, const char* outputPath) {
	lockdownd_client_t client = NULL;
	lockdownd_error_t ldret = LOCKDOWN_E_UNKNOWN_ERROR;
	int simple = 0;
	const char *domain = NULL;
	const char *key = NULL;
	plist_t node = NULL;


#ifndef WIN32
	signal(SIGPIPE, SIG_IGN);
#endif

	if (LOCKDOWN_E_SUCCESS != (ldret = simple ?
									   lockdownd_client_new(device, &client, TOOL_NAME):
									   lockdownd_client_new_with_handshake(device, &client, TOOL_NAME))) {
		__android_log_print(ANDROID_LOG_ERROR, "LIB", "ERROR: Could not connect to lockdownd: %s (%d)\n", lockdownd_strerror(ldret), ldret);
		idevice_free(device);
		return -1;
	}

	if (domain && !is_domain_known(domain)) {
		__android_log_print(ANDROID_LOG_ERROR, "LIB", "WARNING: Sending query with unknown domain \"%s\".\n", domain);
	}

	char *buf = NULL;
	uint32_t len = 0;

	/* run query and output information */
	if(LOCKDOWN_E_SUCCESS == (ldret =lockdownd_get_value(client, domain, key, &node))) {
		if (node) {

			plist_err_t perr = plist_to_xml(node, &buf, &len);//plist_to_json(node, &buf, &len, 1);

			if (perr != PLIST_ERR_SUCCESS) {
				__android_log_print(ANDROID_LOG_ERROR, "LIB", "ERROR: Failed to convert data to XML format (%d).\n", perr);
			}

			if (buf) {
				FILE *fptr = fopen(outputPath,"w");
				fputs(buf, fptr);
				fclose(fptr);

				free(buf);
			}

			plist_free(node);
			node = NULL;
		} else {
			__android_log_print(ANDROID_LOG_ERROR, "LIB", "ERROR: Read file empty");
		}
	} else {
		__android_log_print(ANDROID_LOG_ERROR, "LIB", "ERROR: Could not get value to lockdownd: %s (%d)\n", lockdownd_strerror(ldret), ldret);
	}

	lockdownd_client_free(client);
	idevice_free(device);

	return 0;
}

