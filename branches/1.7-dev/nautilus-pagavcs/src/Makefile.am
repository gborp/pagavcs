INCLUDES =						\
	-I$(top_srcdir)					\
	-I$(top_builddir)

nautilus_extensiondir=$(NAUTILUS_EXTENSION_DIR)

nautilus_extension_LTLIBRARIES=libnautilus-pagavcs.la

libnautilus_pagavcs_la_CFLAGS = 	                \
	-Wall                                           \
	$(WARN_CFLAGS)                                  \
	$(DISABLE_DEPRECATED_CFLAGS)					\
	$(NAUTILUS_CFLAGS)                              \
	$(GLIB_CFLAGS)

if DEBUG
libnautilus_pagavcs_la_CFLAGS += -DND_DEBUG
else
libnautilus_pagavcs_la_CFLAGS += -DG_DISABLE_ASSERT -DG_DISABLE_CHECKS
endif

libnautilus_pagavcs_la_SOURCES = \
	nautilus-pagavcs.c       \
	nautilus-pagavcs-provider.h \
	nautilus-pagavcs-provider.c

libnautilus_pagavcs_la_LDFLAGS = -module -avoid-version
libnautilus_pagavcs_la_LIBADD  = $(NAUTILUS_LIBS) $(GLIB_LIBS)
