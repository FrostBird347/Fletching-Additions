package frostbird347.fletchingadditions.predicateProvider;

import frostbird347.fletchingadditions.item.ItemManager;
import net.minecraft.client.item.ModelPredicateProviderRegistry;
import net.minecraft.client.item.UnclampedModelPredicateProvider;
import net.minecraft.nbt.NbtElement;
import net.minecraft.util.Identifier;

public class PredicateProviderManager {

	public static void registerPredicateProviders() {
		TrueUnclampedModelPredicateProvider itemTextureDataProvider = (itemStack, clientWorld, livingEntity, value) -> {
			if (!itemStack.hasNbt() || !itemStack.getNbt().contains("itemTextureData", NbtElement.FLOAT_TYPE)) {
				return 0;
			}
			return itemStack.getNbt().getFloat("itemTextureData");
		};
		ModelPredicateProviderRegistry.register(ItemManager.CUSTOM_ARROW, new Identifier("fletching-additions", "texture_data"), (UnclampedModelPredicateProvider)itemTextureDataProvider);
	}
}