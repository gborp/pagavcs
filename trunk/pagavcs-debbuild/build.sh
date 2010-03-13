#!/bin/sh
cd ../../trunk-mugcommander/java
./build.sh
cd ../../trunk/java
./build.sh
cd ../ppa-build
rm -r debian/input/*
rmdir debian/input
sudo rm -r debian/pagavcs/*
sudo rmdir debian/pagavcs
mkdir debian/input
mkdir debian/input/doc
mkdir -p debian/input/icons/hicolor/scalable/actions
mkdir -p debian/input/icons/hicolor/scalable/apps
mkdir -p debian/input/icons/hicolor/scalable/emblems
cp ../java/dist/pagavcs.jar debian/input/
cp ../../trunk-mugcommander/java/dist/mugcommander.jar debian/input/
cp ../java/dist/icon.png debian/input/
cp ../../trunk-mugcommander/java/dist/mugcommander-icon.png debian/input/
cp ../scripts/pagavcs debian/input/
cp ../scripts/pagavcs-nautilus.py debian/input/
cp ../scripts/mugcommander debian/input/
cp ../scripts/mugcommander.desktop debian/input/
cp ../doc/* debian/input/doc
cp ../icons/hicolor/scalable/actions/* debian/input/icons/hicolor/scalable/actions
cp ../icons/hicolor/scalable/apps/* debian/input/icons/hicolor/scalable/apps
cp ../icons/hicolor/scalable/emblems/* debian/input/icons/hicolor/scalable/emblems
cp rules debian/
#cd debian
sudo ./rules
#rm -r debian/input/*
#rmdir debian/input
#sudo rm -r debian/pagavcs/*
#sudo rmdir debian/pagavcs
