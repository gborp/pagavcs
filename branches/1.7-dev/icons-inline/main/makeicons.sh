#/bin/bash

function sedeasy {
  sed -i "s/$(echo $1 | sed -e 's/\([[\/.*]\|\]\)/\\&/g')/$(echo $2 | sed -e 's/[\/&]/\\&/g')/g" $3
}

# convert sourcefile destfile sourcecolor destcolor
function recolor {
	cp "$1" "$2"
	sedeasy "$3" "$4" "$2"
}

recolor icon.svg commit-app-icon.svg "#0057cd" "#cd1d00"
recolor icon.svg other-app-icon.svg "#0057cd" "#cdc600"
recolor icon.svg showlog-app-icon.svg "#0057cd" "#0057cd"
recolor icon.svg switch-app-icon.svg "#0057cd" "#9acd00"
recolor icon.svg update-app-icon.svg "#0057cd" "#22cd00"
