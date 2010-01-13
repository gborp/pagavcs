#!/bin/sh
sudo rm ../*.deb
./build.sh
sudo dpkg -i `ls ../*.deb`
#nautilus -q
