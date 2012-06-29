#!/bin/sh
rm -r debian/input/*
rmdir debian/input
sudo rm -r debian/pagavcs-bigmem/*
sudo rmdir debian/pagavcs-bigmem
mkdir debian/input
cp ../scripts/pagavcs-bigmem debian/input/
sudo ./rules

user=`whoami`
sudo chown -R $user *
