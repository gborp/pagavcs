#!/bin/sh

APPNAME=pagavcs
VERSION=`cat debian/changelog | head -1 | grep -o "[0-9]*\.[0-9]*\.[0-9]*\-[0-9]*"`
WDIRNAMESHORT=${APPNAME}_${VERSION}
WDIRNAME=${WDIRNAMESHORT}~all

./common.sh
cd $WDIR
debuild -S -sa

cd ..
#cp ${WDIRNAMESHORT}_source.changes ${WDIRNAME}_source.changes 
dput ppa:gaborgabor/pagavcs ${WDIRNAME}_source.changes 
