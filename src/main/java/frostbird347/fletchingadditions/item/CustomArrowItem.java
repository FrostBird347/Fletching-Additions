package frostbird347.fletchingadditions.item;

import frostbird347.fletchingadditions.entity.CustomArrowEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ArrowItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
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
}