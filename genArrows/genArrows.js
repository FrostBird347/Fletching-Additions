const nbt = require("nbt-ts");
const csv = require("csv-parse/sync");
const fs = require('fs');
const execFileSync = require('child_process').execFileSync;
const dyes = ["White", "Light gray", "Gray", "Black", "Brown", "Red", "Orange", "Yellow", "Lime", "Green", "Cyan", "Light blue", "Blue", "Purple", "Magenta", "Pink"];

//----------
//Functions

function exists(value) {
	return (value != undefined && value != "");
}

function parseItem(rawItem) {
	let item = {valid: false};
	
	if (!exists(rawItem[0])) { console.error("\titem missing: ", rawItem); return item; };
	item.id = rawItem[0];
	if (!exists(rawItem[1])) { console.error("\tfullName missing: ", rawItem); return item; };
	item.fullName = rawItem[1];
	if (!exists(rawItem[2])) { console.error("\tpartName missing: ", rawItem); return item; };
	item.partName = rawItem[2];
	if (!exists(rawItem[3])) { console.error("\tUnknown type: ", rawItem); return item; };
	
	if (!exists(rawItem[4])) { console.error("\tcategories missing: ", rawItem); return item; };
	let categories = rawItem[4].split(" ");
	//Split categories into 2 arrays for quick and easy blacklist checking
	item.cat = [];
	item.catBlacklist = [];
	for (let i = 0; i < categories.length; i++) {
		if (categories[i].startsWith("no")) {
			item.catBlacklist.push(categories[i].replace("no", ""));
		} else {
			item.cat.push(categories[i]);
		}
	}
	
	if (!exists(rawItem[6])) { console.error("\trenderMode missing: ", rawItem); return item; };
	item.modelType = rawItem[6].split("&")[0];
	item.modelTexture = rawItem[6].split("&")[1];
	
	if (!exists(rawItem[5])) { console.error("\tstats missing: ", rawItem); return item; };
	//flags that will be processed by the game and not this script
	item.gameFlags = [];
	//array of stats present, so we can just check via item.statsPresent.includes()
	item.statsPresent = [];
	//array of already formatted effects, we will just need to combine the arrays of each ingredient later on
	item.effects = [];
	//array of fire chances, the game will run this check for each value in the array
	//as with effects, it's already formatted for nbt usage
	item.fireChance = [];
	//all numbers provided should add up to 1
	item.blockHitActionChances = [];
	item.blockHitActions = [];
	
	let stats = rawItem[5].split("@");
	for (let i = 0; i < stats.length; i++) {
		let currentStat = stats[i].split("&");
		item.statsPresent.push(currentStat[0]);
		switch (currentStat[0]) {
			case "blockHitActions":
				let iA = 1;
				let totalChance = 0;
				
				while (iA < currentStat.length) {
					totalChance += parseFloat(currentStat[iA]);
					item.blockHitActionChances.push(new nbt.Float(totalChance));
					iA++;
					
					switch (currentStat[iA]) {
						case "doNothing":
						case "destroy":
							item.blockHitActions.push({action: currentStat[iA]});
							break;
						case "drop":
							item.blockHitActions.push({action: currentStat[iA], item: currentStat[iA + 1]});
							iA++;
							break;
						default:
							console.error("\tUnknown blockHitAction: ", currentStat[iA]);
							console.error("\tCan't process other actions!");
							iA = currentStat.length;
					}
					iA++;
				}
				break;
			case "fireAspect":
				item.fireChance.push(new nbt.Float(currentStat[1]));
				break;
			case "applyEffect":
				item.effects.push({ id: currentStat[1], duration: new nbt.Int(currentStat[2]) });
				break;
			case "breaksWhenWet":
			case "inheritFireworkStarNBT":
			case "silent":
			case "waterSpeed":
			case "breaksWhenWet":
			case "dynamicLightingIfPossible":
				item.gameFlags.push(currentStat[0]);
				break;
			case "flySpeed":
			case "gravityMult":
			case "drawSpeed":
			case "damageMult":
				if (item[currentStat[0]] == undefined) {
					item[currentStat[0]] = currentStat[1];
				} else {
					item[currentStat[0]] *= currentStat[1];
				}
				break;
			case "_":
			case "skipThisComment":
				break;
			default:
				console.error("\tUnknown stat: ", currentStat[0]);
				//execFileSync("/bin/sleep", ["2"]);
		}
	}
	
	item.valid = true;
	//console.log(item);
	return item;
}


//----------
//Start

console.log("Reading csv...");
let fullDataset = csv.parse(fs.readFileSync('./Ingredients.csv'));
let tips = [];
let sticks = [];
let fins = [];
let effects = [];

console.log("Parsing...");
for (let i = 1; i < fullDataset.length; i++) {
	let splitItems = [fullDataset[i]];
	
	if (fullDataset[i][0].includes("[DYE]")) {
		splitItems = [];
		
		for (let iD = 0; iD < dyes.length; iD++) {
			//Push a copy, not a reference
			splitItems.push(JSON.parse(JSON.stringify(fullDataset[i])));
			
			for (let iC = 0; iC < fullDataset[i].length; iC++) {
				splitItems[iD][iC] = splitItems[iD][iC].replaceAll("[DYE]", dyes[iD]);
			}
			splitItems[iD][0] = splitItems[iD][0].split(" ").join("_").toLowerCase();
		}
	}
	
	for (let iL = 0; iL < splitItems.length; iL++) {
		let item = parseItem(splitItems[iL]);
		if (item.valid) {
			switch(splitItems[iL][3]) {
				case 't':
					tips.push(item);
					break;
				case 's':
					sticks.push(item);
					break;
				case 'f':
					fins.push(item);
					break;
				case 'e':
					effects.push(item);
					break;
				default:
					console.error("\tUnknown type: ", splitItems[iL][3]);
			}
		}
	}
}

for (let iT = 0; iT < tips.length; iT++) {
	for (let iS = 0; iS < sticks.length; iS++) {
		for (let iF = 0; iF < fins.length; iF++) {
			for (let iE = 0; iE < effects.length; iE++) {
				console.log(tips[iT], sticks[iS], fins[iF], effects[iE]);
			}
		}
	}
}