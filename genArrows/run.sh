#!/bin/bash
cd "$(dirname "$0")"
rm ../src/main/resources/data/fletching-additions/recipes/z_autogen_*.json
echo "Downloading..." | tee out.log
wget -i ./DownloadURL.txt -O ./Ingredients.csv
node ./genArrows.js 2>&1 | tee -a out.log
