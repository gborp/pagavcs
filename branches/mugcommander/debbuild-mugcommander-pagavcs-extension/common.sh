#!/bin/sh

if [ "$1" = "" ]; then
	echo Usage: ./cmd [distribution-code-name]
	exit 1
fi

OWNDIR=`pwd`
DIST=$1

APPNAME=mugcommander-pagavcs-extensions
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

cd ../mugcommander-pagavcs-extension/java
./build.sh
cd $OWNDIR

cp -r debian $WDIR/debian

sed -i "s/karmic/$DIST/g" $WDIR/debian/changelog

mkdir $WDIR/debian/input
mkdir $WDIR/debian/input/cfg

cp ../mugcommander-pagavcs-extension/java/dist/mugcommander-pagavcs-extensions.jar $WDIR/debian/input/
cp ../mugcommander-pagavcs-extension/mugcommander-config/* $WDIR/debian/input/cfg/
