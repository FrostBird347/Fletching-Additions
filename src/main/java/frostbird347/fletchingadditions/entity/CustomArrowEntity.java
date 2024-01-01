package frostbird347.fletchingadditions.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import org.jetbrains.annotations.Nullable;
import frostbird347.fletchingadditions.MainMod;
import frostbird347.fletchingadditions.item.ItemManager;
import frostbird347.fletchingadditions.modCompat.ModCompatManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.ProjectileDamageSource;
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
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.math.random.RandomSeed;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import net.minecraft.world.World;
import net.minecraft.world.event.BlockPositionSource;
import net.minecraft.world.event.EntityPositionSource;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.event.GameEvent.Emitter;
import net.minecraft.world.event.PositionSource;
import net.minecraft.world.event.listener.EntityGameEventHandler;
import net.minecraft.world.event.listener.GameEventListener;
import net.minecraft.world.event.listener.VibrationListener;

public class CustomArrowEntity extends PersistentProjectileEntity implements VibrationListener.Callback {
	static {
		ITEM_NBT = DataTracker.registerData(CustomArrowEntity.class, TrackedDataHandlerRegistry.NBT_COMPOUND);
		CLIENT_SOURCE_POS = DataTracker.registerData(CustomArrowEntity.class, TrackedDataHandlerRegistry.BLOCK_POS);
		SHOW_FIREWORK_SPARKS = DataTracker.registerData(CustomArrowEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
	}

	//Nbt stuff
	private NbtCompound itemNbt = new NbtCompound();
	private static final TrackedData<NbtCompound> ITEM_NBT;
	private boolean clientHasNbt = false;
	NbtList gameFlags = new NbtList();

	//These values are not accessed via nbt for peformance reasons (I assume it's a bad idea to access nbt values each tick)
	private float flySpeedMult = 1;
	private float gravityMult = 1;
	private boolean breaksWhenWet = false;
	private boolean waterSpeed = false;
	private boolean dynamicLightingIfPossible = false;
	private boolean echoLink = false;
	private boolean isSensor = false;
	private boolean hasFirework = false;
	private ArrayList<ParticleEffect> particles = new ArrayList<ParticleEffect>();
	
	//Other stuff not directly related to nbt
	Vec3d realVel = new Vec3d(0, 0, 0);
	boolean isRealVel = true;
	public Vec3d serverSourcePos = new Vec3d(0, 0, 0);
	private static final TrackedData<BlockPos> CLIENT_SOURCE_POS;
	private static final TrackedData<Boolean> SHOW_FIREWORK_SPARKS;
	private int flightTime = -1;
	private Vec3d flightEndSpeed = new Vec3d(0, 0, 0);
	int echoDist = 1;
	private final EntityGameEventHandler<VibrationListener> vibrationListenerEventHandler = new EntityGameEventHandler<VibrationListener>(new VibrationListener(new EntityPositionSource(this, 0f), 128, this, (VibrationListener.Vibration)null, 0f, 0));
	Vec3i lastBlockHitDir = new Vec3i(0, 0, 0);
	//echoLink returning/returned, 
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
		initFromNbt(stack.getNbt().copy());
	}

	public boolean reallyOnGround() {
		return (this.isOnGround() || this.inGround);
	}

	public void swapToRealVel() {
		swapToRealVel(true);
	}

