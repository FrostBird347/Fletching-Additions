package frostbird347.fletchingadditions;

import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import frostbird347.fletchingadditions.recipe.RecipeManager;
import frostbird347.fletchingadditions.screenHandler.ScreenHandlerManager;

public class MainMod implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("fletching-additions");

	@Override
	public void onInitialize() {
		RecipeManager.registerRecipieTypes();
		ScreenHandlerManager.registerScreenHandlers();
	}
}