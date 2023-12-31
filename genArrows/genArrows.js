const nbt = require("nbt-ts");
const csv = require("csv-parse/sync");
const fs = require('fs');
const execFileSync = require('child_process').execFileSync;
const JSDOM = require("jsdom").JSDOM;
let Plot;
const dyes = ["White", "Light gray", "Gray", "Black", "Brown", "Red", "Orange", "Yellow", "Lime", "Green", "Cyan", "Light blue", "Blue", "Purple", "Magenta", "Pink"];

//Stats
let stats = {
	totalCount: 0,
	mostFireChances: 0,
	longestName: "",
	flySpeedStats: [100000000000000000, 0],
	gravityMultStats: [100000000000000000, 0],
	drawSpeedStats: [100000000000000000, 0],
	drawMinMultStats: [100000000000000000, 0],
	damageMultStats: [100000000000000000, 0],
	itemOutputStats: [100000000000000000, 0]
};
let graphStats = {
	fireChanceDist: [],
	detailedFireChanceDist: [],
	nameLengthDist: [],
	flySpeedDist: [],
	gravityMultDist: [],
	drawSpeedDist: [],
	drawMinMultDist: [],
	damageMultDist: [],
	itemOutputDist: []
}
let fireStackCounters = [];

//----------
//Functions

