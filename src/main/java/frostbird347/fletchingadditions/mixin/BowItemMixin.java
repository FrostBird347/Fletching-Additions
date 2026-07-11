package frostbird347.fletchingadditions.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import frostbird347.fletchingadditions.MainMod;
import frostbird347.fletchingadditions.item.ItemManager;

@Mixin(BowItem.class)
public class BowItemMixin {
	private PlayerEntity lastKnownPlayer = null;

    @Inject(at = @At("HEAD"), method = "use")
    public void updatePlayer(World world, PlayerEntity user, Hand hand, CallbackInfoReturnable<TypedActionResult<ItemStack>> ci) {
		lastKnownPlayer = user;
	}

	@ModifyReturnValue(at = @At("RETURN"), method = "getMaxUseTime")
	public int changeUseTime(int defaultSpeed, @Local(ordinal = 0) ItemStack bow) {
		//MainMod.LOGGER.info(bow.getTranslationKey());
		//Get the player holding the bow
		if (lastKnownPlayer != null) {
			PlayerEntity player = lastKnownPlayer;
			//MainMod.LOGGER.info(player.getEntityName());

			//Get the arrows that they are going to fire
			ItemStack arrows = player.getArrowType(bow);
			if (!arrows.isEmpty() && arrows.isOf(ItemManager.CUSTOM_ARROW)) {
				//MainMod.LOGGER.info(arrows.getName().getString());
				
				//Finally extract the drawSpeed value, making sure to now allow 0 negative values
				NbtCompound arrowNbt = arrows.getNbt();
				if (arrowNbt != null && !arrowNbt.isEmpty()) {
					float drawSpeed = arrowNbt.getFloat("drawSpeed");
					//MainMod.LOGGER.info(Float.toString(drawSpeed));

					if (drawSpeed > 0f) {
						return Math.round(defaultSpeed * (1f/drawSpeed));
					}
				}
			}
		}
		return defaultSpeed;
	}
}
