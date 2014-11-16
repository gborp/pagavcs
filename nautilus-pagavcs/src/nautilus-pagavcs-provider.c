/*-
 * Copyright (c) 2013 PagaVCS <pagavcs@gmail.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

#ifdef HAVE_CONFIG_H
# include <config.h>
#endif

#include "nautilus-pagavcs-provider.h"
#include <libnautilus-extension/nautilus-menu-provider.h>
#include <gtk/gtk.h>
#include <string.h>
#include <glib/gstring.h>
#include <sys/socket.h>
#include <sys/stat.h>
#include <linux/un.h>
#include <unistd.h>
#include <sys/types.h>
#include <pwd.h>

#define PAGAVCS_DIRECTORY "/.pagavcs"
#define SOCKET_FILENAME "/.pagavcs/socket"
#define LOG_FILENAME "/.pagavcs/pagavcs.log"
#define BUFFER_SIZE 256*256*4

static void nautilus_pagavcs_instance_init(NautilusPagavcs* img);
static void nautilus_pagavcs_class_init(NautilusPagavcsClass* class);
GList* nautilus_pagavcs_get_file_items(NautilusMenuProvider* provider,
		GtkWidget* window, GList* files);
GList* nautilus_pagavcs_get_background_items(NautilusMenuProvider* provider,
		GtkWidget* window, NautilusFileInfo* file_info);

static GType pagavcs_type = 0;

static void nautilus_pagavcs_menu_provider_iface_init(
		NautilusMenuProviderIface* iface) {
	iface->get_background_items = nautilus_pagavcs_get_background_items;
	iface->get_file_items = nautilus_pagavcs_get_file_items;
}

void nautilus_pagavcs_register_type(GTypeModule* module) {

	static const GTypeInfo info = { sizeof(NautilusPagavcsClass),
			(GBaseInitFunc) NULL, (GBaseFinalizeFunc) NULL,
			(GClassInitFunc) nautilus_pagavcs_class_init, NULL, NULL,
			sizeof(NautilusPagavcs), 0,
			(GInstanceInitFunc) nautilus_pagavcs_instance_init };

	static const GInterfaceInfo menu_provider_iface_info = {
			(GInterfaceInitFunc) nautilus_pagavcs_menu_provider_iface_init,
			NULL, NULL };

	pagavcs_type = g_type_module_register_type(module, G_TYPE_OBJECT,
			"NautilusPagavcs", &info, 0);

	g_type_module_add_interface(module, pagavcs_type,
	NAUTILUS_TYPE_MENU_PROVIDER, &menu_provider_iface_info);
}

void startServer() {

	char fullPagavcsDir[512];
	char fullLogFilename[512];
	char command[2048];
	int uid;
	struct passwd *pw;

	uid = getuid();
	pw = getpwuid(uid);
	strcpy(fullPagavcsDir, pw->pw_dir);
	strcat(fullPagavcsDir, PAGAVCS_DIRECTORY);
	mkdir(fullPagavcsDir, S_IRWXU);

	strcpy(fullLogFilename, pw->pw_dir);
	strcat(fullLogFilename, LOG_FILENAME);

	strcpy(command, "nohup java -Xms16m");
	strcat(command, " -Xmx400m -XX:MaxPermSize=160M");
	strcat(command, " -XX:PermSize=32M");
	// strcat(command, " -XX:MinHeapFreeRatio=10 -XX:MaxHeapFreeRatio=20");
	strcat(command, " -Djava.library.path=/usr/lib/jni -jar");
	strcat(command, " /usr/share/pagavcs/bin/pagavcs.jar >> ");
	strcat(command, fullLogFilename);
	strcat(command, " 2>&1 &");

	system(command);
}

static gboolean isServerRunning() {
	int uid;
	struct passwd *pw;
	struct sockaddr_un address;
	int socket_fd, nbytes;
	char fullSocketFilename[512];

	uid = getuid();
	pw = getpwuid(uid);

	socket_fd = socket(PF_UNIX, SOCK_STREAM, 0);
	if (socket_fd < 0) {
		printf("pagavcs socket is not available");
		startServer();
		return FALSE;
	}

	strcpy(fullSocketFilename, "/home/");
	strcat(fullSocketFilename, pw->pw_name);
	strcat(fullSocketFilename, SOCKET_FILENAME);

	memset(&address, 0, sizeof(struct sockaddr_un));

	address.sun_family = AF_UNIX;
	strcpy(address.sun_path, fullSocketFilename);

	if (connect(socket_fd, (struct sockaddr *) &address,
			sizeof(struct sockaddr_un)) != 0) {
		printf("pagavcs socket is not answering");
		startServer();
		return FALSE;
	}
	close(socket_fd);

	printf("debug-1\n");

	return TRUE;
}

static void connectServer(int socket_fd, struct sockaddr_un address) {
	int first = 1;
	while (connect(socket_fd, (struct sockaddr *) &address,
			sizeof(struct sockaddr_un)) != 0) {
		if (first == 1) {
			startServer();
			first = 0;
		}
		sleep(1);
	}
}

static char* sendToServerCommand(char *command) {
	int uid;
	struct passwd *pw;
	struct sockaddr_un address;
	int socket_fd, nbytes;
	char buffer[BUFFER_SIZE];
	char fullSocketFilename[512];

	uid = getuid();
	pw = getpwuid(uid);

	strcpy(fullSocketFilename, "/home/");
	strcat(fullSocketFilename, pw->pw_name);
	strcat(fullSocketFilename, SOCKET_FILENAME);

	socket_fd = socket(PF_UNIX, SOCK_STREAM, 0);
	if (socket_fd < 0) {
		printf("socket() failed\n");
		return "";
	}

	memset(&address, 0, sizeof(struct sockaddr_un));

	address.sun_family = AF_UNIX;
	strcpy(address.sun_path, fullSocketFilename);

	connectServer(socket_fd, address);
	int writeResult;
	writeResult = write(socket_fd, command, strlen(command));
	writeResult = write(socket_fd, "\n", strlen("\n"));

	nbytes = read(socket_fd, buffer, sizeof(buffer));
	buffer[nbytes] = 0;

	close(socket_fd);

	return strdup(buffer);
}

static char* sendToServer(char *command, char *path) {
	char buffer[BUFFER_SIZE];
	strcpy(buffer, "\"");
	strcat(buffer, command);
	strcat(buffer, "\"");
	strcat(buffer, " ");
	strcat(buffer, path);
	return sendToServerCommand(buffer);
}

static gboolean pagavcs_file_is_mergeable(NautilusFileInfo* file_info) {
	gchar* mime_type;
	gboolean mergeable;

	mergeable = FALSE;
	mime_type = nautilus_file_info_get_mime_type(file_info);

	if ((strcmp(mime_type, "application/x-extension-xtm") == 0)
			|| (strcmp(mime_type, "application/x-extension-gsp") == 0)
			|| (strcmp(mime_type, "application/x-extension-yct") == 0)
			|| (strcmp(mime_type, "application/x-extension-kk") == 0)
			|| (strcmp(mime_type, "application/x-generic-chunk") == 0)) {
		mergeable = TRUE;
	}

	g_free(mime_type);

	return mergeable;
}

static void pagavcs_merge_callback(NautilusMenuItem* item, GList* files) {
	GString* buffer;
	GFile* location;
	gchar* command;

	buffer = g_string_new("gnome-split --merge ");
	location = nautilus_file_info_get_location(files->data);

	g_string_append(buffer, g_file_get_path(location));
	g_object_unref(location);

	command = buffer->str;
	g_string_free(buffer, FALSE);

	g_spawn_command_line_async(command, NULL);
}

static void pagavcs_split_callback(NautilusMenuItem* item, GList* files) {
	GString* buffer;
	GFile* location;
	gchar* command;

	buffer = g_string_new("gnome-split --split ");
	location = nautilus_file_info_get_location(files->data);

	g_string_append(buffer, g_file_get_path(location));
	g_object_unref(location);

	command = buffer->str;
	g_string_free(buffer, FALSE);

	g_spawn_command_line_async(command, NULL);
}

/*
 static gchar *getFilesAsString(GList *files) {
 GList *lp;
 gchar *path;
 gchar *uri;
 char argumentToSend[BUFFER_SIZE];

 strcpy(argumentToSend, "");

 for (lp = files; lp != NULL; lp = lp->next) {
 uri = thunarx_file_info_get_uri(lp->data);
 path = g_filename_from_uri(uri, NULL, NULL);

 strcat(argumentToSend, "\"");
 strcat(argumentToSend, path);
 strcat(argumentToSend, "\" ");

 g_free(path);
 g_free(uri);
 }
 return g_strdup(argumentToSend);
 }*/

