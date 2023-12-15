const nbt = require("nbt-ts");
const csv = require("csv-parse/sync");
const fs = require('fs');
const execFileSync = require('child_process').execFileSync;
const sha1 = require('sha1');
const dyes = ["White", "Light gray", "Gray", "Black", "Brown", "Red", "Orange", "Yellow", "Lime", "Green", "Cyan", "Light blue", "Blue", "Purple", "Magenta", "Pink"];

//----------
//Functions

function exists(value) {
	return (value != undefined && value != "");
}

function notDefault(value) {
	return exists(value) && value != "_";
}

function getOrDefault(item, key, defValue) {
	if (notDefault(item[key])) {
		return item[key];
	}
	return defValue;
}

function getAddOrEmpty(item, key, defValue, prependValue, appendValue) {
	let output = getOrDefault(item, key, defValue);
	if (output != defValue) {
		return prependValue + output + appendValue;
	}
	return "";
}

function parseItem(rawItem) {
	let item = {valid: false};
	
	if (!exists(rawItem[0])) { console.log("\titem missing:\t", rawItem); return item; };
	item.id = rawItem[0];
	if (!exists(rawItem[1])) { console.log("\tfullName missing:\t", rawItem[0]); return item; };
	item.fullName = rawItem[1];
	if (!exists(rawItem[2])) { console.log("\tpartName missing:\t", rawItem[0]); return item; };
	item.partName = rawItem[2];
	if (!exists(rawItem[3])) { console.log("\tunknown type:\t", rawItem[0]); return item; };
	
	if (!exists(rawItem[4])) { console.log("\tcategories missing:\t", rawItem[0]); return item; };
	let categories = rawItem[4].split(" ");
	//Split categories into 2 arrays for quick and easy blacklist checking
	item.cat = [];
	item.catBlacklist = [];
	item.catRequirements = [];
	for (let i = 0; i < categories.length; i++) {
		if (categories[i].startsWith("no")) {
			item.catBlacklist.push(categories[i].replace("no", ""));
		} else if (categories[i].startsWith("req")) {
			item.catRequirements.push(categories[i].replace("req", ""));
		} else {
			item.cat.push(categories[i]);
		}
	}
	
	if (!exists(rawItem[6])) { console.log("\trenderMode missing:\t", rawItem[0]); return item; };
	item.modelType = rawItem[6].split("&")[0];
	item.modelTexture = rawItem[6].split("&")[1];
	
	if (!exists(rawItem[5])) { console.log("\tstats missing:   \t", rawItem[0]); return item; };
	//flags that will be processed by the game and not this script
	item.gameFlags = [];
	//flags that will be processed by this script
	item.genFlags = [];
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
							item.blockHitActions.push({action: currentStat[iA], sound: currentStat[iA + 1]});
							iA++;
							break;
						case "drop":
							item.blockHitActions.push({action: currentStat[iA], item: currentStat[iA + 1], count: new nbt.Int(currentStat[iA + 2]), sound: currentStat[iA + 3]});
							iA += 3;
							break;
						default:
							console.log("\tunknown blockHitAction:\t", currentStat[iA]);
							console.log("\tcan't process other actions!");
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
			case "replaceTextures":
			case "partialNameIsFull":
				item.genFlags.push(currentStat[0]);
				break;
			case "breaksWhenWet":
			case "inheritFireworkStarNBT":
			case "inheritFireworkNBT":
			case "silent":
			case "waterSpeed":
			case "breaksWhenWet":
			case "dynamicLightingIfPossible":
			case "impactSoundIncreasePitchNoAngle":
			case "farSound":
				item.gameFlags.push(currentStat[0]);
				break;
			//Also put this out as a gen flag
			case "outputCountMult":
				item.genFlags.push(currentStat[0]);
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
				console.log("\tunknown stat:    \t", currentStat[0]);
				//execFileSync("/bin/sleep", ["2"]);
		}
	}
	
	item.valid = true;
	//console.log(item);
	return item;
}

