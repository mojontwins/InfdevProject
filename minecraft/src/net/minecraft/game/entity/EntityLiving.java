package net.minecraft.game.entity;

import com.mojang.nbt.NBTTagCompound;
import com.mojang.nbt.NBTTagList;
import java.util.List;
import net.minecraft.game.entity.misc.EntityItem;
import net.minecraft.game.item.Item;
import net.minecraft.game.item.ItemArmor;
import net.minecraft.game.item.ItemStack;
import net.minecraft.game.world.World;
import net.minecraft.game.world.block.Block;
import net.minecraft.game.world.block.StepSound;
import util.MathHelper;

/**
 * Any entity with a health pool, a model that swings its limbs and an ambient
 * life of its own: wandering, sound cues, drowning, knock-back and death.
 *
 * <p>The tick flow is fixed in {@link #onUpdate()}: first the shared per-tick
 * bookkeeping (sounds, drowning, timer decay, death), then the teleport-safe
 * {@link #onLivingUpdate()} that lets subclasses drive their own movement, and
 * finally the body animation — the model's yaw lagging behind the true heading
 * and the limb swing winding up with distance travelled.
 */
public class EntityLiving extends Entity {
	/** How many ticks the red "hearts" flash stays visible after the last damage or heal. */
	public int heartsHalvesLife = 20;
	/** Yaw the model is drawn at; lags behind {@code rotationYaw} so the torso twists instead of snapping. */
	public float renderYawOffset = 0.0F;
	public float prevRenderYawOffset = 0.0F;
	/** Path to the skin/entity texture. */
	protected String texture = "/char.png";
	public int health;
	public int prevHealth;
	/** Decreasing countdown before the next ambient sound; negative values delay it further. */
	private int livingSoundTime;
	/** Remaining ticks of the red hurt flash (drives the tint in the renderers). */
	public int hurtTime;
	/** The value {@code hurtTime} is reset to each time the creature is hit. */
	public int maxHurtTime;
	/** Yaw the last blow came from, used to spin the corpse when it dies. */
	public float attackedAtYaw = 0.0F;
	/** Ticks the creature has already spent dying; past 20 it is removed. */
	public int deathTime = 0;
	/** Attack cooldown in ticks before this creature may strike again. */
	public int attackTime = 0;
	public float prevCameraPitch;
	public float cameraPitch;
	/** Last tick's limb swing, kept so renderers can interpolate between frames. */
	public float prevLimbSwing;
	/** How far the legs have swung, wound up by horizontal speed (capped at 1). */
	public float limbSwing;
	/** Persistently growing limb travel, fed to the model as the swing pitch. */
	public float limbSwingPitch;
	/** Ticks since this creature came into being; also the far-distance despawn timer. */
	protected int entityAge;
	protected float moveStrafing;
	protected float moveForward;
	/** Random turn rate injected while idling. */
	private float randomYawVelocity;
	protected boolean isJumping;
	protected float moveSpeed;

	/** Armour worn on the four body slots (index 0 = boots, 3 = helmet; see {@link ItemArmor#armorType}). */
	protected ItemStack[] armorInventory = new ItemStack[4];
	/** The part of a blow the armour hopper could not divide evenly; banked for the next {@link #attackEntityFrom}. */
	protected int armorDamageCarryover = 0;

	public EntityLiving(World world) {
		super(world);
		// Keep the unused Math.random() draws: they advance the shared RNG stream
		// exactly like the original, so every later random value stays in sync.
		Math.random();
		this.entityAge = 0;
		this.isJumping = false;
		this.moveSpeed = 0.7F;
		this.health = 10;
		this.preventEntitySpawning = true;
		Math.random();
		this.setPosition(this.posX, this.posY, this.posZ);
		Math.random();
		this.rotationYaw = (float)(Math.random() * (double)((float)Math.PI) * 2.0D);
		this.stepHeight = 0.5F;
	}

	public final String getEntityTexture() {
		return this.texture;
	}

	public final boolean canBeCollidedWith() {
		return !this.isDead;
	}

	public final boolean canBePushed() {
		return !this.isDead;
	}

	protected float getEyeHeight() {
		return this.height * 0.85F;
	}

