package frostbird347.fletchingadditions.modCompat;

import frostbird347.fletchingadditions.MainMod;
import moriyashiine.onsoulfire.common.component.entity.OnSoulFireComponent;
import moriyashiine.onsoulfire.common.registry.ModEntityComponents;
import net.minecraft.entity.Entity;

public class OnSoulFireCompat extends BaseCompat {
	public OnSoulFireCompat() {

	}

	@Override
	public void executeAction(String actionName, Object[] args) {
		switch (actionName) {
			case "applySoulFlame":
				try {
					Entity target = (Entity)args[0];
					
					OnSoulFireComponent targetSoulFire = ModEntityComponents.ON_SOUL_FIRE.get(target);
					if (!targetSoulFire.isOnSoulFire()) {
						targetSoulFire.setOnSoulFire(true);
						targetSoulFire.sync();
					}
				} catch (Exception err) {
					MainMod.LOGGER.error("An exception occured while attempting to apply soul flame!");
					MainMod.LOGGER.error(err.toString());
				}
				break;
			default:
				MainMod.LOGGER.warn("OnSoulFireCompat was given an unknown action: " + actionName);
				break;
		}
	}
	
}