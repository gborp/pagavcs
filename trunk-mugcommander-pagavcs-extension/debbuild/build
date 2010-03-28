#!/bin/sh
cd ../java
./build.sh
cd ../debbuild
rm -r debian/input/*
rmdir debian/input
sudo rm -r debian/mugcommander-pagavcs-extensions/*
sudo rmdir debian/mugcommander-pagavcs-extensions
mkdir debian/input
mkdir debian/input/cfg
cp ../java/dist/mugcommander-pagavcs-extensions.jar debian/input/
cp ../mugcommander-config/* debian/input/cfg/
cp rules debian/
#cd debian
sudo ./rules
rm -r debian/input/*
rmdir debian/input
