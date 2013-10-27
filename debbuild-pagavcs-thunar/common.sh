#!/bin/sh

if [ "$1" = "" ]; then
	echo Usage: ./cmd [distribution-code-name]
	exit 1
fi

OWNDIR=`pwd`
DIST=$1

APPNAME=pagavcs-17-thunar
COMMONBUILDDIR=temp-build
VERSION=`../version.sh`
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
cd $OWNDIR

cp -r debian $WDIR/debian

echo "${APPNAME} (${VERSION}${DIST}) ${DIST}; urgency=low" > $WDIR/debian/changelog
echo "" >> $WDIR/debian/changelog
echo "  * release" >> $WDIR/debian/changelog
echo "" >> $WDIR/debian/changelog
# date -R
echo " -- Gábor Pápai <gabor.papai@gmail.com>  Tue, 01 Jan 2013 00:00:00 +0100">> $WDIR/debian/changelog

mkdir $WDIR/debian/input
cp -R ../thunar-pagavcs/* $WDIR
find $WDIR/ -type d -name .svn | xargs rm -fr 

