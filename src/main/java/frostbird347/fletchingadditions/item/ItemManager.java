package frostbird347.fletchingadditions.item;

import net.minecraft.block.DispenserBlock;
import net.minecraft.block.dispenser.ProjectileDispenserBehavior;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Position;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;

public class ItemManager {
	public static final CustomArrowItem CUSTOM_ARROW = new CustomArrowItem(new Item.Settings().group(ItemGroup.COMBAT));

	public static void registerItems() {
		Registry.register(Registry.ITEM, new Identifier("fletching-additions", "custom_arrow"), (Item)CUSTOM_ARROW);

		DispenserBlock.registerBehavior(CUSTOM_ARROW, new ProjectileDispenserBehavior() {
			@Override
			protected ProjectileEntity createProjectile(World world, Position position, ItemStack stack) {
				return CUSTOM_ARROW.createArrow(world, stack, position);
			}
		});
	}
}