#!/bin/sh

rm -f -r ../temp-build

for dist in oneiric precise quantal
do
	./upload-ppa.sh $dist
done
