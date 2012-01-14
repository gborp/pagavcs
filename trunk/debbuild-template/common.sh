#!/bin/sh

APPNAME=pagavcs
VERSION=`cat debian/changelog | head -1 | grep -o "[0-9]*\.[0-9]*\.[0-9]*\-[0-9]*"`
WDIRNAME=$APPNAME_${VERSION}
WDIR=../$WDIRNAME
export WDIR

rm -r $WDIR
rmdir $WDIR
mkdir $WDIR

cd ../java
./build.sh
cd ../debbuild-template

cp -r debian $WDIR/debian

mkdir $WDIR/debian/input
mkdir $WDIR/debian/input/gnome3
mkdir $WDIR/debian/input/doc
mkdir -p $WDIR/debian/input/icons/hicolor/scalable/actions
mkdir -p $WDIR/debian/input/icons/hicolor/scalable/apps
mkdir -p $WDIR/debian/input/icons/hicolor/scalable/emblems
cp ../java/dist/pagavcs.jar $WDIR/debian/input/
cp ../java/dist/icon.png $WDIR/debian/input/
cp ../scripts/pagavcs $WDIR/debian/input/
cp ../scripts/pagavcs-nautilus.py $WDIR/debian/input/
cp ../scripts/gnome3/pagavcs-nautilus.py $WDIR/debian/input/gnome3
cp ../doc/* $WDIR/debian/input/doc
cp ../icons/hicolor/scalable/actions/* $WDIR/debian/input/icons/hicolor/scalable/actions
#cp ../icons/hicolor/scalable/apps/*    $WDIR/debian/input/icons/hicolor/scalable/apps
cp ../icons/hicolor/scalable/emblems/* $WDIR/debian/input/icons/hicolor/scalable/emblems

