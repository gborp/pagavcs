#!/bin/sh
cd ..
dput ppa:gaborgabor/pagavcs *.changes
rm --force *.dsc
rm --force *.tar.gz
#rm --force *.deb
rm --force *.build
rm --force *.changes
cat *.upload
rm --force *.upload