static char *
get_path_from_file_info(NautilusFileInfo *info) {
	GFile *file;
	gchar *str, *path;

	str = nautilus_file_info_get_uri(info);
	file = g_file_new_for_uri(str);

	path = g_file_get_path(file);

	g_object_unref(file);
	g_free(str);

	return path;
}

GList* nautilus_pagavcs_get_background_items(NautilusMenuProvider* provider,
		GtkWidget* window, NautilusFileInfo* file_info) {

	GList *toret = NULL;

	NautilusMenuItem *root_item;
	NautilusMenu *root_menu;

	NautilusMenuItem *item;

	/* g_signal_connect(item, "activate", G_CALLBACK(pagavcs_merge_callback),
	 nautilus_file_info_list_copy(files));*/

	char* path = get_path_from_file_info(file_info);
	printf("pagavcs path: %s\n", path);

	if (path==NULL || !isServerRunning()) {

		root_menu = nautilus_menu_new();
		root_item = nautilus_menu_item_new("NautilusPagaVCS2::root_item",
				"PagaVCS loading...", "loading", NULL);

		toret = g_list_append(toret, root_item);
		return toret;
	}

	printf("debug0\n");

	root_menu = nautilus_menu_new();
	root_item = nautilus_menu_item_new("NautilusPagaVCS2::root_item", "PagaVCS-SVN",
			"PagaVCS", NULL);

	toret = g_list_append(toret, root_item);

	nautilus_menu_item_set_submenu(root_item, root_menu);

	printf("debug1\n");

	char argumentToSend[BUFFER_SIZE];

	printf("debug2\n");

	strcpy(argumentToSend, "");
	strcat(argumentToSend, "\"");
	strcat(argumentToSend, path);
	strcat(argumentToSend, "\" ");

	printf("debug3\n");

	gchar *response = sendToServer("getmenuitems", argumentToSend);
	printf("debug4\n");
	// g_free(argumentToSend);
	printf("debug5\n");

	char *lines[4024];
	int lineCount = 0;

	char delims[] = "\n";
	char *result = response;
	char *nextEnter;
	while ((nextEnter = strchr(result, '\n')) != NULL) {
		lines[lineCount] = calloc(1, (nextEnter - result) + 2);
		lines[lineCount] = strncpy(lines[lineCount], result,
				(nextEnter - result));
		nextEnter++;
		result = nextEnter;
		lineCount++;
	}
	g_free(response);

	int lineIndex = 0;

	while (1) {

		if (strcmp(lines[lineIndex], "--end--") == 0) {
			break;
		}

		gchar *id;
		char *command;
		char *label;
		char *tooltip;
		char *icon;
		char *mode;
		id = g_strdup(lines[lineIndex]);
		lineIndex++;
		label = g_strdup(lines[lineIndex]);
		lineIndex++;
		tooltip = g_strdup(lines[lineIndex]);
		lineIndex++;
		icon = g_strdup(lines[lineIndex]);
		lineIndex++;
		mode = g_strdup(lines[lineIndex]);
		lineIndex++;
		command = g_strdup(lines[lineIndex]);

		if (strchr(mode, 's') != NULL) {
			//GtkToolItem *separator = gtk_separator_menu_item_new();
			//gtk_menu_shell_append(menu, separator);
			//gtk_widget_show(separator);
		}

		// add_subaction(action, GTK_MENU_SHELL (menu), id, label, tooltip,		 stock, command);

		item = nautilus_menu_item_new(id, label, tooltip,
		NULL);
		nautilus_menu_append_item(root_menu, item);

		lineIndex++;
	}

	int i;
	for (i = 0; i <= lineIndex; i++) {
		free(lines[i]);
	}

	/*
	 gchar *argumentToSend = getFilesAsString(file_info->);
	 gchar *response = sendToServer("getmenuitems", argumentToSend);
	 g_free(argumentToSend);

	 char *lines[4024];
	 int lineCount = 0;

	 char delims[] = "\n";
	 char *result = response;
	 char *nextEnter;
	 while ((nextEnter = strchr(result, '\n')) != NULL ) {
	 lines[lineCount] = calloc(1, (nextEnter - result) + 2);
	 lines[lineCount] = strncpy(lines[lineCount], result,
	 (nextEnter - result));
	 nextEnter++;
	 result = nextEnter;
	 lineCount++;
	 }

	 int lineIndex = 0;

	 while (1) {

	 if (strcmp(lines[lineIndex], "--end--") == 0) {
	 break;
	 }

	 gchar *id;
	 char *command;
	 char *label;
	 char *tooltip;
	 char *icon;
	 char *mode;
	 id = g_strdup(lines[lineIndex]);
	 lineIndex++;
	 label = g_strdup(lines[lineIndex]);
	 lineIndex++;
	 tooltip = g_strdup(lines[lineIndex]);
	 lineIndex++;
	 icon = g_strdup(lines[lineIndex]);
	 lineIndex++;
	 mode = g_strdup(lines[lineIndex]);
	 lineIndex++;
	 command = g_strdup(lines[lineIndex]);
	 lineIndex++;

	 gchar *stock = GTK_STOCK_NETWORK;
	 if (strcmp(icon, "pagavcs-other") == 0) {
	 stock = GTK_STOCK_NETWORK;
	 } else if (strcmp(icon, "pagavcs-update") == 0) {
	 stock = GTK_STOCK_REFRESH;
	 } else if (strcmp(icon, "pagavcs-commit") == 0) {
	 stock = GTK_STOCK_APPLY;
	 } else if (strcmp(icon, "pagavcs-difficon") == 0) {
	 stock = GTK_STOCK_FIND_AND_REPLACE;
	 } else if (strcmp(icon, "pagavcs-log") == 0) {
	 stock = GTK_STOCK_INDEX;
	 } else if (strcmp(icon, "pagavcs-drive") == 0) {
	 stock = GTK_STOCK_NETWORK;
	 } else if (strcmp(icon, "pagavcs-ignore") == 0) {
	 stock = GTK_STOCK_MISSING_IMAGE;
	 } else if (strcmp(icon, "pagavcs-unignore") == 0) {
	 stock = GTK_STOCK_UNDELETE;
	 } else if (strcmp(icon, "pagavcs-rename") == 0) {
	 stock = GTK_STOCK_EDIT;
	 } else if (strcmp(icon, "pagavcs-delete") == 0) {
	 stock = GTK_STOCK_DELETE;
	 } else if (strcmp(icon, "pagavcs-revert") == 0) {
	 stock = GTK_STOCK_REDO;
	 } else if (strcmp(icon, "pagavcs-checkout") == 0) {
	 stock = GTK_STOCK_CONNECT;
	 } else if (strcmp(icon, "pagavcs-cleanup") == 0) {
	 stock = GTK_STOCK_CLEAR;
	 } else if (strcmp(icon, "pagavcs-lock") == 0) {
	 stock = GTK_STOCK_DIALOG_AUTHENTICATION;
	 } else if (strcmp(icon, "pagavcs-unlock") == 0) {
	 stock = GTK_STOCK_DIALOG_AUTHENTICATION;
	 } else if (strcmp(icon, "pagavcs-resolve") == 0) {
	 stock = GTK_STOCK_YES;
	 } else if (strcmp(icon, "pagavcs-switch") == 0) {
	 stock = GTK_STOCK_JUMP_TO;
	 } else if (strcmp(icon, "pagavcs-merge") == 0) {
	 stock = GTK_STOCK_CONVERT;
	 } else if (strcmp(icon, "pagavcs-export") == 0) {
	 stock = GTK_STOCK_COPY;
	 } else if (strcmp(icon, "pagavcs-applypatch") == 0) {
	 stock = GTK_STOCK_PASTE;
	 } else if (strcmp(icon, "pagavcs-properties") == 0) {
	 stock = GTK_STOCK_PROPERTIES;
	 } else if (strcmp(icon, "pagavcs-settings") == 0) {
	 stock = GTK_STOCK_PREFERENCES;
	 } else if (strcmp(icon, "pagavcs-blame") == 0) {
	 stock = GTK_STOCK_SPELL_CHECK;
	 }

	 if (strchr(mode, 's') != NULL ) {
	 GtkToolItem *separator = gtk_separator_menu_item_new();
	 gtk_menu_shell_append(menu, separator);
	 gtk_widget_show(separator);
	 }

	 add_subaction(action, GTK_MENU_SHELL (menu), id, label, tooltip,
	 stock, command);
	 }

	 g_free(response);
	 } else {
	 system("pagavcs ping &");
	 GtkToolItem *loading = gtk_menu_item_new_with_label("Loading...");
	 gtk_menu_shell_append(menu, loading);
	 gtk_widget_show(loading);
	 }
	 */

	return toret;
}

