package frostbird347.fletchingadditions.screen;

import frostbird347.fletchingadditions.screenHandler.ScreenHandlerManager;
import net.minecraft.client.gui.screen.ingame.HandledScreens;

public class ScreenManager {

	public static void registerScreens() {
		HandledScreens.register(ScreenHandlerManager.FLETCHING_TABLE, FletchingTableScreen::new);
	}
}