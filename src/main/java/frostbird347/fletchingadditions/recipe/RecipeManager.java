package frostbird347.fletchingadditions.recipe;

import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class RecipeManager {

	public static void registerRecipieTypes() {
		Registry.register(Registry.RECIPE_SERIALIZER, FletchingRecipeSerializer.ID, FletchingRecipeSerializer.INSTANCE);
		Registry.register(Registry.RECIPE_TYPE, new Identifier("fletching-additions", FletchingRecipe.Type.ID), FletchingRecipe.Type.INSTANCE);
	}
}