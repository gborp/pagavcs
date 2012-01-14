#!/bin/sh

APPNAME=pagavcs
VERSION=`cat debian/changelog | head -1 | grep -o "[0-9]*\.[0-9]*\.[0-9]*\-[0-9]*"`
WDIRNAME=$APPNAME-${VERSION}
WDIR=../$WDIRNAME

./common.sh

cd $WDIR
debuild -sa

rm -r $WDIR
rmdir $WDIR

