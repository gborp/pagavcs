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

#include <gmodule.h>
#include <glib.h>
#include <gtk/gtk.h>

static GType type_list[1];

void nautilus_module_initialize(GTypeModule *module) {

	g_print("Initializing Nautilus-PagaVCS extension\n");

	nautilus_pagavcs_register_type (module);
	type_list[0] = NAUTILUS_TYPE_PAGAVCS;

}

void nautilus_module_shutdown(void) {
	g_print("Shutting down Nautilus-PagaVCS extension\n");
}

void nautilus_module_list_types(const GType **types, int *num_types) {
	*types = type_list;
	*num_types = G_N_ELEMENTS(type_list);
}
