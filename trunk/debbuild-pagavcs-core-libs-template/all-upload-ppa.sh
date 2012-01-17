#!/bin/sh

rm -f -r ../temp-build

for dist in karmic lucid maverick natty oneiric
do
	./upload-ppa.sh $dist
done
