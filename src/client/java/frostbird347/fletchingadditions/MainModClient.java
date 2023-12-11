package frostbird347.fletchingadditions;

import frostbird347.fletchingadditions.entityRenderer.EntityRendererManager;
import frostbird347.fletchingadditions.screen.ScreenManager;
import net.fabricmc.api.ClientModInitializer;

public class MainModClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ScreenManager.registerScreens();
		EntityRendererManager.registerEntityRenderers();
	}
}