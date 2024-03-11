package frostbird347.fletchingadditions.mixin;

import net.minecraft.client.resource.language.LanguageManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import frostbird347.fletchingadditions.MainMod;

@Mixin(LanguageManager.class)
public class AfterLangLoad {

	//Clear the FletchingRecipeSerializer's cached errors
	@Inject(at = @At("TAIL"), method = "reload")
	private void afterLoad(CallbackInfo info) {
		MainMod.langReloads++;
	}
}