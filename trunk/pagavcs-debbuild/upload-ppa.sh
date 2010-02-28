#!/bin/sh
cd ..
dput ppa:gaborgabor/pagavcs *.changes
rm *.dsc
rm *.tar.gz
#rm --force *.deb
rm *.build
rm *.changes
cat *.upload
rm *.upload
