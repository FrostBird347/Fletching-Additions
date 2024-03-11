package frostbird347.fletchingadditions.item;

import net.minecraft.block.DispenserBlock;
import net.minecraft.block.dispenser.ProjectileDispenserBehavior;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;
import net.minecraft.util.math.Position;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;

public class ItemManager {
	public static final CustomArrowItem CUSTOM_ARROW = new CustomArrowItem(new Item.Settings());
	public static final ModelPlaceholderItem MODEL_PLACEHOLDER = new ModelPlaceholderItem(new Item.Settings().maxCount(1).rarity(Rarity.EPIC));

	public static void registerItems() {
		Registry.register(Registry.ITEM, new Identifier("fletching-additions", "custom_arrow"), (Item)CUSTOM_ARROW);
		Registry.register(Registry.ITEM, new Identifier("fletching-additions", "zzzzzzzzzz_4d4f44454c5f504c414345484f4c444552"), (Item)MODEL_PLACEHOLDER);

		DispenserBlock.registerBehavior(CUSTOM_ARROW, new ProjectileDispenserBehavior() {
			@Override
			protected ProjectileEntity createProjectile(World world, Position position, ItemStack stack) {
				return CUSTOM_ARROW.createArrow(world, stack, position);
			}
		});
	}
}