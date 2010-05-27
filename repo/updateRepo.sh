#!/bin/sh
reprepro --keepunreferencedfiles -b . includedeb karmic ../trunk/*.deb
reprepro --keepunreferencedfiles -b . includedeb karmic ../trunk-mugcommander/*.deb
reprepro --keepunreferencedfiles -b . includedeb karmic ../trunk-mugcommander-gnome-extension/*.deb
reprepro --keepunreferencedfiles -b . includedeb karmic ../trunk-mugcommander-pagavcs-extension/*.deb
reprepro --keepunreferencedfiles -b . includedeb karmic ../trunk-mugcommnader-bonjour-extension/*.deb
