#!/bin/sh

rm -f -r ../temp-build

for dist in oneiric precise
do
	./build-deb.sh $dist
done

