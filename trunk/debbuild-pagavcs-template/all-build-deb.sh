#!/bin/sh

for dist in karmic lucid maverick natty oneiric
do
	./build-deb.sh $dist
done

