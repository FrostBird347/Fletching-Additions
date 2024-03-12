package frostbird347.fletchingadditions.mixin;

import net.minecraft.datafixer.fix.ArrowPickupFix;
import java.util.function.Function;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mojang.datafixers.Typed;
import com.mojang.serialization.Dynamic;

//Ensure the arrow can be picked up by anyone if it has no owner
//I have no idea why this patch is in some seperate class
@Mixin(ArrowPickupFix.class)
public class PickupFix {

	@Shadow
	private static Dynamic<?> update(Dynamic<?> arrowData) {
		throw new AssertionError("You should never see this error message!");
	}

	@Shadow
	private Typed<?> updateEntity(Typed<?> typed, String choiceName, Function<Dynamic<?>, Dynamic<?>> updater) {
		throw new AssertionError("You should never see this error message!");
	}

	@Inject(at = @At("HEAD"), method = "update")
	public void fixArrow(Typed<?> typed, CallbackInfoReturnable ci) {
		typed = this.updateEntity(typed, "fletching-additions:custom_arrow", PickupFix::update);
	}
}