GList* nautilus_pagavcs_get_file_items(NautilusMenuProvider* provider,
		GtkWidget* window, GList* files) {

	return NULL;

}

static void nautilus_pagavcs_instance_init(NautilusPagavcs* this) {
}

static void nautilus_pagavcs_class_init(NautilusPagavcsClass* class) {
}

GType nautilus_pagavcs_get_type(void) {
	return pagavcs_type;
}

/*
 static GtkWidget *
 pagavcs_svn_action_create_menu_item(GtkAction *action) {
 GtkWidget *item;
 GtkWidget *menu;
 PagavcsSvnAction *pagavcs_action = PAGAVCS_SVN_ACTION(action);

 item = GTK_ACTION_CLASS(pagavcs_svn_action_parent_class) ->create_menu_item(
 action);

 menu = gtk_menu_new();
 gtk_menu_item_set_submenu(GTK_MENU_ITEM (item), menu);

 if (isServerRunning()) {
 gchar *argumentToSend = getFilesAsString(
 PAGAVCS_SVN_ACTION(action)->files);
 gchar *response = sendToServer("getmenuitems", argumentToSend);
 g_free(argumentToSend);

 char *lines[4024];
 int lineCount = 0;

 char delims[] = "\n";
 char *result = response;
 char *nextEnter;
 while ((nextEnter = strchr(result, '\n')) != NULL ) {
 lines[lineCount] = calloc(1, (nextEnter - result) + 2);
 lines[lineCount] = strncpy(lines[lineCount], result,
 (nextEnter - result));
 nextEnter++;
 result = nextEnter;
 lineCount++;
 }

 int lineIndex = 0;

 while (1) {

 if (strcmp(lines[lineIndex], "--end--") == 0) {
 break;
 }

 gchar *id;
 char *command;
 char *label;
 char *tooltip;
 char *icon;
 char *mode;
 id = g_strdup(lines[lineIndex]);
 lineIndex++;
 label = g_strdup(lines[lineIndex]);
 lineIndex++;
 tooltip = g_strdup(lines[lineIndex]);
 lineIndex++;
 icon = g_strdup(lines[lineIndex]);
 lineIndex++;
 mode = g_strdup(lines[lineIndex]);
 lineIndex++;
 command = g_strdup(lines[lineIndex]);
 lineIndex++;

 gchar *stock = GTK_STOCK_NETWORK;
 if (strcmp(icon, "pagavcs-other") == 0) {
 stock = GTK_STOCK_NETWORK;
 } else if (strcmp(icon, "pagavcs-update") == 0) {
 stock = GTK_STOCK_REFRESH;
 } else if (strcmp(icon, "pagavcs-commit") == 0) {
 stock = GTK_STOCK_APPLY;
 } else if (strcmp(icon, "pagavcs-difficon") == 0) {
 stock = GTK_STOCK_FIND_AND_REPLACE;
 } else if (strcmp(icon, "pagavcs-log") == 0) {
 stock = GTK_STOCK_INDEX;
 } else if (strcmp(icon, "pagavcs-drive") == 0) {
 stock = GTK_STOCK_NETWORK;
 } else if (strcmp(icon, "pagavcs-ignore") == 0) {
 stock = GTK_STOCK_MISSING_IMAGE;
 } else if (strcmp(icon, "pagavcs-unignore") == 0) {
 stock = GTK_STOCK_UNDELETE;
 } else if (strcmp(icon, "pagavcs-rename") == 0) {
 stock = GTK_STOCK_EDIT;
 } else if (strcmp(icon, "pagavcs-delete") == 0) {
 stock = GTK_STOCK_DELETE;
 } else if (strcmp(icon, "pagavcs-revert") == 0) {
 stock = GTK_STOCK_REDO;
 } else if (strcmp(icon, "pagavcs-checkout") == 0) {
 stock = GTK_STOCK_CONNECT;
 } else if (strcmp(icon, "pagavcs-cleanup") == 0) {
 stock = GTK_STOCK_CLEAR;
 } else if (strcmp(icon, "pagavcs-lock") == 0) {
 stock = GTK_STOCK_DIALOG_AUTHENTICATION;
 } else if (strcmp(icon, "pagavcs-unlock") == 0) {
 stock = GTK_STOCK_DIALOG_AUTHENTICATION;
 } else if (strcmp(icon, "pagavcs-resolve") == 0) {
 stock = GTK_STOCK_YES;
 } else if (strcmp(icon, "pagavcs-switch") == 0) {
 stock = GTK_STOCK_JUMP_TO;
 } else if (strcmp(icon, "pagavcs-merge") == 0) {
 stock = GTK_STOCK_CONVERT;
 } else if (strcmp(icon, "pagavcs-export") == 0) {
 stock = GTK_STOCK_COPY;
 } else if (strcmp(icon, "pagavcs-applypatch") == 0) {
 stock = GTK_STOCK_PASTE;
 } else if (strcmp(icon, "pagavcs-properties") == 0) {
 stock = GTK_STOCK_PROPERTIES;
 } else if (strcmp(icon, "pagavcs-settings") == 0) {
 stock = GTK_STOCK_PREFERENCES;
 } else if (strcmp(icon, "pagavcs-blame") == 0) {
 stock = GTK_STOCK_SPELL_CHECK;
 }

 if (strchr(mode, 's') != NULL ) {
 GtkToolItem *separator = gtk_separator_menu_item_new();
 gtk_menu_shell_append(menu, separator);
 gtk_widget_show(separator);
 }

 add_subaction(action, GTK_MENU_SHELL (menu), id, label, tooltip,
 stock, command);
 }

 g_free(response);
 } else {
 system("pagavcs ping &");
 GtkToolItem *loading = gtk_menu_item_new_with_label("Loading...");
 gtk_menu_shell_append(menu, loading);
 gtk_widget_show(loading);
 }

 return item;
 }

 static void pagavcs_action_exec(GtkAction *item, PagavcsSvnAction *action) {

 gchar *command = g_strdup(
 g_object_get_qdata(G_OBJECT (item), pagavcs_action_arg_quark));
 gchar *argumentToSend = getFilesAsString(PAGAVCS_SVN_ACTION(action)->files);
 gchar *response = sendToServer(command, argumentToSend);
 g_free(argumentToSend);
 g_free(command);

 g_free(response);
 }





 */

