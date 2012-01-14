#!/bin/sh

for dist in karmic lucid maverick natty oneiric
do
	./upload-ppa.sh $dist
done