async function loadPlot() {
	console.log("Loading plot...");
	Plot = await import("@observablehq/plot");
	realStart();
}

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
	//array of particles to spawn in as the arrow is in the air
	item.particles = [];
	//all numbers provided should add up to 1
	item.blockHitActionChances = [];
	item.blockHitActions = [];
	
	let stats = rawItem[5].split("@");
	for (let i = 0; i < stats.length; i++) {
		let currentStat = stats[i].split("&");
		//Prevent stats from potentially being processed multiple times later
		if (!item.statsPresent.includes(currentStat[0])) {
			item.statsPresent.push(currentStat[0]);
		}
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
			case "particle":
				item.particles.push(currentStat[1]);
				break;
			case "fireChance":
				item.fireChance.push(new nbt.Float(currentStat[1]));
				break;
			case "applyEffect":
				item.effects.push({ id: currentStat[1], duration: new nbt.Int(currentStat[2]) });
				break;
			case "replaceTextures":
			case "partialNameIsFull":
				item.genFlags.push(currentStat[0]);
				break;
			case "inheritFireworkStarNBT":
			case "inheritFireworkNBT":
			case "silent":
			case "waterSpeed":
			case "breaksWhenWet":
			case "dynamicLightingIfPossible":
			case "impactSoundIncreasePitchNoAngle":
			case "farSound":
			case "isSoulFire":
			case "echoLink":
			case "isSensor":
			case "arrowDrift":
				item.gameFlags.push(currentStat[0]);
				break;
			//Also put this out as a gen flag
			case "outputCountMult":
				item.genFlags.push(currentStat[0]);
			case "flySpeed":
			case "gravityMult":
			case "drawSpeed":
			case "drawMinMult":
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
	let globalMultKeys = ["flySpeed", "gravityMult", "drawSpeed", "drawMinMult", "damageMult"];
	for (let i = 0; i < globalMultKeys.length; i++) {
		let tempValue = new nbt.Float(parseFloat(getOrDefault(inputs[tipID], globalMultKeys[i], 1) * getOrDefault(inputs[stickID], globalMultKeys[i], 1) * getOrDefault(inputs[finID], globalMultKeys[i], 1) * getOrDefault(inputs[effectID], globalMultKeys[i], 1)));
		if (tempValue != 1) {
			outputNBT[globalMultKeys[i]] = tempValue;
			
			//Stats (ignore items with zero damage, as those aren't interesting)
			if (tempValue < stats[globalMultKeys[i] + "Stats"][0] && !(globalMultKeys[i] == "damageMult" && tempValue == 0)) stats[globalMultKeys[i] + "Stats"][0] = tempValue;
			if (tempValue > stats[globalMultKeys[i] + "Stats"][1]) stats[globalMultKeys[i] + "Stats"][1] = tempValue;
		}
		graphStats[globalMultKeys[i] + "Dist"].push(tempValue);
	}
	
	outputJSON.outputAmount = Math.max(Math.round(outputJSON.outputAmount * getOrDefault(inputs[tipID], "outputCountMult", 1) * getOrDefault(inputs[stickID], "outputCountMult", 1) * getOrDefault(inputs[finID], "outputCountMult", 1) * getOrDefault(inputs[effectID], "outputCountMult", 1)), 1);
	
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
					case "particle":
						if (outputNBT.particles == undefined) outputNBT.particles = [];
						outputNBT.particles.push(...inputs[i].particles);
						break;
					case "applyEffect":
						if (outputNBT.effects == undefined) outputNBT.effects = [];
						outputNBT.effects.push(...inputs[i].effects);
						break;
					case "fireChance":
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
					case "drawMinMult":
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
	outputNBT.display.Name = JSON.stringify([{text:outputName, italic: false}]);
	
	/*outputNBT.display.Lore = ['[{"text":"","italic":false}]'];
	for (let iI = 0; iI < inputs.length; iI++) {
		let loreColour = "";
		let currentName = "";
		for (let iC = 0; iC < inputs[iI].cat.length; iC++) {
			loreColour = "dark_aqua";
			currentName = inputs[iI].cat[iC];
			if (currentName != "_") {
				//Newlines seem to break the lore nbt for some reason
				outputNBT.display.Lore.push(JSON.stringify([{text: "◆" + currentName, italic: false, "color": loreColour}]).replaceAll("\n", ""));
			}
		}
		for (let iCB = 0; iCB < inputs[iI].catBlacklist.length; iCB++) {
			loreColour = "dark_red";
			currentName = inputs[iI].catBlacklist[iCB];
			if (currentName != "_") {
				//Newlines seem to break the lore nbt for some reason
				outputNBT.display.Lore.push(JSON.stringify([{text: "-" + currentName, italic: false, "color": loreColour}]).replaceAll("\n", ""));
			}
		}
		for (let iCR = 0; iCR < inputs[iI].catRequirements.length; iCR++) {
			loreColour = "dark_green";
			currentName = inputs[iI].catRequirements[iCR];
			if (currentName != "_") {
				//Newlines seem to break the lore nbt for some reason
				outputNBT.display.Lore.push(JSON.stringify([{text: "+" + currentName, italic: false, "color": loreColour}]).replaceAll("\n", ""));
			}
		}
	}*/
	
	//console.log(outputName);
	//console.log(inputs);
	//console.log(outputNBT);
	outputJSON.outputNbt = nbt.stringify(outputNBT);
	//console.log(outputJSON);
	
	let outputFilePath = `../src/main/resources/data/fletching-additions/recipes/zzzzzzzzzz_autogen_${inputs[tipID].id.split(":")[1]}_${inputs[stickID].id.split(":")[1]}_${inputs[finID].id.split(":")[1]}`;
	if (inputs[effectID].id != "_") {
		outputFilePath += `_${inputs[effectID].id.split(":")[1]}`;
	}
	//Make sure no duplicates remain, without needing to use sha1
	let appendCounter = 1;
	let appendString = "";
	while (fs.existsSync(outputFilePath + appendString + ".json")) {
		appendCounter++;
		appendString = "_" + appendCounter;
	}
	outputFilePath += appendString + ".json";
	
	fs.writeFileSync(outputFilePath, JSON.stringify(outputJSON));
	
	//More stats
	if (outputNBT.fireChance != undefined && stats.mostFireChances < outputNBT.fireChance.length) stats.mostFireChances = outputNBT.fireChance.length;
	if (stats.longestName.length < outputName.length) stats.longestName = outputName;
	if (outputJSON.outputAmount < stats.itemOutputStats[0]) stats.itemOutputStats[0] = outputJSON.outputAmount;
	if (outputJSON.outputAmount > stats.itemOutputStats[1]) stats.itemOutputStats[1] = outputJSON.outputAmount;
	if (outputNBT.fireChance != undefined) {
		graphStats.fireChanceDist.push(outputNBT.fireChance.length);
		for (let cM = 0; cM < outputNBT.fireChance.length; cM++) {
			let detailedChance = {"Fire Stack": cM, "% Chance": 1};
			for (let cI = 0; cI < outputNBT.fireChance.length; cI++) {
				let tempChance = 1;
				for (let cIB = 0; cIB < outputNBT.fireChance.length; cIB++) {
					if (cI == cIB) {
						tempChance *= outputNBT.fireChance[cIB];
					} else {
						tempChance *= 1 - outputNBT.fireChance[cIB];
					}
				}
				detailedChance["% Chance"] += tempChance;
			}
			detailedChance["% Chance"] -= 1;
			detailedChance["% Chance"] *= 100;
			detailedChance["Fire Stack"] += 1;
			
			if (fireStackCounters[detailedChance["% Chance"].toString() + "|" + detailedChance["Fire Stack"].toString()] == undefined) {
				fireStackCounters[detailedChance["% Chance"].toString() + "|" + detailedChance["Fire Stack"].toString()] = {count: 1};
			} else {
				fireStackCounters[detailedChance["% Chance"].toString() + "|" + detailedChance["Fire Stack"].toString()].count++;
			}
			detailedChance["SyncedCounter"] = fireStackCounters[detailedChance["% Chance"].toString() + "|" + detailedChance["Fire Stack"].toString()];
			
			graphStats.detailedFireChanceDist.push(detailedChance);
		}
	}
	graphStats.nameLengthDist.push(outputName.length);
	graphStats.itemOutputDist.push(outputJSON.outputAmount);
}

