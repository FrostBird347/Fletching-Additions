package frostbird347.fletchingadditions.entity;

import frostbird347.fletchingadditions.MainMod;
import frostbird347.fletchingadditions.item.ItemManager;
import net.minecraft.entity.Entity;
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
	private NbtCompound itemNbt = new NbtCompound();
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

	public boolean reallyOnGround() {
		return (this.isOnGround() || this.inGround);
	}

	public void swapToRealVel() {
		if (!isRealVel) {
				float gravity = this.reallyOnGround() ? 0f : 0.05f;
				this.setVelocity(this.getVelocity().add(0, gravity, 0).multiply(1f / flySpeedMult).subtract(0, gravity * gravityMult, 0));
				isRealVel = true;
		}
	}

	public void initFromNbt(NbtCompound nbt) {
		if (nbt == null || nbt.isEmpty()) return;
		itemNbt = nbt;

		//Values that need to be accessed every tick are stored as variables for peformance reasons
		flySpeedMult = 1;
		gravityMult = 1;
		if (itemNbt.contains("flySpeed", NbtElement.FLOAT_TYPE)) {
			flySpeedMult = itemNbt.getFloat("flySpeed");
			//Make sure we don't divide by 0, and also negative flySpeed values should not be supported either
			if (flySpeedMult <= 0) {
				flySpeedMult = 1f;
				MainMod.LOGGER.error("flySpeed attribute of an arrow was equal or less than 0. flySpeed has been reset to 1!");
			}
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

		//Modify damage
		if (itemNbt.contains("damageMult", NbtElement.FLOAT_TYPE)) {
			this.setDamage(2.0 * itemNbt.getFloat("damageMult"));
		}
		
		MainMod.LOGGER.info(itemNbt.asString());
	}

	@Override
	public void tick() {
		//Don't mess with velocity when the arrow is in the ground
		//This will hopefully fix client side velocity desync issues
		if (!this.reallyOnGround()) {
			//Seems to improve client side interpolation issues with slow speeds
			if (this.world.isClient) {
				swapToRealVel();
			}
			realVel = this.getVelocity();
			this.setVelocity(realVel.multiply(flySpeedMult));
			isRealVel = false;
			super.tick();
			//Seems to improve client side interpolation issues with slow speeds
			if (!this.world.isClient) {
				swapToRealVel();
			}
			
			if (this.world.isClient && !this.inGround) {
				this.world.addParticle(ParticleTypes.INSTANT_EFFECT, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
			}
		} else {
			realVel = Vec3d.ZERO;
			super.tick();
		}
	}

	@Override
	public void onBubbleColumnSurfaceCollision(boolean down) {
		swapToRealVel();

		Vec3d oldVel = this.getVelocity();
		double newSpeed = down ? Math.max(-0.9, oldVel.y - 0.03) : Math.min(1.8, oldVel.y + 0.1);
		this.setVelocity(oldVel.x, newSpeed, oldVel.z);
	}

	@Override
	public void onBubbleColumnCollision(boolean down) {
		swapToRealVel();

		Vec3d oldVel = this.getVelocity();
		double newSpeed = down ? Math.max(-0.3 * flySpeedMult, oldVel.y - 0.03) : Math.min(0.7 * flySpeedMult, oldVel.y + 0.06);
		this.setVelocity(oldVel.x, newSpeed, oldVel.z);
		this.onLanding();
	}

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

	//Make arrows with waterSpeed still be a little slower in water than air
	//Air has a drag of 0.99
	@Override
	protected float getDragInWater() {
		if (waterSpeed) return 0.9f;
		return super.getDragInWater();
	}

	@Override
	protected ItemStack asItemStack() {
		ItemStack stack = new ItemStack(ItemManager.CUSTOM_ARROW);
		stack.setNbt(itemNbt);
		return stack;
	}
	
	@Override
	protected void onEntityHit(EntityHitResult entityHitResult) {
		swapToRealVel();
		super.onEntityHit(entityHitResult);

		//Process fireChance
		NbtList fireChances = itemNbt.getList("fireChance", NbtElement.FLOAT_TYPE);
		int fireHits = 0;
		for (int i = 0; i < fireChances.size(); i++) {
			double chance = Math.random();
			if (chance <= fireChances.getFloat(i)) {
				fireHits++;
			}
		}
		
		//Apply the correct amount of fire depending on the number of hits above
		if (fireHits > 0) {
			double shiftedFireHits = (float)fireHits + Math.random() - 0.5;
			//Thanks to https://tools.softinery.com/CurveFitter/ for the 2 burnTime curves below
			//y=-0.18466x^{4}+2.48169x^{3}-8.9517x^{2}+13.3603x-1.00379
			double burnTime = -0.18466 * Math.pow(shiftedFireHits, 4) + 2.48169 * Math.pow(shiftedFireHits, 3) - 8.9517 * Math.pow(shiftedFireHits, 2) + 13.3603 * shiftedFireHits + 1.00379;
			//We have to switch to a difference equation after the one above peaks
			if (shiftedFireHits > 6.977) {
				//-12.32896+10.61645*x
				burnTime = -12.32896 + 10.61645 * shiftedFireHits;
			}

			//Set the entity on fire
			Entity target = entityHitResult.getEntity();
			target.setOnFireFor((int)Math.round(burnTime));

			//Also set the OnSoulFire flag if the arrow is also on fire (from lava/fire or the flame enchant), which will cause the target to be burned with soul flames if https://modrinth.com/mod/on-soul-fire is installed
			if (this.isOnFire()) {
				//Extract the nbt nessecary
				NbtCompound targetNbt = target.writeNbt(new NbtCompound());
				NbtCompound cardinalComponentsNbt = targetNbt.getCompound("cardinal_components");
				NbtCompound onSoulFireNbt = cardinalComponentsNbt.getCompound("onsoulfire:on_soul_fire");
				
				//Set the values
				onSoulFireNbt.putByte("OnSoulFire", (byte)1);
				cardinalComponentsNbt.put("onsoulfire:on_soul_fire", onSoulFireNbt);
				targetNbt.put("cardinal_components", cardinalComponentsNbt);

				//Update the target entity nbt
				target.readNbt(targetNbt);
			}
			MainMod.LOGGER.info(Integer.toString(fireHits) + ":" + Double.toString(burnTime));
		}
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
		if (this.itemNbt != null && !this.itemNbt.isEmpty()) {
			nbt.put("itemNbt", this.itemNbt);
		}
	}
}