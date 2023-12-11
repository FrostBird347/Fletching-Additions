package frostbird347.fletchingadditions.item;

import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class ItemManager {
	public static final CustomArrowItem CUSTOM_ARROW = new CustomArrowItem(new Item.Settings().group(ItemGroup.COMBAT));

	public static void registerItems() {
		Registry.register(Registry.ITEM, new Identifier("fletching-additions", "custom_arrow"), (Item)CUSTOM_ARROW);
	}
}