	@Override
	public void onUpdate() {
		super.onUpdate();
		if(this.rand.nextInt(1000) < this.livingSoundTime++) {
			this.livingSoundTime = -80;
			String sound = this.getLivingSound();
			if(sound != null) {
				this.playSound(sound);
			}
		}

		// Drowning: the head is inside water, so run down the air supply and,
		// at its exhaustion, surface an 8-bubble burst and deal 2 damage.
		if(this.isEntityAlive() && this.isInsideOfMaterial()) {
			--this.air;
			if(this.air == -20) {
				this.air = 0;

				for(int bubble = 0; bubble < 8; ++bubble) {
					float spawnX = this.rand.nextFloat() - this.rand.nextFloat();
					float spawnY = this.rand.nextFloat() - this.rand.nextFloat();
					float spawnZ = this.rand.nextFloat() - this.rand.nextFloat();
					this.worldObj.spawnParticle("bubble", this.posX + (double)spawnX, this.posY + (double)spawnY, this.posZ + (double)spawnZ, this.motionX, this.motionY, this.motionZ);
				}

				this.attackEntityFrom((Entity)null, 2);
			}

			this.fire = 0;
		} else {
			this.air = this.maxAir;
		}

		this.prevCameraPitch = this.cameraPitch;
		if(this.attackTime > 0) {
			--this.attackTime;
		}

		if(this.hurtTime > 0) {
			--this.hurtTime;
		}

		if(this.heartsLife > 0) {
			--this.heartsLife;
		}

		if(this.health <= 0) {
			++this.deathTime;
			if(this.deathTime > 20) {
				this.isDead = true;
			}
		}

		this.prevRenderYawOffset = this.renderYawOffset;
		this.prevRotationYaw = this.rotationYaw;
		this.prevRotationPitch = this.rotationPitch;
		this.onLivingUpdate();

		// Body animation. The torso yaw is eased toward either the direction the
		// creature is travelling, or its last heading, so the body follows the
		// head instead of snapping around.
		double travelledX = this.posX - this.prevPosX;
		double travelledZ = this.posZ - this.prevPosZ;
		float distanceTravelled = MathHelper.sqrt_double(travelledX * travelledX + travelledZ * travelledZ);
		float travelYaw = this.renderYawOffset;
		if(distanceTravelled > 0.05F) {
			travelYaw = (float)Math.atan2(travelledZ, travelledX) * 180.0F / (float)Math.PI - 90.0F;
		}

		float yawTwist = wrapAngleTo180(travelYaw - this.renderYawOffset);
		this.renderYawOffset += yawTwist * 0.1F;
		float headTurn = wrapAngleTo180(this.rotationYaw - this.renderYawOffset);
		if(headTurn < -75.0F) {
			headTurn = -75.0F;
		}

		if(headTurn >= 75.0F) {
			headTurn = 75.0F;
		}

		this.renderYawOffset = this.rotationYaw - headTurn;
		this.renderYawOffset += headTurn * 0.1F;

		// Keep every interpolated angle on the same side of a 360° wrap so the
		// renderers never sweep a model the long way around a full turn.
		this.unwrapPrevAngle(this.rotationYaw, this.prevRotationYaw, false);
		this.unwrapPrevAngle(this.renderYawOffset, this.prevRenderYawOffset, false);
		this.unwrapPrevAngle(this.rotationPitch, this.prevRotationPitch, true);
	}

	/** Plays a sound with the usual ambient volume and a small random pitch jitter. */
	private void playSound(String sound) {
		this.worldObj.playSoundAtEntity(this, sound, 1.0F, (this.rand.nextFloat() - this.rand.nextFloat()) * 0.2F + 1.0F);
	}

	/**
	 * Normalizes an angle into the [-180°, 180°) range by crossing the 360°
	 * boundary as often as needed.
	 */
	private static float wrapAngleTo180(float angle) {
		while(angle < -180.0F) {
			angle += 360.0F;
		}

		while(angle >= 180.0F) {
			angle -= 360.0F;
		}

		return angle;
	}

