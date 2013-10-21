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

#include <pagavcs-provider.h>
#include <pagavcs-svn-action.h>

static GType type_list[1];

G_MODULE_EXPORT void thunar_extension_initialize(ThunarxProviderPlugin *plugin);

G_MODULE_EXPORT void thunar_extension_initialize(ThunarxProviderPlugin *plugin) {
	const gchar *mismatch;

	mismatch = thunarx_check_version(THUNARX_MAJOR_VERSION,
			THUNARX_MINOR_VERSION, THUNARX_MICRO_VERSION);
	if (G_UNLIKELY (mismatch != NULL)) {
		g_warning("Version mismatch: %s", mismatch);
		return;
	}

	pagavcs_provider_register_type(plugin);
	pagavcs_svn_action_register_type(plugin);

	type_list[0] = PAGAVCS_TYPE_PROVIDER;
}

G_MODULE_EXPORT void thunar_extension_shutdown(void);

G_MODULE_EXPORT void thunar_extension_shutdown(void) {
}

G_MODULE_EXPORT void thunar_extension_list_types(const GType **types,
		gint *n_types);

G_MODULE_EXPORT void thunar_extension_list_types(const GType **types,
		gint *n_types) {
	*types = type_list;
	*n_types = G_N_ELEMENTS (type_list);
}

