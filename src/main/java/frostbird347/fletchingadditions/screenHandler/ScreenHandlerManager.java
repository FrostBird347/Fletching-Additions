package frostbird347.fletchingadditions.screenHandler;

import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class ScreenHandlerManager {
	public static final ScreenHandlerType<FletchingTableScreenHandler> FLETCHING_TABLE = new ScreenHandlerType<frostbird347.fletchingadditions.screenHandler.FletchingTableScreenHandler>(FletchingTableScreenHandler::new);

	public static void registerScreenHandlers() {
		Registry.register(Registry.SCREEN_HANDLER, new Identifier("fletching-additions", "fletching_table_screenhandler"), FLETCHING_TABLE);
	}
}