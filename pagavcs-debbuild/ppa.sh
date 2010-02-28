#!/bin/sh
cd ..
sudo rm -r --force ppa-build
svn export pagavcs-debbuild ppa-build
cd ppa-build
./build.sh
sudo debuild -S -k3FAEE1DA