function checkCompatMatch(itemA, itemB, term = "catBlacklist") {
	for (let i = 0; i < itemA.cat.length; i++) {
		if (itemB[term].includes(itemA.cat[i])) return false;
	}
	for (let i = 0; i < itemB.cat.length; i++) {
		if (itemA[term].includes(itemB.cat[i])) return false;
	}
	return true;
}

function checkCompat(tip, stick, fin, effect) {
	//Check blacklist
	let isCompat = checkCompatMatch(tip, stick);
	isCompat &&= checkCompatMatch(fin, stick);
	isCompat &&= checkCompatMatch(effect, stick);
	isCompat &&= checkCompatMatch(tip, effect);
	isCompat &&= checkCompatMatch(fin, effect);
	
	//Check requirements
	let reqCount = tip.catRequirements.length + stick.catRequirements.length + fin.catRequirements.length + effect.catRequirements.length;
	let reqCheck = (reqCount == 0);
	reqCheck ||=  !checkCompatMatch(tip, stick, "catRequirements");
	reqCheck ||= !checkCompatMatch(tip, fin, "catRequirements");
	reqCheck ||= !checkCompatMatch(tip, effect, "catRequirements");
	reqCheck ||= !checkCompatMatch(stick, fin, "catRequirements");
	reqCheck ||= !checkCompatMatch(stick, effect, "catRequirements");
	reqCheck ||= !checkCompatMatch(fin, effect, "catRequirements");
	
	//if (reqCount != 0 && reqCheck) {
	//	debugger;
	//	execFileSync("/bin/sleep", ["2"]);
	//}
	
	return isCompat && reqCheck;
}

