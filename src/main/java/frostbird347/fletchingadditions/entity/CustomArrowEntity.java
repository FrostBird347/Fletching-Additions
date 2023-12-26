package frostbird347.fletchingadditions.entity;

import java.util.ArrayList;
import frostbird347.fletchingadditions.MainMod;
import frostbird347.fletchingadditions.item.ItemManager;
import frostbird347.fletchingadditions.modCompat.ModCompatManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtDouble;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.particle.VibrationParticleEffect;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.RandomSeed;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import net.minecraft.world.event.BlockPositionSource;
import net.minecraft.world.event.EntityPositionSource;
import net.minecraft.world.event.PositionSource;

public class CustomArrowEntity extends PersistentProjectileEntity {
   static {
      ITEM_NBT = DataTracker.registerData(CustomArrowEntity.class, TrackedDataHandlerRegistry.NBT_COMPOUND);
      CLIENT_SOURCE_POS = DataTracker.registerData(CustomArrowEntity.class, TrackedDataHandlerRegistry.BLOCK_POS);
   }

   	//nbt
	private NbtCompound itemNbt = new NbtCompound();
	private static final TrackedData<NbtCompound> ITEM_NBT;

	//These values are not accessed via nbt for peformance reasons (I assume it's a bad idea to access nbt values each tick)
	private float flySpeedMult = 1;
	private float gravityMult = 1;
	private boolean breaksWhenWet = false;
	private boolean waterSpeed = false;
	private boolean dynamicLightingIfPossible = false;
	private boolean echoLink = false;
	private ArrayList<ParticleEffect> particles = new ArrayList<ParticleEffect>();
	//This list is more of a pain to extract than I expected, so I am just saving it here
	NbtList gameFlags = new NbtList();
	
	//Other stuff not directly related to nbt
	Vec3d realVel = new Vec3d(0, 0, 0);
	boolean isRealVel = true;
	public Vec3d serverSourcePos = new Vec3d(0, 0, 0);
	private static final TrackedData<BlockPos> CLIENT_SOURCE_POS;
	int echoDist = 1;
	//echoLink, 
	boolean[] tempGlobalFlags = new boolean[] {false};

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
		//Default values
		flySpeedMult = 1;
		gravityMult = 1;
		breaksWhenWet = false;
		waterSpeed = false;
		dynamicLightingIfPossible = false;
		echoLink = this.world.isClient;
		echoLink = false;
		particles = new ArrayList<ParticleEffect>();
		gameFlags = new NbtList();
		

		//If there is no nbt, stop processing
		if (nbt == null || nbt.isEmpty()) return;
		itemNbt = nbt;

		//Values that need to be accessed every tick are stored as variables for peformance reasons
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
		if (gameFlags.indexOf(NbtString.of("echoLink")) >= 0) echoLink = true;

		//Modify damage
		if (itemNbt.contains("damageMult", NbtElement.FLOAT_TYPE)) {
			this.setDamage(2.0 * itemNbt.getFloat("damageMult"));
		}

		//Process particles
		if (itemNbt.contains("particles")) {
			NbtList rawParticles = itemNbt.getList("particles", NbtElement.STRING_TYPE);
			for (int i = 0; i < rawParticles.size(); i++) {
				Identifier currentID = Identifier.tryParse(rawParticles.getString(i));
				if (currentID != null) {
					ParticleEffect currentType = (ParticleEffect)Registry.PARTICLE_TYPE.get(currentID);
					if (currentType != null) {
						particles.add(currentType);
					} else {
						MainMod.LOGGER.error("unknown particle: ", rawParticles.getString(i));
					}
				} else {
					MainMod.LOGGER.error("could not parse identifier of particle: ", rawParticles.getString(i));
				}
			}
		}

		//Extract name
		try {
			if (itemNbt.contains("display", NbtElement.COMPOUND_TYPE) && itemNbt.getCompound("display").contains("Name", NbtElement.STRING_TYPE)) {
				this.setCustomName(Text.Serializer.fromJson(itemNbt.getCompound("display").getString("Name")));
			}
		} catch(Exception err) {
			MainMod.LOGGER.error("Failed to parse arrow name: ", err);
		}
		