	/**
	 * Rewinds {@code previous} so that {@code current - previous} stays inside
	 * [-360°, 360°), allowing a safe linear interpolation. Pairs wrap without
	 * cross-domain mixing when {@code zeroCentered} is set (used for pitch,
	 * which is already centered on 0).
	 */
	private void unwrapPrevAngle(float current, float previous, boolean zeroCentered) {
		while(current - previous < -180.0F) {
			previous -= 360.0F;
		}

		while(current - previous >= 180.0F) {
			previous += 360.0F;
		}
	}

	public final void heal(int amount) {
		if(this.health > 0) {
			this.health += amount;
			if(this.health > 20) {
				this.health = 20;
			}

			this.heartsLife = this.heartsHalvesLife / 2;
		}
	}

	/** The armour piece worn in the given slot (0 = boots, 3 = helmet), or null. */
	public ItemStack getArmorInSlot(int slot) {
		return this.armorInventory[slot];
	}

	public void setArmorInSlot(int slot, ItemStack stack) {
		this.armorInventory[slot] = stack;
	}

	/**
	 * The total armour rating of the worn set, attenuated as the pieces wear
	 * down: a pristine leather set is worth 4, a set about to fall apart may
	 * only count for 1.
	 */
	protected int getTotalArmorValue() {
		int damageReduceTotal = 0;
		int remainingDurability = 0;
		int totalDurability = 0;

		for (ItemStack armor : this.armorInventory) {
			if (armor != null && armor.getItem() instanceof ItemArmor) {
				int maxDamage = armor.getMaxDamage();
				remainingDurability += maxDamage - armor.itemDamage;
				totalDurability += maxDamage;
				damageReduceTotal += ((ItemArmor) armor.getItem()).damageReduceAmount;
			}
		}

		if (totalDurability == 0) {
			return 0;
		} else {
			return (damageReduceTotal - 1) * remainingDurability / totalDurability + 1;
		}
	}

	/** Wears every worn armour piece by one blow's worth of damage, dropping pieces that break. */
	protected void damageArmor(int damage) {
		for (int slot = 0; slot < this.armorInventory.length; ++slot) {
			ItemStack armorStack = this.armorInventory[slot];
			if (armorStack != null && armorStack.getItem() instanceof ItemArmor) {
				armorStack.damageItem(damage);
				if (armorStack.stackSize == 0) {
					this.armorInventory[slot] = null;
				}
			}
		}
	}

	/**
	 * The 25-point armour hopper: every point of {@link #getTotalArmorValue()}
	 * widens the swing by one, and whatever does not divide evenly is banked in
	 * {@link #armorDamageCarryover} for the next blow. Environmental harm
	 * (drowning, fire, falls — no attacker) skips armour entirely.
	 */
	protected int applyArmorCalculations(Entity attacker, int damage) {
		if (attacker != null) {
			int hopper = 25 - this.getTotalArmorValue();
			int hopperTotal = damage * hopper + this.armorDamageCarryover;
			this.damageArmor(damage);
			damage = hopperTotal / 25;
			this.armorDamageCarryover = hopperTotal % 25;
		}

		return damage;
	}

	/**
	 * Applies {@code damage} from {@code attacker}. While the hearts flash is
	 * still shown the blow lands at a fraction of its strength; otherwise the
	 * full amount is absorbed, the hurt timer starts and the victim is pushed
	 * away from the attacker. Returns true if the blow had any effect.
	 */
	public boolean attackEntityFrom(Entity attacker, int damage) {
		this.entityAge = 0;
		if(this.health <= 0) {
			return false;
		}

		this.limbSwing = 1.5F;
		damage = this.applyArmorCalculations(attacker, damage);
		if((float)this.heartsLife > (float)this.heartsHalvesLife / 2.0F) {
			if(this.prevHealth - damage >= this.health) {
				return false;
			}

			this.health = this.prevHealth - damage;
		} else {
			this.prevHealth = this.health;
			this.heartsLife = this.heartsHalvesLife;
			this.health -= damage;
			this.hurtTime = this.maxHurtTime = 10;
		}

		this.attackedAtYaw = 0.0F;
		if(attacker != null) {
			double deltaX = attacker.posX - this.posX;
			double deltaZ = attacker.posZ - this.posZ;
			this.attackedAtYaw = (float)(Math.atan2(deltaZ, deltaX) * 180.0D / (double)((float)Math.PI)) - this.rotationYaw;
			float attackDistance = MathHelper.sqrt_double(deltaX * deltaX + deltaZ * deltaZ);
			this.motionX /= 2.0D;
			this.motionY /= 2.0D;
			this.motionZ /= 2.0D;
			this.motionX -= deltaX / (double)attackDistance * (double)0.4F;
			this.motionY += (double)0.4F;
			this.motionZ -= deltaZ / (double)attackDistance * (double)0.4F;
			if(this.motionY > (double)0.4F) {
				this.motionY = (double)0.4F;
			}
		} else {
			this.attackedAtYaw = (float)((int)(Math.random() * 2.0D) * 180);
		}

		if(this.health <= 0) {
			this.playSound(this.getDeathSound());
			this.onDeath(attacker);
		} else {
			this.playSound(this.getHurtSound());
		}

		return true;
	}

