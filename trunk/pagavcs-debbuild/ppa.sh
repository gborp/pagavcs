#!/bin/sh
cd ..
rm -r --force ppa-build
rmdir ppa-build
#cp -R pagavcs-debbuild/* ppa-build
svn export pagavcs-debbuild ppa-build
cd ppa-build
./build.sh
debuild -S
