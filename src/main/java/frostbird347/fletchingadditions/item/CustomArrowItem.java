package frostbird347.fletchingadditions.item;

import frostbird347.fletchingadditions.entity.CustomArrowEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ArrowItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Position;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class CustomArrowItem extends ArrowItem {
	public CustomArrowItem(Item.Settings settings) {
		super(settings);
	}

	@Override
	public PersistentProjectileEntity createArrow(World world, ItemStack stack, LivingEntity shooter) {
		CustomArrowEntity customArrowEntity = new CustomArrowEntity(world, shooter);
		customArrowEntity.initFromStack(stack);
		return customArrowEntity;
	}

	public PersistentProjectileEntity createArrow(World world, ItemStack stack, Position position) {
		CustomArrowEntity customArrowEntity = new CustomArrowEntity(world, position.getX(), position.getY(), position.getZ());
		customArrowEntity.initFromStack(stack);
		customArrowEntity.serverSourcePos = new Vec3d(position.getX(), position.getY(), position.getZ());
		return customArrowEntity;
	}
}