package frostbird347.fletchingadditions.modCompat;

import net.fabricmc.loader.api.FabricLoader;

public class ModCompatManager {
	public static BaseCompat ON_SOUL_FIRE = new BaseCompat();
	//Runnable osfCompatBuilder = ()-> new BaseCompat();

	public static void setupModCompat() {

		if (FabricLoader.getInstance().isModLoaded("onsoulfire")) {
   			ON_SOUL_FIRE = new OnSoulFireCompat();
		}
	}
}