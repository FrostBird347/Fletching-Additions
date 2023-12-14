#!/bin/bash
cd "$(dirname "$0")"
echo "Downloading..." | tee out.log
wget -i ./DownloadURL.txt -O ./Ingredients.csv
node ./genArrows.js 2>&1 | tee -a out.log
