#!/bin/sh
ls -1 ../trunk/*.deb > /dev/null 2>&1
if [ "$?" = "0" ]; then
	reprepro --ask-passphrase --keepunreferencedfiles -b . includedeb karmic ../trunk/*.deb
fi

ls -1 ../trunk-mugcommander/*.deb > /dev/null 2>&1
if [ "$?" = "0" ]; then
	reprepro --ask-passphrase --keepunreferencedfiles -b . includedeb karmic ../trunk-mugcommander/*.deb
fi

ls -1 ../trunk-mugcommander-gnome-extension/*.deb > /dev/null 2>&1
if [ "$?" = "0" ]; then
	reprepro --ask-passphrase --keepunreferencedfiles -b . includedeb karmic ../trunk-mugcommander-gnome-extension/*.deb
fi

ls -1 ../trunk-mugcommander-pagavcs-extension/*.deb > /dev/null 2>&1
if [ "$?" = "0" ]; then
	reprepro --ask-passphrase --keepunreferencedfiles -b . includedeb karmic ../trunk-mugcommander-pagavcs-extension/*.deb
fi

ls -1 ../trunk-mugcommnader-bonjour-extension/*.deb > /dev/null 2>&1
if [ "$?" = "0" ]; then
	reprepro --ask-passphrase --keepunreferencedfiles -b . includedeb karmic ../trunk-mugcommnader-bonjour-extension/*.deb
fi