	public void swapToRealVel(boolean undoGravity) {
		if (!isRealVel) {
				float gravity = this.reallyOnGround() ? 0f : undoGravity ? 0.05f : 0f;
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
		echoLink = false;
		hasFirework = false;
		particles = new ArrayList<ParticleEffect>();
		gameFlags = new NbtList();
		if (!this.world.isClient) this.dataTracker.set(SHOW_FIREWORK_SPARKS, false);

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
		if (gameFlags.indexOf(NbtString.of("isSensor")) >= 0) isSensor = true;
		if (gameFlags.indexOf(NbtString.of("inheritFireworkNBT")) >= 0) hasFirework = true;
		if (!this.world.isClient) this.dataTracker.set(SHOW_FIREWORK_SPARKS, hasFirework);

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
		
		syncItemNbt();
		if (!this.world.isClient) {
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

			//Firework flight
			//Loading and unloading will cause all this to reset, however this isn't a huge issue considering how long this lasts
			if (hasFirework) {
				realVel = this.getVelocity();
				this.setVelocity(realVel.multiply(flySpeedMult));
				isRealVel = false;

				//Setup firework
				if (flightTime == -1) {
					flightTime = 10 * itemNbt.getCompound("inheritFireworkNBT").getCompound("Fireworks").getByte("Flight") +  + this.random.nextInt(6) + this.random.nextInt(7);

					this.gravityMult = 0;
					flightEndSpeed = this.getVelocity();
					//0.2*(1.02^(20*3)) = final speed multiple before explosion
					this.setVelocity(this.getVelocity().multiply(0.2));
					//Don't allow arrows to go too slowly
					if (this.getVelocity().length() < 0.6) {
						this.setVelocity(this.getVelocity().normalize().multiply(0.6));
					}
					this.world.getServer().getWorld(this.world.getRegistryKey()).playSoundFromEntity(null, this, SoundEvents.ENTITY_FIREWORK_ROCKET_LAUNCH, this.getSoundCategory(), 3f, 1f, RandomSeed.getSeed());;

				//When it's going to explode
				} else if (flightTime == 0) {
					NbtList explosions = this.itemNbt.getCompound("inheritFireworkNBT").getCompound("Fireworks").getList("Explosions", NbtElement.COMPOUND_TYPE).copy();
					this.world.sendEntityStatus(this, (byte)18);
					explodeFirework(explosions);
					this.setVelocity(this.getVelocity().normalize().multiply(explosions.size() * 0.3).add(flightEndSpeed));
					removeFireworkRocket();
				
				//While flying
				} else {
					flightTime--;
					this.gravityMult = 0;
					
					this.setVelocity(this.getVelocity().multiply(1.02, 1.02, 1.02));
				}

				swapToRealVel(false);
			}

			//Tick sensor listener
			this.vibrationListenerEventHandler.getListener().tick(this.world.getServer().getWorld(this.world.getRegistryKey()));
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

		if (this.world.isClient && this.age % 2 < 2 && this.dataTracker.get(SHOW_FIREWORK_SPARKS)) {
			this.world.addParticle(ParticleTypes.FIREWORK, this.getX(), this.getY(), this.getZ(), this.random.nextGaussian() * 0.05, -this.getVelocity().y * 0.5, this.random.nextGaussian() * 0.05);
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

	//Sculk sensor signal listener stuff
	@Override
	public void accept(ServerWorld world, GameEventListener listener, BlockPos pos, GameEvent event, @Nullable Entity entity, @Nullable Entity sourceEntity, float distance) {
		if (!this.isRemoved()) {
			//TODO: replace with custom sound
			this.playSound(SoundEvents.ENTITY_WARDEN_TENDRIL_CLICKS, 0.75f, 1.5f);
			Vec3d targetPos = new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
			if (entity != null && entity.getBoundingBox() != null) {
				targetPos = entity.getBoundingBox().getCenter();
			}

			Vec3d targetDir = new Vec3d((double)targetPos.getX() - this.getX(), (double)targetPos.getY() - this.getY(), (double)targetPos.getZ() - this.getZ());
			targetDir = targetDir.normalize();
			Vec3d moveMult = new Vec3d(1, 1, 1);

			if (this.reallyOnGround()) {
				Vec3d boostVel = new Vec3d(lastBlockHitDir.getX(), lastBlockHitDir.getY(), lastBlockHitDir.getZ()).multiply(0.25f);
				moveMult = new Vec3d(((lastBlockHitDir.getX() == 1 && targetDir.getX() < 0) || (lastBlockHitDir.getX() == -1 && targetDir.getX() > 0)) ? -1 : 1, ((lastBlockHitDir.getY() == 1 && targetDir.getY() < 0) || (lastBlockHitDir.getY() == -1 && targetDir.getY() > 0)) ? -1 : 1, ((lastBlockHitDir.getZ() == 1 && targetDir.getZ() < 0) || (lastBlockHitDir.getZ() == -1 && targetDir.getZ() > 0)) ? -1 : 1);
				MainMod.LOGGER.info(lastBlockHitDir.toString());
				this.setVelocity(this.getVelocity().add(boostVel));
				this.inGround = false;

				this.setVelocity(this.getVelocity().multiply(0.25).add(targetDir.multiply(0.5).multiply(moveMult).multiply(Math.random() * 0.02 + 0.99, Math.random() * 0.02 + 0.99, Math.random() * 0.02 + 0.99)));
			} else {
				//Ignore downwards speed so we can't fly up forever
				double currentSpeed = new Vec3d(this.getVelocity().getX(), Math.max(this.getVelocity().getY(), 0), this.getVelocity().getZ()).length();

				this.setVelocity(targetDir.multiply(currentSpeed));
			}

			
		}
	}

	//Sculk sensor signal listener stuff
	@Override
	public boolean accepts(ServerWorld world, GameEventListener listener, BlockPos pos, GameEvent event, Emitter emitter) {

		//Pretty much the same checks the warden does for every vibration
		if (this.isSensor && this.age > 2 && world.getWorldBorder().contains(pos) && !this.isRemoved() && this.world == world) {
			Entity source = emitter.sourceEntity();
			if (source == null) {
				return true;
			} else if (!(source instanceof LivingEntity) && this.world == source.world) {
				return (!(source instanceof CustomArrowEntity) || !((CustomArrowEntity)source).isSensor);
			} else if (source instanceof LivingEntity && this.world == source.world && EntityPredicates.EXCEPT_CREATIVE_OR_SPECTATOR.test(source) && (this.getOwner() == null || !this.getOwner().isTeammate(source)) && ((LivingEntity)source).getType() != EntityType.WARDEN && !((LivingEntity)source).isInvulnerable() && !((LivingEntity)source).isDead() && this.world.getWorldBorder().contains(((LivingEntity)source).getBoundingBox())) {
				return true;
			}
		}

		return false;
	}

	//Sculk sensor signal listener stuff
	public void updateEventHandler(BiConsumer<EntityGameEventHandler<?>, ServerWorld> callback) {
		if (!this.world.isClient) {
			callback.accept(this.vibrationListenerEventHandler, this.world.getServer().getWorld(this.world.getRegistryKey()));
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
				this.discard();
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
		//Activate the noFins flag only when the arrow is picked up
		int noFinsIndex = gameFlags.indexOf(NbtString.of("_noFins"));
		if (noFinsIndex >= 0) {
			gameFlags.set(noFinsIndex, NbtString.of("noFins"));
		}

		ItemStack stack = new ItemStack(ItemManager.CUSTOM_ARROW);
		stack.setNbt(itemNbt.copy());
		return stack;
	}

	private void explodeFirework(NbtList explosions) {
		NbtCompound fireworkExplosion = new NbtCompound();
		fireworkExplosion.put("Explosions", explosions);

		//If it's the server, we just need to deal damage and not bother with effects
		if (!this.world.isClient) {
			dealFireworkDamage(fireworkExplosion);
			return;
		}

		if (explosions.size() == 0) {
			this.world.addParticle(ParticleTypes.POOF, this.getX(), this.getY(), this.getZ(), this.random.nextGaussian() * 0.05, 0.005, this.random.nextGaussian() * 0.05);
		} else {
			Vec3d vel = this.getVelocity();
			this.world.addFireworkParticle(this.getX(), this.getY(), this.getZ(), vel.x, vel.y, vel.z, fireworkExplosion);
		}
	}

	//Just reimplement the firework's damage code
	private void dealFireworkDamage(NbtCompound fireworkExplosion) {
		float damage = 0;
		NbtList explosions = fireworkExplosion.getList("Explosions", NbtElement.COMPOUND_TYPE);

		if (!explosions.isEmpty()) {
			damage = 5.0F + (float)(explosions.size() * 2);
			
			List<LivingEntity> targets = this.world.getNonSpectatingEntities(LivingEntity.class, this.getBoundingBox().expand(5.0));
			for (int iT = 0; iT < targets.size(); iT++) {
				LivingEntity target = targets.get(iT);

				boolean wasHit = false;
				//Check the bottom, middle and top of of their hitbox
				for(int iH = 0; iH < 2; iH++) {
					Vec3d checkPos = new Vec3d(target.getX(), target.getBodyY(0.5 * (double)iH), target.getZ());
					HitResult blocksHit = this.world.raycast(new RaycastContext(this.getPos(), checkPos, ShapeType.COLLIDER, FluidHandling.NONE, this));

					//If it hit, we don't need to check the other spots on their hitbox
					if (blocksHit.getType() == HitResult.Type.MISS) {
						wasHit = true;
						break;
					}
				}

				if (wasHit) {
					float finalDamage = damage * (float)Math.sqrt((5.0 - (double)this.distanceTo(target)) / 5.0);
					DamageSource damageSource = new ProjectileDamageSource("fireworks", this, this.getOwner()).setExplosive();
					
					//If it's right at the edge of the explosion distance, NaN damage will be given
					//Im not sure how this doesn't occur with fireworks shot from crossbows, since the damage calculation code is the same
					if (!Float.isNaN(finalDamage)) {
						target.damage(damageSource, finalDamage);
					}
				}
			}
		}
	}

	@Override
	protected void onBlockHit(BlockHitResult hit) {
		lastBlockHitDir = hit.getSide().getVector();
		super.onBlockHit(hit);
		afterBlockOrEntityHit();
	}

	//Called when a rocket powered arrow explodes
	private void removeFireworkRocket() {
		//Change the item name but not the entity name, so this will only have an impact when the arrow is picked up or unloaded/reloaded
		itemNbt.getCompound("display").putString("Name", itemNbt.getCompound("display").getString("Name").replaceFirst("Rocket Powered ", "Unfinned "));
		//Replace inheritFireworkNBT with the noFins gameflag (will only apply once the arrow is picked up)
		//Also remove the inheritFireworkNBT data, to allow arrows which only had differing fireworks to be stacked together
		int flagIndex = gameFlags.indexOf(NbtString.of("inheritFireworkNBT"));
		if (flagIndex <= 0) {
			gameFlags.set(flagIndex, NbtString.of("_noFins"));
		}
		itemNbt.remove("inheritFireworkNBT");

		//Refresh gravity
		gravityMult = 1;
		if (itemNbt.contains("gravityMult", NbtElement.FLOAT_TYPE)) {
			gravityMult = itemNbt.getFloat("gravityMult");
		}

		hasFirework = false;
		this.world.sendEntityStatus(this, (byte)1);
		//Just to be extra sure, manually set this as well
		this.dataTracker.set(SHOW_FIREWORK_SPARKS, false);
	}

	private void afterBlockOrEntityHit() {

		//Firework stuff
		NbtList explosions = new NbtList();

		if (!this.world.isClient && gameFlags.indexOf(NbtString.of("inheritFireworkNBT")) >= 0 && this.itemNbt.contains("inheritFireworkNBT", NbtElement.COMPOUND_TYPE) && this.itemNbt.getCompound("inheritFireworkNBT").contains("Fireworks", NbtElement.COMPOUND_TYPE)) {
			if (this.itemNbt.getCompound("inheritFireworkNBT").getCompound("Fireworks").contains("Explosions", NbtElement.LIST_TYPE)) {
				syncItemNbt();
				this.world.sendEntityStatus(this, (byte)17);
				explosions = this.itemNbt.getCompound("inheritFireworkNBT").getCompound("Fireworks").getList("Explosions", NbtElement.COMPOUND_TYPE).copy();
				removeFireworkRocket();
			}
		}

		if (!this.world.isClient && gameFlags.indexOf(NbtString.of("inheritFireworkStarNBT")) >= 0 && this.itemNbt.contains("inheritFireworkStarNBT", NbtElement.COMPOUND_TYPE) && this.itemNbt.getCompound("inheritFireworkStarNBT").contains("Explosion", NbtElement.COMPOUND_TYPE)) {
			syncItemNbt();
			this.world.sendEntityStatus(this, (byte)17);
			this.discard();
			explosions.add(this.itemNbt.getCompound("inheritFireworkStarNBT").getCompound("Explosion"));
		}

		if (!explosions.isEmpty()) {
			explodeFirework(explosions);
		}
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
				//-10.32896+10.61645*x
				burnTime = -10.32896 + 10.61645 * shiftedFireHits;
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

		afterBlockOrEntityHit();
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
		
		this.world.sendEntityStatus(this, (byte)1);
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

	@Override
	public void handleStatus(byte status) {
		//Firework rocket/star impact explosion
		if (status == 17 && this.world.isClient) {
			syncItemNbt();
			//If there is no firework nbt it should return an empty list
			NbtList explosions = this.itemNbt.getCompound("inheritFireworkNBT").getCompound("Fireworks").getList("Explosions", NbtElement.COMPOUND_TYPE).copy();
			if (!this.itemNbt.getCompound("inheritFireworkStarNBT").getCompound("Explosion").isEmpty()) {
				explosions.add(this.itemNbt.getCompound("inheritFireworkStarNBT").getCompound("Explosion").copy());
			}
			MainMod.LOGGER.info(explosions.asString());

			explodeFirework(explosions);
			hasFirework = false;
		}
		//Firework rocket in air explosion
		if (status == 18 && this.world.isClient) {
			syncItemNbt();
			explodeFirework(this.itemNbt.getCompound("inheritFireworkNBT").getCompound("Fireworks").getList("Explosions", NbtElement.COMPOUND_TYPE).copy());
			hasFirework = false;
		}

		//Refresh client itemNbt
		if (status == 1) {
			if (this.world.isClient) {
				//Nbt will be updated next tick
				clientHasNbt = false;
			} else {
				if (itemNbt != null) {
					this.dataTracker.set(ITEM_NBT, itemNbt.copy());
				} else {
					this.dataTracker.set(ITEM_NBT, new NbtCompound());
				}
			}
		}

		super.handleStatus(status);
	}

	protected void initDataTracker() {
		super.initDataTracker();
		this.dataTracker.startTracking(ITEM_NBT, new NbtCompound());
		this.dataTracker.startTracking(CLIENT_SOURCE_POS, new BlockPos(0, 0, 0));
		this.dataTracker.startTracking(SHOW_FIREWORK_SPARKS, false);
	}

	public void syncItemNbt() {
		//Make the client load item nbt
		//This is fine because I don't intend to modify this data
		//Well, I might modify it to remove certain stats in one specific scenario, but it wouldn't really impact anything client side
		if (this.world.isClient && (itemNbt.isEmpty() || !clientHasNbt) && !this.dataTracker.get(ITEM_NBT).isEmpty()) {
			initFromNbt(this.dataTracker.get(ITEM_NBT));
			clientHasNbt = true;
		} else if (!this.world.isClient && this.dataTracker.get(ITEM_NBT).isEmpty() && !itemNbt.isEmpty()) {
			this.dataTracker.set(ITEM_NBT, itemNbt.copy());
		}
	}
}