	protected String getLivingSound() {
		return null;
	}

	protected String getHurtSound() {
		return "random.hurt";
	}

	protected String getDeathSound() {
		return "random.hurt";
	}

	/** Scatters the creature's dropped item a random number of times (0-2), and sheds any worn armour. */
	public void onDeath(Entity killer) {
		int droppedItem = this.getDroppedItem();
		if(droppedItem > 0) {
			int dropCount = this.rand.nextInt(3);

			for(int drop = 0; drop < dropCount; ++drop) {
				this.dropItemWithOffset(droppedItem, 1);
			}
		}

		// Worn armour falls off the corpse, keeping the damage it had taken.
		for(int slot = 0; slot < this.armorInventory.length; ++slot) {
			ItemStack wornArmor = this.armorInventory[slot];
			if(wornArmor != null) {
				this.worldObj.spawnEntityInWorld(new EntityItem(this.worldObj, this.posX, this.posY + (double)0.3F, this.posZ, wornArmor));
				this.armorInventory[slot] = null;
			}
		}

	}

	protected int getDroppedItem() {
		return 0;
	}

	/** The five armour materials, each listing helmet, plate, leggings and boots (see {@link ItemArmor#armorType}). */
	private static final Item[][] armorMaterialSets = new Item[][]{
		{Item.helmetLeather, Item.plateLeather, Item.legsLeather, Item.bootsLeather},
		{Item.helmetChain, Item.plateChain, Item.legsChain, Item.bootsChain},
		{Item.helmetSteel, Item.plateSteel, Item.legsSteel, Item.bootsSteel},
		{Item.helmetDiamond, Item.plateDiamond, Item.legsDiamond, Item.bootsDiamond},
		{Item.helmetGold, Item.plateGold, Item.legsGold, Item.bootsGold}
	};

	/**
	 * Whether this creature can spawn already wearing random armour. Off by
	 * default; the MobSpawner only rolls armour through {@link #addRandomArmor()}
	 * when this returns true, so subclasses may enable it for their kind only.
	 */
	public boolean mightSpawnArmored() {
		return false;
	}

	/**
	 * Bestows random pieces of armour, scaled by the difficulty and weighted
	 * toward the cheap sets, on a creature as it spawns. The MobSpawner calls
	 * this only when {@link #mightSpawnArmored()} allows it.
	 */
	public void addRandomArmor() {
		int pieceCount = this.rand.nextInt(2 + this.worldObj.difficultySetting * 2);

		for(int piece = 0; piece < pieceCount; ++piece) {
			ItemStack armorPiece = this.randomArmorPiece();
			this.setArmorInSlot(3 - ((ItemArmor)armorPiece.getItem()).armorType, armorPiece);
		}

	}

	/** Builds one random piece: a random armour slot and a material drawn from the weighted table. */
	private ItemStack randomArmorPiece() {
		int armorType = this.rand.nextInt(4);
		int materialRoll = this.rand.nextInt(10);
		int material;
		if(materialRoll < 4) {
			// leather — the common early-game set.
			material = 0;
		} else if(materialRoll < 6) {
			material = 1;
		} else if(materialRoll < 8) {
			material = 2;
		} else if(materialRoll < 9) {
			material = 4;
		} else {
			material = 3;
		}

		return new ItemStack(armorMaterialSets[material][armorType]);
	}

