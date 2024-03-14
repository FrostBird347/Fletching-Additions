package frostbird347.fletchingadditions.mixin;

import net.minecraft.recipe.RecipeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import frostbird347.fletchingadditions.recipe.FletchingRecipeSerializer;

@Mixin(RecipeManager.class)
public class RecipeManagerMixin {

	//Clear the FletchingRecipeSerializer's cached errors
	@Inject(at = @At("HEAD"), method = "apply")
	private void beforeLoad(CallbackInfo info) {
		FletchingRecipeSerializer.PREVIOUS_ERRORS.clear();
	}
}