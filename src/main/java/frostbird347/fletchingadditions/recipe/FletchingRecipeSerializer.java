package frostbird347.fletchingadditions.recipe;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class FletchingRecipeSerializer implements RecipeSerializer<FletchingRecipe> {

	//Store instance and make sure it can be initialized
	public static final FletchingRecipeSerializer INSTANCE = new FletchingRecipeSerializer();
	private FletchingRecipeSerializer() {}

	public static final Identifier ID = new Identifier("fletching-additions:fletching_recipe");

	//Turns json into Recipe
	@Override
	public FletchingRecipe read(Identifier id, JsonObject json) {
		FletchingRecipeJsonFormat recipeJson = new Gson().fromJson(json, FletchingRecipeJsonFormat.class);

		//Check for all of these individually so more detailed error messages can be displayed
		if (recipeJson.inputTip == null) throw new JsonSyntaxException("inputTip paramater is corrupt or missing!");
		if (recipeJson.inputStick == null) throw new JsonSyntaxException("inputStick paramater is corrupt or missing!");
		if (recipeJson.inputFins == null) throw new JsonSyntaxException("inputFins paramater is corrupt or missing!");
		
		if (recipeJson.outputItem == null) throw new JsonSyntaxException("outputItem paramater is corrupt or missing!");
		//If no output amount value is set, set it to one
		if (recipeJson.outputAmount == 0) {
			recipeJson.outputAmount = 1;
		}

		//get NBT
		NbtCompound outputNbt = null;
		if (recipeJson.outputNbt != null) {
			outputNbt = new NbtCompound();
			try {
				outputNbt = StringNbtReader.parse(recipeJson.outputNbt);
			} catch(CommandSyntaxException e) {
				throw new JsonSyntaxException("outputNbt paramater is corrupt: " + e.toString());
			}
		}

		Ingredient inputTip = Ingredient.fromJson(recipeJson.inputTip);
		Ingredient inputStick = Ingredient.fromJson(recipeJson.inputStick);
		Ingredient inputFins = Ingredient.fromJson(recipeJson.inputFins);
		Ingredient inputEffect = Ingredient.EMPTY;
		if (recipeJson.inputEffect != null) {
			inputEffect = Ingredient.fromJson(recipeJson.inputEffect);
		};
		
		Item outputItem = Registry.ITEM.getOrEmpty(new Identifier(recipeJson.outputItem)).orElseThrow(() -> new JsonSyntaxException("No such item " + recipeJson.outputItem + " for the output!"));;
		ItemStack output = new ItemStack(outputItem, recipeJson.outputAmount);
		output.setNbt(outputNbt);
		return new FletchingRecipe(id, output, inputTip, inputStick, inputFins, inputEffect);
	}
	
	//Send recipe to client?
	@Override
	public void write(PacketByteBuf packetData, FletchingRecipe recipe) {
		recipe.getInputTip().write(packetData);
		recipe.getInputStick().write(packetData);
		recipe.getInputFins().write(packetData);
		recipe.getInputEffect().write(packetData);
		packetData.writeItemStack(recipe.getOutput());
	}

	//Read recipe from server?
	@Override
	public FletchingRecipe read(Identifier id, PacketByteBuf packetData) {
		Ingredient inputTip = Ingredient.fromPacket(packetData);
		Ingredient inputStick = Ingredient.fromPacket(packetData);
		Ingredient inputFins = Ingredient.fromPacket(packetData);
		Ingredient inputEffect = Ingredient.fromPacket(packetData);
		ItemStack output = packetData.readItemStack();
		return new FletchingRecipe(id, output, inputTip, inputStick, inputFins, inputEffect);
	}

}