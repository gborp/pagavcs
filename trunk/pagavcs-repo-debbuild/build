#!/bin/sh
rm -r debian/input/*
rmdir debian/input
sudo rm -r debian/pagavcs-repo/*
sudo rmdir debian/pagavcs-repo
mkdir debian/input
mkdir debian/input/doc
cp ../doc/* debian/input/doc
sudo ./rules
rm -r debian/input/*
rmdir debian/input
sudo rm -r debian/pagavcs-repo/*
sudo rmdir debian/pagavcs-repo
cd ..
mv -f pagavcs-repo_1.0.0ubuntu_all.deb ../binary/Packages/