function genPlot(dataset, name) {
	let currentPlot = Plot.rectY(dataset, Plot.binX()).plot({x: {label: name}, style: {color: "dodgerblue"}, document: (new JSDOM(`...`)).window.document});
	savePlot(currentPlot, name);
}

function savePlot(plot, name) {
	plot.setAttribute("xmlns", "http://www.w3.org/2000/svg");
	plot.setAttribute("xmlns:xlink", "http://www.w3.org/1999/xlink");
	fs.writeFileSync("./Stats_" + name.replaceAll(" ", "") + ".svg", plot.outerHTML);
}

//----------
//Start

let tips = [];
let sticks = [];
let fins = [];
let effects = [];
loadPlot();
function realStart() {
	console.log("Reading csv...");
	let fullDataset = csv.parse(fs.readFileSync('./Ingredients.csv'));

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
	for (let iT = 0; iT < tips.length; iT++) {
		for (let iS = 0; iS < sticks.length; iS++) {
			for (let iF = 0; iF < fins.length; iF++) {
				for (let iE = 0; iE < effects.length; iE++) {
					//Make sure parts are compatible
					if (checkCompat(tips[iT], sticks[iS], fins[iF], effects[iE])) {
						//Make sure we don't recreate vanilla arrows
						if (!(tips[iT].id == "minecraft:flint" && sticks[iS].id == "minecraft:stick" && fins[iF].id == "minecraft:feather" && (effects[iE].id == "minecraft:glowstone_dust" || effects[iE].id == "_"))) {
							genOutput([tips[iT], sticks[iS], fins[iF], effects[iE]]);
							stats.totalCount++
						}
					}
				}
			}
		}
	}
	console.log("Stats:", stats);
	console.log("Saving graphs...");
	//temporarily disable console.log, because it spams out a bunch of junk info and I don't know how to stop it
	let tempLog = console.log;
	console.log = function() {}

	genPlot(graphStats.nameLengthDist, "Name Length");
	genPlot(graphStats.damageMultDist, "Damage Multiplier");
	genPlot(graphStats.drawSpeedDist, "Draw Speed Multiplier");
	genPlot(graphStats.drawMinMultDist, "Minimum Draw Multiplier");
	genPlot(graphStats.fireChanceDist, "Maximum Fire Stack");
	genPlot(graphStats.flySpeedDist, "Airspeed Multiplier");
	genPlot(graphStats.gravityMultDist, "Gravity Multiplier");
	genPlot(graphStats.itemOutputDist, "Item Output Amount");
	//non-historgram plot
	let detailedFireChancePlot = Plot.plot({
		y: {grid: true, domain: [0, 100]},
		x: {grid: false, type: "band"},
		marks: [
			Plot.dot(graphStats.detailedFireChanceDist, {x: "Fire Stack", y: "% Chance", symbol: "triangle2", stroke: (d) => d.SyncedCounter.count, fill: "dodgerblue"}),
			Plot.text(graphStats.detailedFireChanceDist, {x: "Fire Stack", y: "% Chance", text: (d) => `${d.SyncedCounter.count}`, fill: (d) => d.SyncedCounter.count, dx: 15, lineAnchor: "middle"})
		],
		style: {color: "dodgerblue"},
		color: {scheme: "Cool"},
		document: (new JSDOM(`...`)).window.document,
	})
	savePlot(detailedFireChancePlot, "Fire Chance");
	
	//re-enable console.log
	console.log = tempLog;
}