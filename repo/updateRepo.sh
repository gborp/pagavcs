#!/bin/bash

function update_pagavcs {
	ls -1 ../trunk/debbuild-temp/*$1*.deb > /dev/null 2>&1
	if [ "$?" = "0" ]; then
		reprepro --ask-passphrase --keepunreferencedfiles -b . includedeb $1 ../trunk/debbuild-temp/*$1*.deb
	fi
}

update_pagavcs karmic
update_pagavcs lucid
update_pagavcs maverick
update_pagavcs natty
update_pagavcs oneiric


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
