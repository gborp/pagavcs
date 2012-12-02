#!/bin/sh

rm -f -r ../temp-build

for dist in oneiric precise quantal raring
do
	./build-deb.sh $dist
done