		MainMod.LOGGER.info(itemNbt.asString());
	}

	@Override
	public void tick() {
		Entity owner = this.getOwner();

		//Make the client load item nbt
		//This is fine because I don't intend to modify this data
		//Well, I might modify it to remove certain stats in one specific scenario, but it wouldn't really impact anything client side
		if (this.world.isClient && itemNbt.isEmpty() && !this.dataTracker.get(ITEM_NBT).isEmpty()) {
			initFromNbt(this.dataTracker.get(ITEM_NBT));
		} else if (!this.world.isClient) {
			if (this.dataTracker.get(ITEM_NBT).isEmpty() && !itemNbt.isEmpty()) {
				this.dataTracker.set(ITEM_NBT, itemNbt.copy());
			}

			//Only tell the client the sender's position after it has touched the ground
			if (!this.inGround && echoLink) {
				//Otherwise set the current position because it looks better to have the first particles stay inside the arrow than to have one or a few go flying in the wrong direction
				this.dataTracker.set(CLIENT_SOURCE_POS, this.getBlockPos());
			} else if (echoLink) {
				if (owner != null && owner.isAlive()) {
					this.dataTracker.set(CLIENT_SOURCE_POS, owner.getBlockPos());
				} else {
					this.dataTracker.set(CLIENT_SOURCE_POS, new BlockPos(serverSourcePos));
				}
			}
		}

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

		} else {
			realVel = Vec3d.ZERO;
			super.tick();
		}

		if (echoLink) {
			processEchoLink(owner);
		}
		
		if (this.world.isClient && !this.inGround && particles.size() > 0) {
			//Generate a random number in the range of -0.49 to (particles.size - 0.51) and then round
			//This guarantees that there is zero chance of it going outside the bounds of the list, while also being just as likely to pick the first/last options instead of being half as likely
			int partIndex = (int)Math.round((Math.random() * ((double)particles.size() - 0.02)) - 0.49);
			this.world.addParticle(particles.get(partIndex), this.getX(), this.getY(), this.getZ(), 0, 0, 0);
		}
	}

	//Just pass owner over so we don't have to get it multiple times
	private void processEchoLink(Entity owner) {
		if (this.inGround) {

			//Vibration particles to the player
			if (this.world.isClient) {
				//Make it follow the player, otherwise go to it's source block
				PositionSource source = null;
				if (owner != null && owner.isAlive()) {
					source = new EntityPositionSource(owner, DEFAULT_FRICTION);
					echoDist = (int)Math.ceil(this.getPos().distanceTo(owner.getPos()));
				} else {
					BlockPos sourceBlockPos = this.dataTracker.get(CLIENT_SOURCE_POS);
					source = new BlockPositionSource(sourceBlockPos);
					echoDist = Math.max(echoDist, (int)Math.ceil(this.getPos().distanceTo(new Vec3d(sourceBlockPos.getX(), sourceBlockPos.getY(), sourceBlockPos.getZ()))));
				}
				VibrationParticleEffect newParticle = new VibrationParticleEffect(source, echoDist);
				this.world.addParticle(newParticle, this.getX(), this.getY(), this.getZ(), 0, 0, 0);
			//Otherwise make the server calculate distance
			//But only do it once a second
			} else if (this.inGroundTime % 20 == 0) {
				if (owner != null && owner.isAlive() && !owner.isSpectator()) {
					echoDist = (int)Math.ceil(this.getPos().distanceTo(owner.getPos()));
				} else {
					echoDist = Math.max(echoDist, (int)Math.ceil(this.getPos().distanceTo(serverSourcePos)));
				}
			}
			
			//Arrow return effect
			if (!this.world.isClient && this.inGroundTime >= 30 + echoDist && !tempGlobalFlags[0]) {
				tempGlobalFlags[0] = true;
				ServerWorld thisWorld = this.world.getServer().getWorld(this.world.getRegistryKey());
				thisWorld.spawnParticles(ParticleTypes.SONIC_BOOM, this.getX(), this.getY(), this.getZ(), 1, 0, 0, 0, 0);
				//TODO: replace with a custom sound
				thisWorld.playSoundFromEntity(null, this, SoundEvents.ENTITY_WARDEN_SONIC_CHARGE, this.getSoundCategory(), 0.5f, 4f, RandomSeed.getSeed());
			}

			//Actually give the arrow back, half a second later
			if (!this.world.isClient && this.inGroundTime >= 40 + echoDist) {
				//If the owner exists, is alive and isn't in spectator mode, we give them back the arrow
				if (owner != null && owner.isAlive() && !owner.isSpectator()) {
					if (owner.isPlayer()) {
						((PlayerEntity)owner).giveItemStack(this.asItemStack());
					//If they aren't a player than we drop the arrow as an item at their position
					} else {
						this.setPosition(owner.getPos());
						this.dropStack(this.asItemStack());
					}
				//Otherwise teleport it back to the source position and drop itself as an item
				} else {
					this.setPosition(serverSourcePos);
					this.dropStack(this.asItemStack());
				}
				//Finally remove the arroe entity
				this.discard();
			}
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

	//stop silent arrows from triggering sensors
	@Override
	public boolean occludeVibrationSignals() {
		return gameFlags.indexOf(NbtString.of("silent")) >= 0;
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
			
			//Use soul flames if the arrow is also on fire (from lava/fire or the flame enchant), if https://modrinth.com/mod/on-soul-fire is installed
			//Or if it has the isSoulFire flag
			if (this.isOnFire() || gameFlags.indexOf(NbtString.of("isSoulFire")) >= 0) {
				ModCompatManager.ON_SOUL_FIRE.executeAction("applySoulFlame", new Object[] { (Object)target });
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
		MainMod.LOGGER.info(Boolean.toString(this.world.isClient));
		super.readCustomDataFromNbt(nbt);
		if (nbt.contains("itemNbt", NbtElement.COMPOUND_TYPE)) {
			initFromNbt(nbt.getCompound("itemNbt"));
		}

		//Store sourcePos incase the arrow is shot from outside the player's render distance
		NbtList rawSourcePos = itemNbt.getList("sourcePos", NbtElement.DOUBLE_TYPE);
		if (rawSourcePos != null && rawSourcePos.size() == 3) {
			serverSourcePos = new Vec3d(rawSourcePos.getDouble(0), rawSourcePos.getDouble(1), rawSourcePos.getDouble(2));
		} else {
			serverSourcePos = new Vec3d(this.getX(), this.getY(), this.getZ());
		}

		if (itemNbt != null) {
			this.dataTracker.set(ITEM_NBT, itemNbt.copy());
		} else {
			this.dataTracker.set(ITEM_NBT, new NbtCompound());
		}
	}

	@Override
	public void writeCustomDataToNbt(NbtCompound nbt) {
		MainMod.LOGGER.info(Boolean.toString(this.world.isClient));
		super.writeCustomDataToNbt(nbt);
		if (this.itemNbt != null && !this.itemNbt.isEmpty()) {
			nbt.put("itemNbt", this.itemNbt);
		}

		//Store sourcePos incase the arrow is shot from outside the player's render distance
		//The chances of it being exactly zero should be pretty much impossible to occur
		if (!serverSourcePos.equals(Vec3d.ZERO)) {
			NbtList rawSourcePos = new NbtList();
			rawSourcePos.add(NbtDouble.of(serverSourcePos.x));
			rawSourcePos.add(NbtDouble.of(serverSourcePos.y));
			rawSourcePos.add(NbtDouble.of(serverSourcePos.z));
		}
	}

	protected void initDataTracker() {
		super.initDataTracker();
		this.dataTracker.startTracking(ITEM_NBT, new NbtCompound());
		this.dataTracker.startTracking(CLIENT_SOURCE_POS, new BlockPos(0, 0, 0));
	}
}