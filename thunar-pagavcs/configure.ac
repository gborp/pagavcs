#                                               -*- Autoconf -*-
# Process this file with autoconf to produce a configure script.

AC_PREREQ([2.68])
AC_INIT([thunar-plugin], [1.5.2], [pagavcs@gmail.com])
AC_CONFIG_SRCDIR([src])
AC_CONFIG_HEADERS([config.h])

PKG_CHECK_MODULES([GLIBDEPS], [glib-2.0])
PKG_CHECK_MODULES([THUNARXDEPS], [thunarx-2])

LT_INIT

THUNARX_API_VERSION=2
prefix=`pkg-config --variable prefix thunarx-2`
AC_SUBST(THUNARX_API_VERSION)
 
THUNARX_LIBDIR=`pkg-config --variable=libdir thunarx-${THUNARX_API_VERSION}`
AC_SUBST(THUNARX_LIBDIR)
AC_DEFINE_UNQUOTED(THUNARX_LIBDIR, "$THUNARX_LIBDIR", [Thunarx libdir])


dnl ---------------------------------
dnl Get thunar extensions directory
dnl ---------------------------------
THUNARX_EXTENSION_DIR=`pkg-config --variable=extensionsdir thunarx-${THUNARX_API_VERSION}`
AC_SUBST(THUNARX_EXTENSION_DIR)
AC_DEFINE_UNQUOTED(THUNARX_EXTENSION_DIR, "$THUNARX_EXTENSION_DIR", [Thunar's extensions directory])

AM_INIT_AUTOMAKE
AC_OUTPUT(Makefile src/Makefile)

# Checks for programs.
AC_PROG_CC
AC_PROG_INSTALL
AC_PROG_RANLIB
AM_PROG_CC_C_O

# Checks for libraries.

# Checks for header files.
AC_CHECK_HEADERS([memory.h string.h])

# Checks for typedefs, structures, and compiler characteristics.

# Checks for library functions.

AC_OUTPUT
