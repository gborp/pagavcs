#!/bin/sh

if [ "$1" = "" ]; then
	echo Usage: ./cmd [distribution-code-name]
	exit 1
fi

OWNDIR=`pwd`
DIST=$1

APPNAME=pagavcs-17-dev
COMMONBUILDDIR=temp-build
VERSION=`cat debian/changelog | head -1 | grep -o "[0-9]*\.[0-9]*\.[0-9]*\-[0-9]*"`
WDIRNAME=${APPNAME}_${VERSION}${DIST}
WDIR=../$COMMONBUILDDIR/$WDIRNAME
export WDIR
export APPNAME
export VERSION
export WDIRNAME

mkdir -p ../$COMMONBUILDDIR
rm -r -f $WDIR
mkdir $WDIR

cd ../java
./build.sh

cd $OWNDIR

cp -r debian $WDIR/debian

sed -i "s/karmic/$DIST/g" $WDIR/debian/changelog

cp -R ../c/* $WDIR

mkdir $WDIR/debian/input
mkdir $WDIR/debian/input/doc
mkdir -p $WDIR/debian/input/icons/hicolor/scalable/actions
mkdir -p $WDIR/debian/input/icons/hicolor/scalable/apps
mkdir -p $WDIR/debian/input/icons/hicolor/scalable/emblems
cp ../java/dist/pagavcs.jar $WDIR/debian/input/
cp ../java/dist/pagavcs-libs.jar $WDIR/debian/input/
cp ../java/dist/icon.png $WDIR/debian/input/
cp ../doc/* $WDIR/debian/input/doc
cp ../icons/hicolor/scalable/actions/* $WDIR/debian/input/icons/hicolor/scalable/actions
cp ../icons/hicolor/scalable/emblems/* $WDIR/debian/input/icons/hicolor/scalable/emblems



