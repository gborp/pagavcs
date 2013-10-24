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

#ifndef __PAGAVCS_PROVIDER_H__
#define __PAGAVCS_PROVIDER_H__

#include <thunarx/thunarx.h>

G_BEGIN_DECLS;

typedef struct _PagavcsProviderClass PagavcsProviderClass;
typedef struct _PagavcsProvider PagavcsProvider;

#define PAGAVCS_TYPE_PROVIDER             (pagavcs_provider_get_type ())
#define PAGAVCS_PROVIDER(obj)             (G_TYPE_CHECK_INSTANCE_CAST ((obj), PAGAVCS_TYPE_PROVIDER, PagavcsProvider))
#define PAGAVCS_PROVIDER_CLASS(klass)     (G_TYPE_CHECK_CLASS_CAST ((klass), PAGAVCS_TYPE_PROVIDER, PagavcsProviderClass))
#define PAGAVCS_IS_PROVIDER(obj)          (G_TYPE_CHECK_INSTANCE_TYPE ((obj), PAGAVCS_TYPE_PROVIDER))
#define PAGAVCS_IS_PROVIDER_CLASS(klass)  (G_TYPE_CHECK_CLASS_TYPE ((klass), PAGAVCS_TYPE_PROVIDER))
#define PAGAVCS_PROVIDER_GET_CLASS(obj)   (G_TYPE_INSTANCE_GET_CLASS ((obj), PAGAVCS_TYPE_PROVIDER, PagavcsProviderClass))

GType pagavcs_provider_get_type(void) G_GNUC_CONST G_GNUC_INTERNAL;
void pagavcs_provider_register_type(ThunarxProviderPlugin *plugin) G_GNUC_INTERNAL;


G_END_DECLS;

#endif
