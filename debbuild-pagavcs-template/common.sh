#!/bin/sh

if [ "$1" = "" ]; then
	echo Usage: ./cmd [distribution-code-name]
	exit 1
fi

OWNDIR=`pwd`
DIST=$1

APPNAME=pagavcs-17
COMMONBUILDDIR=temp-build
VERSION=`../version.sh`
WDIRNAME=${APPNAME}_${VERSION}${DIST}
WDIR=../$COMMONBUILDDIR/$WDIRNAME
export WDIR
export APPNAME
export VERSION
export WDIRNAME

rm -r -f ../$COMMONBUILDDIR
mkdir -p ../$COMMONBUILDDIR
rm -r -f $WDIR
mkdir $WDIR

cd ../java
./build.sh

cd $OWNDIR

cp -r debian $WDIR/debian

echo "${APPNAME} (${VERSION}${DIST}) ${DIST}; urgency=low" > $WDIR/debian/changelog
echo "" >> $WDIR/debian/changelog
echo "  * release" >> $WDIR/debian/changelog
echo "" >> $WDIR/debian/changelog
# date -R
echo " -- PagaVCS <pagavcs@gmail.com>  Tue, 01 Jan 2013 00:00:00 +0100">> $WDIR/debian/changelog

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
cp ../icons/hicolor/scalable/emblems/* $WDIR/debian/input/icons/hicolor/scalable/emblem
echo ${APPNAME} ${VERSION}${DIST} > $WDIR/debian/input/version.txt