	/** Counts fall damage for every block past 3, and reports the landing block's step sound. */
	protected final void fall(float distance) {
		int fallLevel = (int)Math.ceil((double)(distance - 3.0F));
		if(fallLevel > 0) {
			this.attackEntityFrom((Entity)null, fallLevel);
			int landingBlockId = this.worldObj.getBlockId(MathHelper.floor_double(this.posX), MathHelper.floor_double(this.posY - (double)0.2F - (double)this.yOffset), MathHelper.floor_double(this.posZ));
			if(landingBlockId > 0) {
				StepSound stepSound = Block.blocksList[landingBlockId].stepSound;
				this.worldObj.playSoundAtEntity(this, stepSound.getStepSound(), stepSound.stepSoundVolume * 0.5F, stepSound.stepSoundPitch * (12.0F / 16.0F));
			}
		}

	}

	public void writeEntityToNBT(NBTTagCompound compound) {
		compound.setShort("Health", (short)this.health);
		compound.setShort("HurtTime", (short)this.hurtTime);
		compound.setShort("DeathTime", (short)this.deathTime);
		compound.setShort("AttackTime", (short)this.attackTime);
		this.writeEquipmentToNBT(compound);
	}

	public void readEntityFromNBT(NBTTagCompound compound) {
		this.health = compound.getShort("Health");
		if(!compound.hasKey("Health")) {
			this.health = 10;
		}

		this.hurtTime = compound.getShort("HurtTime");
		this.deathTime = compound.getShort("DeathTime");
		this.attackTime = compound.getShort("AttackTime");
		this.readEquipmentFromNBT(compound);
	}

	/**
	 * Serializes the worn armour into an {@code "Armor"} list of item tags (an
	 * empty compound marks a missing piece). The player reads and writes its
	 * armour through the inventory instead and overrides this to a no-op.
	 */
	protected void writeEquipmentToNBT(NBTTagCompound compound) {
		NBTTagList armorList = new NBTTagList();

		for(ItemStack armor : this.armorInventory) {
			NBTTagCompound itemTag = new NBTTagCompound();
			if(armor != null) {
				armor.writeToNBT(itemTag);
			}

			armorList.setTag(itemTag);
		}

		compound.setTag("Armor", armorList);
	}

	protected void readEquipmentFromNBT(NBTTagCompound compound) {
		NBTTagList armorList = compound.getTagList("Armor");
		int slotCount = Math.min(this.armorInventory.length, armorList.tagCount());

		for(int slot = 0; slot < slotCount; ++slot) {
			NBTTagCompound itemTag = (NBTTagCompound)armorList.tagAt(slot);
			if(itemTag != null && itemTag.hasKey("id")) {
				this.armorInventory[slot] = new ItemStack(itemTag);
			}
		}

	}

	public final boolean isEntityAlive() {
		return !this.isDead && this.health > 0;
	}

