#!/bin/sh

rm -f -r ../temp-build

cat ../ubuntu-distros.txt | while read dist
do
	./upload-ppa.sh $dist
done

