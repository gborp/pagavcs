#!/bin/sh

if [ "$1" = "" ]; then
	echo Usage: ./cmd [distribution-code-name]
	exit 1
fi

. ./common.sh $1

cd $WDIR
debuild -S -sa

cd ..
dput ppa:gaborgabor/pagavcs ${WDIRNAME}_source.changes 


rm -r -f $WDIRNAME