	/**
	 * Per-tick living behaviour hook: despawns creatures that wandered too far
	 * from the player (128 blocks out, or 32+ blocks and idle too long), then
	 * either lets the subclass steer ({@link #updatePlayerActionState}) or lies
	 * still while dying. Ends with the actual movement pass.
	 */
	public void onLivingUpdate() {
		++this.entityAge;
		Entity player = this.worldObj.getPlayerEntity();
		if(player != null) {
			double deltaX = player.posX - this.posX;
			double deltaY = player.posY - this.posY;
			double deltaZ = player.posZ - this.posZ;
			double distanceSq = deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ;
			if(distanceSq > 16384.0D) {
				this.isDead = true;
			}

			if(this.entityAge > 600 && this.rand.nextInt(800) == 0) {
				if(distanceSq < 1024.0D) {
					this.entityAge = 0;
				} else {
					this.isDead = true;
				}
			}
		}

		if(this.health <= 0) {
			this.isJumping = false;
			this.moveStrafing = 0.0F;
			this.moveForward = 0.0F;
			this.randomYawVelocity = 0.0F;
		} else {
			this.updatePlayerActionState();
		}

		boolean inWater = this.handleWaterMovement();
		boolean inLava = this.handleLavaMovement();
		if(this.isJumping) {
			if(inWater) {
				this.motionY += (double)0.04F;
			} else if(inLava) {
				this.motionY += (double)0.04F;
			} else if(this.onGround) {
				this.motionY = (double)0.42F;
			}
		}

		this.moveStrafing *= 0.98F;
		this.moveForward *= 0.98F;
		this.randomYawVelocity *= 0.9F;
		float forwardInput = this.moveForward;
		float strafeInput = this.moveStrafing;
		if(inWater) {
			this.moveInFluid(0.8F);
		} else if(inLava) {
			this.moveInFluid(0.5F);
		} else {
			this.moveFlying(strafeInput, forwardInput, this.onGround ? 0.1F : 0.02F);
			this.moveEntity(this.motionX, this.motionY, this.motionZ);
			this.motionX *= (double)0.91F;
			this.motionY *= (double)0.98F;
			this.motionZ *= (double)0.91F;
			this.motionY -= 0.08D;
			if(this.onGround) {
				this.motionX *= (double)0.6F;
				this.motionZ *= (double)0.6F;
			}
		}

		this.prevLimbSwing = this.limbSwing;
		double travelledX = this.posX - this.prevPosX;
		double travelledZ = this.posZ - this.prevPosZ;
		float distanceTravelled = MathHelper.sqrt_double(travelledX * travelledX + travelledZ * travelledZ) * 4.0F;
		if(distanceTravelled > 1.0F) {
			distanceTravelled = 1.0F;
		}

		this.limbSwing += (distanceTravelled - this.limbSwing) * 0.4F;
		this.limbSwingPitch += this.limbSwing;

		List<Entity> nearbyEntities = this.worldObj.getEntitiesWithinAABBExcludingEntity(this, this.boundingBox.expand((double)0.2F, 0.0D, (double)0.2F));
		if(nearbyEntities != null) {
			nearbyEntities.forEach(entity -> {
				if(entity.canBePushed()) {
					entity.applyEntityCollision(this);
				}
			});
		}

	}

	/** Applies one tick of swimming in water ({@code damping} 0.8) or lava (0.5). */
	private void moveInFluid(float damping) {
		double startY = this.posY;
		this.moveFlying(this.moveStrafing, this.moveForward, 0.02F);
		this.moveEntity(this.motionX, this.motionY, this.motionZ);
		this.motionX *= (double)damping;
		this.motionY *= (double)damping;
		this.motionZ *= (double)damping;
		this.motionY -= 0.02D;
		if(this.isCollidedHorizontally && this.isOffsetPositionInLiquid(this.motionX, this.motionY + (double)0.6F - this.posY + startY, this.motionZ)) {
			this.motionY = (double)0.3F;
		}

	}

	/**
	 * Default creature "personality": every few ticks it picks a random strafe,
	 * pace and turn, and almost never jumps — unless it is in water or lava,
	 * where it swims restlessly.
	 */
	protected void updatePlayerActionState() {
		if(this.rand.nextFloat() < 0.07F) {
			this.moveStrafing = (this.rand.nextFloat() - 0.5F) * this.moveSpeed;
			this.moveForward = this.rand.nextFloat() * this.moveSpeed;
		}

		this.isJumping = this.rand.nextFloat() < 0.01F;
		if(this.rand.nextFloat() < 0.04F) {
			this.randomYawVelocity = (this.rand.nextFloat() - 0.5F) * 60.0F;
		}

		this.rotationYaw += this.randomYawVelocity;
		this.rotationPitch = 0.0F;
		if(this.handleWaterMovement() || this.handleLavaMovement()) {
			this.isJumping = this.rand.nextFloat() < 0.8F;
		}

	}

	/** True when the given cell is open, dry and reachable — the spawn check used by all creatures. */
	public boolean getCanSpawnHere(float x, float y, float z) {
		this.setPosition((double)x, (double)(y + this.height / 2.0F), (double)z);
		return this.worldObj.checkIfAABBIsClear1(this.boundingBox) && this.worldObj.getCollidingBoundingBoxes(this.boundingBox).size() == 0 && !this.worldObj.getIsAnyLiquid(this.boundingBox);
	}
}