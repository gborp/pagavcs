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

#ifndef __PAGAVCS_SVN_ACTION_H__
#define __PAGAVCS_SVN_ACTION_H__

#include <gtk/gtk.h>
#include <thunarx/thunarx.h>

G_BEGIN_DECLS;

typedef struct _PagavcsSvnActionClass PagavcsSvnActionClass;
typedef struct _PagavcsSvnAction PagavcsSvnAction;

#define PAGAVCS_TYPE_SVN_ACTION             (pagavcs_svn_action_get_type ())
#define PAGAVCS_SVN_ACTION(obj)             (G_TYPE_CHECK_INSTANCE_CAST ((obj), PAGAVCS_TYPE_SVN_ACTION, PagavcsSvnAction))
#define PAGAVCS_SVN_ACTION_CLASS(klass)     (G_TYPE_CHECK_CLASS_CAST ((klass), PAGAVCS_TYPE_SVN_ACTION, PagavcsSvnActionClass))
#define PAGAVCS_IS_SVN_ACTION(obj)          (G_TYPE_CHECK_INSTANCE_TYPE ((obj), PAGAVCS_TYPE_SVN_ACTION))
#define PAGAVCS_IS_SVN_ACTION_CLASS(klass)  (G_TYPE_CHECK_CLASS_TYPE ((klass), PAGAVCS_TYPE_SVN_ACTION))
#define PAGAVCS_SVN_ACTION_GET_CLASS(obj)   (G_TYPE_INSTANCE_GET_CLASS ((obj), PAGAVCS_TYPE_SVN_ACTION, PagavcsSvnActionClass))

GType pagavcs_svn_action_get_type(void) G_GNUC_CONST G_GNUC_INTERNAL;
void pagavcs_svn_action_register_type(ThunarxProviderPlugin *) G_GNUC_INTERNAL;

GtkAction *pagavcs_svn_action_new(const gchar*, const gchar*, GList *,
		GtkWidget *) G_GNUC_MALLOC G_GNUC_INTERNAL;

G_END_DECLS;

#endif
