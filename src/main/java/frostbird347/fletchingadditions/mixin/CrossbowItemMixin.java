package frostbird347.fletchingadditions.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ArrowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import frostbird347.fletchingadditions.entity.CustomArrowEntity;
import frostbird347.fletchingadditions.item.ItemManager;

@Mixin(CrossbowItem.class)
public class CrossbowItemMixin {

	//Treat crossbows like dispensers, because they can also be fired instantly
	@Inject(at = @At("RETURN"), method = "createArrow", locals = LocalCapture.CAPTURE_FAILSOFT)
	private static void fixArrow(World world, LivingEntity entity, ItemStack crossbow, ItemStack arrow, CallbackInfoReturnable<PersistentProjectileEntity> cir, ArrowItem arrowItem, PersistentProjectileEntity persistentProjectileEntity) {
		if (arrow.isOf(ItemManager.CUSTOM_ARROW)) {
			((CustomArrowEntity)persistentProjectileEntity).markAsDispenserArrow();
		}
	}
}
