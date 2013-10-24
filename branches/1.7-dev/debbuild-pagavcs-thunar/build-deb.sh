#!/bin/sh

if [ "$1" = "" ]; then
	echo Usage: ./cmd [distribution-code-name]
	exit 1
fi

. ./common.sh $1


echo wdir: $WDIR
cd $WDIR
echo y | debuild -ai386  -sa
echo y | debuild -aamd64 -sa

rm -r -f $WDIR
