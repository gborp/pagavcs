#!/bin/sh

rm -f -r ../temp-build

for dist in lucid maverick natty oneiric precise
do
	./build-deb.sh $dist
done

