#!/bin/sh

rm -f -r ../temp-build

for dist in karmic lucid maverick natty oneiric
do
	./build-deb.sh $dist
done

