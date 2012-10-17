#!/bin/sh

rm -f -r ../temp-build

for dist in quantal
do
	./build-deb.sh $dist
done

