/*-
 * Copyright (c) 2012 Gabor Papai <pagavcs@gmail.com>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public
 * License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA 02111-1307, USA.
 */

#ifdef HAVE_CONFIG_H
#include <config.h>
#endif

#ifdef HAVE_UNISTD_H
#include <unistd.h>
#endif

#ifdef HAVE_STRING_H
#include <string.h>
#endif

#include <pagavcs-provider.h>
#include <pagavcs-svn-action.h>

#define g_access(filename, mode) access((filename), (mode))

static void pagavcs_provider_menu_provider_init(ThunarxMenuProviderIface *iface);
static void pagavcs_provider_finalize(GObject *object);
static GList *pagavcs_provider_get_file_actions(
		ThunarxMenuProvider *menu_provider, GtkWidget *window, GList *files);
static GList *pagavcs_provider_get_folder_actions(
		ThunarxMenuProvider *menu_provider, GtkWidget *window,
		ThunarxFileInfo *folder);

struct _PagavcsProviderClass {
	GObjectClass __parent__;
};

struct _PagavcsProvider {
	GObject __parent__;
};

THUNARX_DEFINE_TYPE_WITH_CODE(
		PagavcsProvider,
		pagavcs_provider,
		G_TYPE_OBJECT,
		THUNARX_IMPLEMENT_INTERFACE (THUNARX_TYPE_MENU_PROVIDER, pagavcs_provider_menu_provider_init));

static void pagavcs_provider_class_init(PagavcsProviderClass *klass) {
	GObjectClass *gobject_class;

	gobject_class = G_OBJECT_CLASS (klass);
	gobject_class->finalize = pagavcs_provider_finalize;
}

static void pagavcs_provider_menu_provider_init(ThunarxMenuProviderIface *iface) {
	iface->get_file_actions = pagavcs_provider_get_file_actions;
	iface->get_folder_actions = pagavcs_provider_get_folder_actions;
}

static void pagavcs_provider_init(PagavcsProvider *pagavcs_provider) {
}

static void pagavcs_provider_finalize(GObject *object) {
	PagavcsProvider *pagavcs_provider = PAGAVCS_PROVIDER(object);

	(*G_OBJECT_CLASS (pagavcs_provider_parent_class)->finalize)(object);
}

static GList*
pagavcs_provider_get_file_actions(ThunarxMenuProvider *menu_provider,
		GtkWidget *window, GList *files) {
	GList *actions = NULL;
	GtkAction *action;
	GList *lp;
	gint n_files = 0;
	gchar *scheme;

	for (lp = files; lp != NULL; lp = lp->next, ++n_files) {
		scheme = thunarx_file_info_get_uri_scheme(lp->data);

		if (G_UNLIKELY (strcmp (scheme, "file"))) {
			g_free(scheme);
			return NULL;
		}
		g_free(scheme);
	}

	action = pagavcs_svn_action_new("PagaVCS::submenu", "PagaVCS", files);
	actions = g_list_append(actions, action);

	return actions;
}

static GList*
pagavcs_provider_get_folder_actions(ThunarxMenuProvider *menu_provider,
		GtkWidget *window, ThunarxFileInfo *folder) {
	GList *files;
	files = g_list_append(NULL, folder);

	GList *result;
	result = pagavcs_provider_get_file_actions(menu_provider, window, files);
	g_list_free(files);
	return result;
}

