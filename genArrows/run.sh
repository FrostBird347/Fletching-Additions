#!/bin/bash
cd "$(dirname "$0")"
#There are too many files for rm to process at once, so we use find
find ../src/main/resources/data/fletching-additions/recipes/ -maxdepth 1 -name 'zzzzzzzzzz_autogen_*.json' -delete
echo "Downloading..." | tee out.log
wget -i ./DownloadURL.txt -O ./Ingredients.csv
node ./genArrows.js 2>&1 | tee -a out.log
