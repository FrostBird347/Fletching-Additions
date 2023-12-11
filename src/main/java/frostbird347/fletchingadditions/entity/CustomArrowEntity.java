package frostbird347.fletchingadditions.entity;

import frostbird347.fletchingadditions.item.ItemManager;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.world.World;

public class CustomArrowEntity extends PersistentProjectileEntity {
    private int duration = 200;
    private NbtCompound itemNbt = null;

    public CustomArrowEntity(EntityType<? extends CustomArrowEntity> entityType, World world) {
        super((EntityType<? extends PersistentProjectileEntity>)entityType, world);
    }

    public CustomArrowEntity(World world, LivingEntity owner) {
        super(EntityManager.CUSTOM_ARROW, owner, world);
    }

    public CustomArrowEntity(World world, double x, double y, double z) {
        super(EntityManager.CUSTOM_ARROW, x, y, z, world);
        
    }

    public void initFromStack(ItemStack stack) {
        initFromNbt(stack.getNbt());
    }

    public void initFromNbt(NbtCompound nbt) {
        if (nbt == null || nbt.isEmpty()) return;
        itemNbt = nbt;
        
        if (itemNbt.contains("testKey", NbtElement.INT_TYPE)) {
            duration = itemNbt.getInt("testKey");
        } else {
            duration = (int)(Math.random() * 200);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.world.isClient && !this.inGround) {
            this.world.addParticle(ParticleTypes.INSTANT_EFFECT, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
        }
    }

    @Override
    protected ItemStack asItemStack() {
        ItemStack stack = new ItemStack(ItemManager.CUSTOM_ARROW);
        stack.setNbt(itemNbt);
        return stack;
    }

    @Override
    protected void onHit(LivingEntity target) {
        super.onHit(target);
        StatusEffectInstance statusEffectInstance = new StatusEffectInstance(StatusEffects.GLOWING, this.duration, 0);
        target.addStatusEffect(statusEffectInstance, this.getEffectCause());
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.contains("itemNbt", NbtElement.COMPOUND_TYPE)) {
            initFromNbt(nbt.getCompound("itemNbt"));
        }
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        if (this.itemNbt != null) {
            nbt.put("itemNbt", this.itemNbt);
        }
    }
}