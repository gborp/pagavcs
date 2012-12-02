#!/bin/sh

rm -f -r ../temp-build

for dist in oneiric precise quantal raring
do
	./upload-ppa.sh $dist
done
