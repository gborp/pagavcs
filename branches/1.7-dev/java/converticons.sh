#!/bin/bash

# convert source-dir width(=postfix) height effect-strength(0-off, 100-max)
function convertSvg2Png {
	for i in $1/*.svg; do
		filename=$(basename "$i")
		pngfile=$2/`echo $filename | sed -e "s/\.svg$/\.png/"`
		inkscape -z $i --export-png=$pngfile -h $3 -w $4
	done
}


convertSvg2Png ../icons/hicolor/scalable/emblems src/hu/pagavcs/client/resources/emblems 128 128
convertSvg2Png ../icons-inline/main src/hu/pagavcs/client/resources 128 128
