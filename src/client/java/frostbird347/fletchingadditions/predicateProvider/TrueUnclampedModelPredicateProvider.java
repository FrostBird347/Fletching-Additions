package frostbird347.fletchingadditions.predicateProvider;

import net.minecraft.client.item.UnclampedModelPredicateProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.Nullable;

//For some reason, UnclampedModelPredicateProvider clamps values from 0 to 1
public interface TrueUnclampedModelPredicateProvider extends UnclampedModelPredicateProvider {
    @Override
    default public float call(ItemStack itemStack, @Nullable ClientWorld clientWorld, @Nullable LivingEntity livingEntity, int i) {
        return (float)this.unclampedCall(itemStack, clientWorld, livingEntity, i);
    }

    public float unclampedCall(ItemStack var1, @Nullable ClientWorld var2, @Nullable LivingEntity var3, int var4);
}

