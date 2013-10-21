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

#ifndef NAUTILUS_PAGAVCS_PROVIDER_H
#define NAUTILUS_PAGAVCS_PROVIDER_H

#include <glib-object.h>

G_BEGIN_DECLS

#define NAUTILUS_TYPE_PAGAVCS  (nautilus_pagavcs_get_type ())
#define NAUTILUS_PAGAVCS(o)    (G_TYPE_CHECK_INSTANCE_CAST ((o), NAUTILUS_TYPE_PAGAVCS, NautilusPagavcs))
#define NAUTILUS_IS_PAGAVCS(o) (G_TYPE_CHECK_INSTANCE_TYPE ((o), NAUTILUS_TYPE_PAGAVCS))
typedef struct _NautilusPagavcs      NautilusPagavcs;
typedef struct _NautilusPagavcsClass NautilusPagavcsClass;

struct _NautilusPagavcs {
    GObject parent_slot;
};

struct _NautilusPagavcsClass {
    GObjectClass parent_slot;
};

GType nautilus_pagavcs_get_type(void);
void nautilus_pagavcs_register_type(GTypeModule *module);

G_END_DECLS

#endif
