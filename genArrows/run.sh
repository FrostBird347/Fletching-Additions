#!/bin/bash
wget -i ./DownloadURL.txt -O ./Ingredients.csv
node ./genArrows.js
rm ./Ingredients.csv
