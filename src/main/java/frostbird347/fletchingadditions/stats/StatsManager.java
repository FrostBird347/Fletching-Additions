package frostbird347.fletchingadditions.stats;

import net.minecraft.stat.StatFormatter;
import net.minecraft.stat.Stats;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class StatsManager {
	public static final Identifier INTERACT_WITH_FLETCHING_TABLE = new Identifier("fletching-additions", "interact_with_fletching_table");

	public static void registerStats() {
		Registry.register(Registry.CUSTOM_STAT, "interact_with_fletching_table", INTERACT_WITH_FLETCHING_TABLE);
		Stats.CUSTOM.getOrCreateStat(INTERACT_WITH_FLETCHING_TABLE, StatFormatter.DEFAULT);
	}
}