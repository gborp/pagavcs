#!/bin/sh

rm -f -r ../temp-build

for dist in precise quantal raring
do
	./build-deb.sh $dist
done

