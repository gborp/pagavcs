#!/bin/sh
cd ../java
./build.sh
cd ../debbuild
rm -r debian/input/*
rmdir debian/input
sudo rm -r debian/mugcommander-gnome-extensions/*
sudo rmdir debian/mugcommander-gnome-extensions
mkdir debian/input
mkdir debian/input/cfg
cp ../java/dist/mugcommander-gnome-extensions.jar debian/input/
cp ../mugcommander-config/* debian/input/cfg/
cp rules debian/
#cd debian
sudo ./rules
rm -r debian/input/*
rmdir debian/input
