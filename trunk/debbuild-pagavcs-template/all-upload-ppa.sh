#!/bin/sh

rm -f -r ../temp-build

for dist in oneiric precise
do
	./upload-ppa.sh $dist
done
