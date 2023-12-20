package frostbird347.fletchingadditions;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import frostbird347.fletchingadditions.entity.EntityManager;
import frostbird347.fletchingadditions.item.ItemManager;
import frostbird347.fletchingadditions.modCompat.ModCompatManager;
import frostbird347.fletchingadditions.particle.ParticleManager;
import frostbird347.fletchingadditions.recipe.RecipeManager;
import frostbird347.fletchingadditions.screenHandler.ScreenHandlerManager;
import frostbird347.fletchingadditions.stats.StatsManager;

public class MainMod implements ModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("fletching-additions");

	@Override
	public void onInitialize() {
		ModCompatManager.setupModCompat();
		
		StatsManager.registerStats();
		RecipeManager.registerRecipieTypes();
		ScreenHandlerManager.registerScreenHandlers();
		ItemManager.registerItems();
		EntityManager.registerEntities();
		ParticleManager.registerParticles();
	}
}