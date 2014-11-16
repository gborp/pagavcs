#!/bin/bash

function sedeasy {
  sed -i "s/$(echo $1 | sed -e 's/\([[\/.*]\|\]\)/\\&/g')/$(echo $2 | sed -e 's/[\/&]/\\&/g')/g" $3
}

# convert sourcedir outputdir width height
function convertSvg2Png {
	for i in $1/*.svg; do
		filename=$(basename "$i")
		pngfile=$2/`echo $filename | sed -e "s/\.svg$/\.png/"`

		#sedeasy "<path d=" "<path fill=\"#000000\" d=" $i

		inkscape -z "$i" --export-png="$pngfile" -h $3 -w $4
	done
}


convertSvg2Png ../icons/hicolor/scalable/emblems src/hu/pagavcs/client/resources/emblems 128 128

../icons-inline/main/makeicons.sh
convertSvg2Png ../icons-inline/main src/hu/pagavcs/client/resources 128 128
