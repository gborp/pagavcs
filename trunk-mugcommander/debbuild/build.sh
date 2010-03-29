#!/bin/sh
cd ../java
./build.sh
cd ../debbuild
rm -r debian/input/*
rmdir debian/input
sudo rm -r debian/mugcommander/*
sudo rmdir debian/mugcommander
mkdir debian/input
mkdir debian/input/doc
cp ../java/dist/mugcommander.jar debian/input/
cp ../java/dist/mugcommander-icon.png debian/input/
cp ../scripts/mugcommander debian/input/
cp ../scripts/mugcommander.desktop debian/input/
cp ../doc/* debian/input/doc
sudo ./rules
