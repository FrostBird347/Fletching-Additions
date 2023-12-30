#!/bin/bash
cd "$(dirname "$0")"
#There are too many files for rm to process at once, so we use find
find ../src/main/resources/data/fletching-additions/recipes/ -maxdepth 1 -name 'zzzzzzzzzz_autogen_*.json' -delete
echo "Downloading..." | tee out.log
wget -i ./DownloadURL.txt -O ./Ingredients.csv
node ./genArrows.js 2>&1 | tee -a out.log

# for come reason adding whitespace and running it a second time gives more compression improvements (the third one is just to be extra sure the output is always the same)
/Applications/ImageOptim.app/Contents/MacOS/ImageOptim ./*.svg >/dev/null 2>&1
for svg in ./*.svg
do
	echo " " >> "$svg"
done
/Applications/ImageOptim.app/Contents/MacOS/ImageOptim ./*.svg >/dev/null 2>&1
for svg in ./*.svg
do
	echo " " >> "$svg"
done
/Applications/ImageOptim.app/Contents/MacOS/ImageOptim ./*.svg >/dev/null 2>&1