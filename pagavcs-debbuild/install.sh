#!/bin/sh
#sudo rm ../*.deb
./build.sh
cd ../..
sudo dpkg -i `ls trunk/*.deb`
mv -f trunk/*.deb binary
dpkg-scanpackages binary /dev/null | gzip -9c > binary/Packages.gz
cd binary
apt-ftparchive release . > Release
rm -f Release.gpg
gpg --keyserver keyserver.ubuntu.com -abs -o Release.gpg Release
#nautilus -q
