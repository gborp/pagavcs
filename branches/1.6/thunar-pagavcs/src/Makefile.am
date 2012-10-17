# what flags you want to pass to the C compiler & linker
AM_CFLAGS = --pedantic -Wall -std=c99 -O2
AM_LDFLAGS =

#INCLUDES =								\
#	-DG_LOG_DOMAIN=\"ThunarPagavcs\"				

# this lists the binaries to produce, the (non-PHONY, binary) targets in
# the previous manual Makefile
extensionsdir = $(THUNARX_LIBDIR)/thunarx-2
extensions_LTLIBRARIES = thunar_pagavcs.la

thunar_pagavcs_la_CFLAGS  = $(GLIBDEPS_CFLAGS) $(THUNARXDEPS_CFLAGS)
thunar_pagavcs_la_SOURCES = thunar-pagavcs.c pagavcs-provider.c pagavcs-provider.h pagavcs-svn-action.c pagavcs-svn-action.h
thunar_pagavcs_la_LIBS    = $(GLIBDEPS_LIBS) $(THUNARXDEPS_LIBS)
thunar_pagavcs_la_LDFLAGS = -avoid-version -export-dynamic -module $(THUNARX_LDFLAGS)

