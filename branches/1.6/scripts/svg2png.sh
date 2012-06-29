#!/bin/sh

for i in *; do inkscape $i -w=64 -h=64 --export-png=`echo $i | sed -e 's/svg$/png/'`; done
