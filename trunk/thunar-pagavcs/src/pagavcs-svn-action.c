/*-
 * Copyright (c) 2012 Gabor Papai <pagavcs@gmail.com>
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
#include <config.h>
#endif

#include <thunarx/thunarx.h>

#include <pagavcs-svn-action.h>

#include <string.h>
#include <sys/wait.h>

#include <stdio.h>
#include <sys/types.h>
#include <stdlib.h>
#include <pwd.h>
#include <string.h>
#include <sys/socket.h>
#include <linux/un.h>
#include <unistd.h>
#include <errno.h>
#include <glib.h>

#define SOCKET_FILENAME "/.pagavcs/socket"
#define LOG_FILENAME "/.pagavcs/pagavcs.log"

struct _PagavcsSvnActionClass {
	GtkActionClass __parent__;
};

struct _PagavcsSvnAction {
	GtkAction __parent__;

	struct {
	} property;

	GList *files;
};

static GQuark pagavcs_action_arg_quark = 0;

static GtkWidget *pagavcs_svn_action_create_menu_item(GtkAction *action);

static void pagavcs_svn_action_finalize(GObject*);

static void pagavcs_svn_action_set_property(GObject*, guint, const GValue*,
		GParamSpec*);

static void pagavcs_action_exec(GtkAction *item,
		PagavcsSvnAction *pagavcs_action);

static char* sendToServer(char *command, char *path);

THUNARX_DEFINE_TYPE(PagavcsSvnAction, pagavcs_svn_action, GTK_TYPE_ACTION)

static void pagavcs_svn_action_class_init(PagavcsSvnActionClass *klass) {
	GObjectClass *gobject_class = G_OBJECT_CLASS (klass);
	GtkActionClass *gtkaction_class = GTK_ACTION_CLASS (klass);

	gobject_class->finalize = pagavcs_svn_action_finalize;
	gobject_class->set_property = pagavcs_svn_action_set_property;

	gtkaction_class->create_menu_item = pagavcs_svn_action_create_menu_item;
	pagavcs_action_arg_quark = g_quark_from_static_string("pagavcs-action-arg");
}

static void pagavcs_svn_action_init(PagavcsSvnAction *self) {
	self->files = NULL;
}

GtkAction *
pagavcs_svn_action_new(const gchar *name, const gchar *label, GList *files) {
	GtkAction *action;

	g_return_val_if_fail(name, NULL);
	g_return_val_if_fail(label, NULL);

	action = g_object_new(PAGAVCS_TYPE_SVN_ACTION, "hide-if-empty", FALSE,
			"name", name, "label", label, "icon-name", GTK_STOCK_FLOPPY, NULL);
	PAGAVCS_SVN_ACTION (action)->files = thunarx_file_info_list_copy(files);
	return action;
}

static void pagavcs_svn_action_finalize(GObject *object) {
	thunarx_file_info_list_free(PAGAVCS_SVN_ACTION (object)->files);
	PAGAVCS_SVN_ACTION (object)->files = NULL;

	G_OBJECT_CLASS (pagavcs_svn_action_parent_class)->finalize(object);
}

static void pagavcs_svn_action_set_property(GObject *object, guint property_id,
		const GValue *value, GParamSpec *pspec) {
	switch (property_id) {
	default:
		G_OBJECT_WARN_INVALID_PROPERTY_ID(object, property_id, pspec);
		break;
	}
}

static void add_subaction(GtkAction *action, GtkMenuShell *menu,
		const gchar *name, const gchar *text, const gchar *tooltip,
		const gchar *stock, const gchar *command) {
	GtkAction *subaction;
	GtkWidget *subitem;

	subaction = gtk_action_new(name, text, tooltip, stock);
	g_object_set_qdata(G_OBJECT (subaction), pagavcs_action_arg_quark, command);
	g_signal_connect_after(subaction, "activate",
			G_CALLBACK (pagavcs_action_exec), action);

	subitem = gtk_action_create_menu_item(subaction);
	g_object_get(G_OBJECT (subaction), "tooltip", &tooltip, NULL);
	gtk_widget_set_tooltip_text(subitem, tooltip);
	gtk_menu_shell_append(menu, subitem);
	gtk_widget_show(subitem);
}

static gchar *getFilesAsString(GList *files) {
	GList *lp;
	gchar *path;
	gchar *uri;
	char argumentToSend[16384];

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
}

static GtkWidget *
pagavcs_svn_action_create_menu_item(GtkAction *action) {
	GtkWidget *item;
	GtkWidget *menu;
	PagavcsSvnAction *pagavcs_action = PAGAVCS_SVN_ACTION (action);

	item = GTK_ACTION_CLASS(pagavcs_svn_action_parent_class)->create_menu_item(
			action);

	menu = gtk_menu_new();
	gtk_menu_item_set_submenu(GTK_MENU_ITEM (item), menu);

	gchar *argumentToSend = getFilesAsString(PAGAVCS_SVN_ACTION(action)->files);
	gchar *response = sendToServer("getmenuitems", argumentToSend);
	g_free(argumentToSend);

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

		add_subaction(action, GTK_MENU_SHELL (menu), id, label, tooltip, stock,
				command);
	}

	g_free(response);

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

static void connectServer(int socket_fd, struct sockaddr_un address) {
	int first = 1;
	while (connect(socket_fd, (struct sockaddr *) &address,
			sizeof(struct sockaddr_un)) != 0) {
		if (first == 1) {
			int systemRetValue;
			systemRetValue = system("pagavcs ping");

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
	char buffer[16384];
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
	char buffer[1024];
	strcpy(buffer, "\"");
	strcat(buffer, command);
	strcat(buffer, "\"");
	strcat(buffer, " ");
	strcat(buffer, path);
	return sendToServerCommand(buffer);
}