function genOutput(inputs) {
	let modelOverride = undefined;
	let overiddenPartialName = undefined;
	let tipID = 0, stickID = 1, finID = 2, effectID = 3;
	let outputJSON = {type: "fletching-additions:fletching_recipe", outputItem: "fletching-additions:custom_arrow", outputAmount: 6};
	let outputNBT = {display:{}};
	let fakeLength = inputs.length;
	if (!notDefault(inputs[effectID].id)) fakeLength--;
	
	outputJSON.inputTip = {item: inputs[tipID].id};
	outputJSON.inputStick = {item: inputs[stickID].id};
	outputJSON.inputFins = {item: inputs[finID].id};
	if (inputs[effectID].id != "_") {
		outputJSON.inputEffect = {item: inputs[effectID].id};
	}
	
	//Simple multiplier values
	let globalMultKeys = ["flySpeed", "gravityMult", "drawSpeed", "damageMult"];
	for (let i = 0; i < globalMultKeys.length; i++) {
		outputNBT[globalMultKeys[i]] = new nbt.Float(parseFloat(getOrDefault(inputs[tipID], globalMultKeys[i], 1) * getOrDefault(inputs[stickID], globalMultKeys[i], 1) * getOrDefault(inputs[finID], globalMultKeys[i], 1) * getOrDefault(inputs[effectID], globalMultKeys[i], 1)));
	}
	
	outputJSON.outputAmount = Math.round(outputJSON.outputAmount * getOrDefault(inputs[tipID], "outputCountMult", 1) * getOrDefault(inputs[stickID], "outputCountMult", 1) * getOrDefault(inputs[finID], "outputCountMult", 1) * getOrDefault(inputs[effectID], "outputCountMult", 1));
	
	outputNBT.gameFlags = [];
	
	for (let i = 0; i < fakeLength; i++) {
		
		//Combine all game flags
		outputNBT.gameFlags.push(...inputs[i].gameFlags);
		
		for (let iS = 0; iS < inputs[i].statsPresent.length; iS++) {
			//Skip over game flags, since those have already been sorted out and will be processed by the game
			if (!inputs[i].gameFlags.includes(inputs[i].statsPresent[iS])) {
				switch (inputs[i].statsPresent[iS]) {
					case "blockHitActions":
						if (outputNBT.blockHitActions == undefined) {
							outputNBT.blockHitActions = [];
							outputNBT.blockHitActionChances = [];
						}
						outputNBT.blockHitActions.push(inputs[i].blockHitActions);
						outputNBT.blockHitActionChances.push(inputs[i].blockHitActionChances);
						break;
					case "applyEffect":
						if (outputNBT.effects == undefined) outputNBT.effects = [];
						outputNBT.effects.push(...inputs[i].effects);
						break;
					case "fireAspect":
						if (outputNBT.fireChance == undefined) outputNBT.fireChance = [];
						outputNBT.fireChance.push(...inputs[i].fireChance);
						break;
					case "replaceTextures":
						modelOverride = {modelType: inputs[i].modelType, modelTexture: inputs[i].modelTexture};
						break;
					case "partialNameIsFull":
						overiddenPartialName = inputs[i].partName;
						break;
					case "_":
					case "skipThisComment":
					//Already processed stats
					case "flySpeed":
					case "gravityMult":
					case "drawSpeed":
					case "damageMult":
					case "outputCountMult":
						break;
					default:
						console.log("\tunknown stat:    \t", inputs[i].statsPresent[iS]);
						//execFileSync("/bin/sleep", ["2"]);
				}
			}
		}
	}
	
	
	let outputName = `${getAddOrEmpty(inputs[tipID], "partName", "_", "", " ")}${getAddOrEmpty(inputs[finID], "partName", "_", "", " ")}${getAddOrEmpty(inputs[effectID], "partName", "_", "", " ")}Arrow${getAddOrEmpty(inputs[stickID], "partName", "_", " with a ", " Core")}`
	if (inputs[tipID].partName == "_" && inputs[stickID].partName == "_" && inputs[finID].partName == "_") {
		outputName = inputs[effectID].fullName;
	} else if (inputs[tipID].partName == "_" && inputs[stickID].partName == "_" && inputs[effectID].partName == "_") {
		outputName = inputs[finID].fullName;
	}  else if (inputs[tipID].partName == "_" && inputs[finID].partName == "_" && inputs[effectID].partName == "_") {
		outputName = inputs[stickID].fullName;
	}  else if (inputs[effectID].partName == "_" && inputs[stickID].partName == "_" && inputs[finID].partName == "_") {
		outputName = inputs[tipID].fullName;
	} else if (overiddenPartialName != undefined) {
		outputName = overiddenPartialName;
	}
	outputNBT.display.Name = nbt.stringify({text:outputName, italic: false});
	
	//console.log(outputName);
	//console.log(inputs);
	//console.log(outputNBT);
	outputJSON.outputNbt = nbt.stringify(outputNBT);
	//console.log(outputJSON);
	
	let outputFilePath = `../src/main/resources/data/fletching-additions/recipes/zzzzzzzzzz_autogen_${inputs[tipID].id.split(":")[1]}_${inputs[stickID].id.split(":")[1]}_${inputs[finID].id.split(":")[1]}`;
	if (inputs[effectID].id != "_") {
		outputFilePath += `_${inputs[effectID].id.split(":")[1]}`;
	}
	outputFilePath += `_${sha1(JSON.stringify(outputJSON))}.json`;
	
	fs.writeFileSync(outputFilePath, JSON.stringify(outputJSON));
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
			
			//Make sure to remove spaces for the item id as well as the texture/model id
			splitItems[iD][0] = splitItems[iD][0].split(" ").join("_").toLowerCase();
			splitItems[iD][6] = splitItems[iD][6].split(" ").join("_").toLowerCase();
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
					console.log("\tunknown type:\t", splitItems[iL][3]);
			}
		}
	}
}

console.log("Combining...");
let totalCount = 0;
for (let iT = 0; iT < tips.length; iT++) {
	for (let iS = 0; iS < sticks.length; iS++) {
		for (let iF = 0; iF < fins.length; iF++) {
			for (let iE = 0; iE < effects.length; iE++) {
				//Make sure parts are compatible
				if (checkCompat(tips[iT], sticks[iS], fins[iF], effects[iE])) {
					//Make sure we don't recreate vanilla arrows
					if (!(tips[iT].id == "minecraft:flint" && sticks[iS].id == "minecraft:stick" && fins[iF].id == "minecraft:feather" && (effects[iE].id == "minecraft:glowstone_dust" || effects[iE].id == "_"))) {
						genOutput([tips[iT], sticks[iS], fins[iF], effects[iE]]);
						totalCount++
					}
				}
			}
		}
	}
}
console.log("Total: " + totalCount);