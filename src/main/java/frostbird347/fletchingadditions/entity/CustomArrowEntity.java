package frostbird347.fletchingadditions.entity;

import frostbird347.fletchingadditions.MainMod;
import frostbird347.fletchingadditions.item.ItemManager;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class CustomArrowEntity extends PersistentProjectileEntity {
	private NbtCompound itemNbt = null;
	//These values are not accessed via nbt for peformance reasons (I assume it's a bad idea to access nbt values each tick)
	private float flySpeedMult = 1;
	private float gravityMult = 1;
	private boolean breaksWhenWet = false;
	private boolean waterSpeed = false;
	private boolean dynamicLightingIfPossible = false;
	//This list is more of a pain to extract than I expected, so I am just saving it here
	NbtList gameFlags = new NbtList();
	
	//Other stuff unrelated to nbt
	Vec3d realVel = new Vec3d(0, 0, 0);
	boolean isRealVel = true;

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

		//Values that need to be accessed every tick are stored as variables for peformance reasons
		flySpeedMult = 1;
		gravityMult = 1;
		if (itemNbt.contains("flySpeed", NbtElement.FLOAT_TYPE)) {
			flySpeedMult = itemNbt.getFloat("flySpeed");
		}
		if (itemNbt.contains("gravityMult", NbtElement.FLOAT_TYPE)) {
			gravityMult = itemNbt.getFloat("gravityMult");
		}
		//Same as above, but gameFlags instead
		breaksWhenWet = false;
		waterSpeed = false;
		dynamicLightingIfPossible = false;
		//Store gameFlags for easier future access
		gameFlags = itemNbt.getList("gameFlags", NbtElement.STRING_TYPE);
	
		if (gameFlags.indexOf(NbtString.of("breaksWhenWet")) >= 0) breaksWhenWet = true;
		if (gameFlags.indexOf(NbtString.of("waterSpeed")) >= 0) waterSpeed = true;
		if (gameFlags.indexOf(NbtString.of("dynamicLightingIfPossible")) >= 0) dynamicLightingIfPossible = true;
		
		MainMod.LOGGER.info(itemNbt.asString());
	}

	@Override
	public void tick() {
		//Don't mess with velocity when the arrow is in the ground
		//This will hopefully fix client side velocity desync issues
		if (!this.isOnGround()) {
			//Seems to improve client side interpolation issues with slow speeds
			if (this.world.isClient && !isRealVel) {
				this.setVelocity(this.getVelocity().add(0, 0.05f, 0).multiply(1f / flySpeedMult).subtract(0, 0.05f * gravityMult, 0));
				isRealVel = true;
			}
			realVel = this.getVelocity();
			this.setVelocity(realVel.multiply(flySpeedMult));
			isRealVel = false;
			super.tick();
			//Seems to improve client side interpolation issues with slow speeds
			if (!this.world.isClient && !isRealVel) {
				this.setVelocity(this.getVelocity().add(0, 0.05f, 0).multiply(1f / flySpeedMult).subtract(0, 0.05f * gravityMult, 0));
				isRealVel = true;
			}
			
			if (this.world.isClient && !this.inGround) {
				this.world.addParticle(ParticleTypes.INSTANT_EFFECT, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
			}
		} else {
			realVel = Vec3d.ZERO;
			super.tick();
		}
	}
//double x, double y, double z, SoundEvent sound, SoundCategory category, float volume, float pitch, boolean useDistance
	@Override
    public void extinguish() {
		if (breaksWhenWet) {
			this.playSound(SoundEvents.ENTITY_GENERIC_EXTINGUISH_FIRE, 0.25f, 1.2f / (this.random.nextFloat() * 0.2f + 0.9f));
			if (!this.world.isClient) {
				this.kill();
			}
		}
		super.extinguish();
	}

	@Override
	protected ItemStack asItemStack() {
		ItemStack stack = new ItemStack(ItemManager.CUSTOM_ARROW);
		stack.setNbt(itemNbt);
		return stack;
	}
	
	@Override
	protected void onEntityHit(EntityHitResult entityHitResult) {
		this.setVelocity(this.getVelocity().add(0, 0.05f, 0).multiply(1f / flySpeedMult).subtract(0, 0.05f * gravityMult, 0));
		isRealVel = true;
		super.onEntityHit(entityHitResult);
	}

	@Override
	protected void onHit(LivingEntity target) {
		super.onHit(target);
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