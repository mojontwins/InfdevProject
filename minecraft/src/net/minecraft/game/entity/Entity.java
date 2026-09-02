package net.minecraft.game.entity;

import com.mojang.nbt.NBTTagCompound;
import com.mojang.nbt.NBTTagDouble;
import com.mojang.nbt.NBTTagFloat;
import com.mojang.nbt.NBTTagList;
import java.util.List;
import java.util.Random;
import net.minecraft.game.entity.misc.EntityItem;
import net.minecraft.game.entity.player.EntityPlayer;
import net.minecraft.game.item.ItemStack;
import net.minecraft.game.physics.AxisAlignedBB;
import net.minecraft.game.world.World;
import net.minecraft.game.world.block.Block;
import net.minecraft.game.world.block.StepSound;
import net.minecraft.game.world.material.Material;
import util.MathHelper;

/**
 * The base of every object with a position, velocity, bounding box and a life
 * of its own: it owns the shared per-tick pass ({@link #onUpdate()}) and the
 * axis-by-axis movement/collision solver ({@link #moveEntity}), the water and
 * lava tests, fire and falling, and the NBT round-trip every saved entity goes
 * through.
 *
 * <p>Subclasses set their shape once in the constructor with
 * {@link #setSize} and extend the hook points ({@link #onUpdate()}, or
 * {@link #fall} / {@link #dealFireDamage} / {@link #attackEntityFrom} / the
 * NBT read/write pair). Fields are public because the world ticker and the
 * client renderers read them directly.
 */
public abstract class Entity {
	/** This entity blocks others from spawning inside it while it is pushed along. */
	public boolean preventEntitySpawning = false;
	protected World worldObj;

	// --- position & look -------------------------------------------------------
	public double prevPosX;
	public double prevPosY;
	public double prevPosZ;
	public double posX;
	public double posY;
	public double posZ;
	public double motionX;
	public double motionY;
	public double motionZ;
	public float rotationYaw;
	public float rotationPitch;
	public float prevRotationYaw;
	public float prevRotationPitch;

	// --- shape & physics state --------------------------------------------------
	/** The solid volume this entity occupies; movement logic reads and relocates it. */
	public AxisAlignedBB boundingBox = new AxisAlignedBB(0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D);
	public boolean onGround = false;
	public boolean isCollidedHorizontally = false;
	/**
	 * When true the entity keeps its horizontal velocity after landing on (or
	 * bumping into) something. Always true in this build — the collision
	 * "stop-all-motion" branches in {@link #moveEntity} are dead but kept for
	 * fidelity with the original bytecode.
	 */
	private boolean keepMovingOnCollide = true;
	public boolean isDead = false;
	/** Vertical offset from the feet to the "logical" origin, e.g. the eyes/centre of gravity. */
	public float yOffset = 0.0F;
	/** Horizontal footprint of the bounding box, in block units. */
	public float width = 0.6F;
	/** Height of the bounding box, in block units. */
	public float height = 1.8F;
	public float prevDistanceWalkedModified = 0.0F;
	public float distanceWalkedModified = 0.0F;
	/** Whether this entity paces out footsteps and plant/walk contacts over solid ground. */
	protected boolean entityWalks = true;
	/** Metres built up during a fall; applied through {@link #fall} on landing. */
	protected float fallDistance = 0.0F;
	private int nextStepDistance = 1;
	public double lastTickPosX;
	public double lastTickPosY;
	public double lastTickPosZ;
	/** Depth sunk into fluid/climb (step-ups wedge it up); subtracted from posY. */
	private float ySize = 0.0F;
	/** How high an automatic step-up may climb (0 disables stepping). */
	public float stepHeight = 0.0F;
	public boolean noClip = false;
	protected Random rand = new Random();
	public int ticksExisted = 0;
	public int fireResistance = 1;
	/** Remaining fire ticks; becomes negative while fire is being resisted/extinguished. */
	public int fire = 0;
	protected int maxAir = 300;
	private boolean inWater = false;
	/** Ticks the red "hearts" flash stays visible after harm or healing. */
	public int heartsLife = 0;
	public int air = 300;
	private boolean isFirstUpdate = true;
	public String skinUrl;
	/** True when this entity has been placed into a chunk's entity list via {@link Chunk#addEntity}. */
	public boolean addedToChunk = false;
	/** The chunk coordinates this entity currently belongs to (set by {@link Chunk#addEntity} and updated on every chunk migration). */
	public int chunkCoordX;
	public int chunkCoordY;
	public int chunkCoordZ;

	public Entity(World world) {
		this.worldObj = world;
		this.setPosition(0.0D, 0.0D, 0.0D);
	}

	/** Walks the player up through any solid ground it got spawned inside. */
	protected void preparePlayerToSpawn() {
		if(this.worldObj != null) {
			while(this.posY > 0.0D) {
				this.setPosition(this.posX, this.posY, this.posZ);
				if(this.worldObj.getCollidingBoundingBoxes(this.boundingBox).size() == 0) {
					break;
				}

				++this.posY;
			}

			this.motionX = this.motionY = this.motionZ = 0.0D;
			this.rotationPitch = 0.0F;
		}
	}

	/** Records the entity's footprint and height; the box is recentred the next time it is positioned. */
	protected void setSize(float width, float height) {
		this.width = width;
		this.height = height;
	}

	/** Recentres the bounding box (and the entity) exactly on the given coordinates. */
	protected final void setPosition(double x, double y, double z) {
		this.posX = x;
		this.posY = y;
		this.posZ = z;
		float halfWidth = this.width / 2.0F;
		float halfHeight = this.height / 2.0F;
		AxisAlignedBB box = this.boundingBox;
		box.minX = x - (double)halfWidth;
		box.minY = y - (double)halfHeight;
		box.minZ = z - (double)halfWidth;
		box.maxX = x + (double)halfWidth;
		box.maxY = y + (double)halfHeight;
		box.maxZ = z + (double)halfWidth;
	}

	public void onUpdate() {
		++this.ticksExisted;
		this.prevDistanceWalkedModified = this.distanceWalkedModified;
		this.prevPosX = this.posX;
		this.prevPosY = this.posY;
		this.prevPosZ = this.posZ;
		this.prevRotationPitch = this.rotationPitch;
		this.prevRotationYaw = this.rotationYaw;
		if(this.handleWaterMovement()) {
			// Slapping into water for the first time: a splash, a bubbly trail,
			// and any accumulated fall is forgotten.
			if(!this.inWater && !this.isFirstUpdate) {
				float splashVolume = MathHelper.sqrt_double(this.motionX * this.motionX * (double)0.2F + this.motionY * this.motionY + this.motionZ * this.motionZ * (double)0.2F) * 0.2F;
				if(splashVolume > 1.0F) {
					splashVolume = 1.0F;
				}

				this.worldObj.playSoundAtEntity(this, "random.splash", splashVolume, 1.0F + (this.rand.nextFloat() - this.rand.nextFloat()) * 0.4F);
				float waterLevel = (float)MathHelper.floor_double(this.boundingBox.minY);

				for(int particle = 0; (float)particle < 1.0F + this.width * 20.0F; ++particle) {
					float spreadX = (this.rand.nextFloat() * 2.0F - 1.0F) * this.width;
					float spreadZ = (this.rand.nextFloat() * 2.0F - 1.0F) * this.width;
					this.worldObj.spawnParticle("bubble", this.posX + (double)spreadX, (double)(waterLevel + 1.0F), this.posZ + (double)spreadZ, this.motionX, this.motionY - (double)(this.rand.nextFloat() * 0.2F), this.motionZ);
				}

				for(int particle = 0; (float)particle < 1.0F + this.width * 20.0F; ++particle) {
					float spreadX = (this.rand.nextFloat() * 2.0F - 1.0F) * this.width;
					float spreadZ = (this.rand.nextFloat() * 2.0F - 1.0F) * this.width;
					this.worldObj.spawnParticle("splash", this.posX + (double)spreadX, (double)(waterLevel + 1.0F), this.posZ + (double)spreadZ, this.motionX, this.motionY, this.motionZ);
				}
			}

			this.fallDistance = 0.0F;
			this.inWater = true;
			this.fire = 0;
		} else {
			this.inWater = false;
		}

		// Burning: one fire tick deals a heart of damage every 20 fires ticks.
		if(this.fire > 0) {
			if(this.fire % 20 == 0) {
				this.attackEntityFrom((Entity)null, 1);
			}

			--this.fire;
		}

		if(this.handleLavaMovement()) {
			this.attackEntityFrom((Entity)null, 10);
			this.fire = 600;
		}

		this.isFirstUpdate = false;
	}

	/** False if the given offset would leave the entity overlapping a solid block (or in fluid). */
	public final boolean isOffsetPositionInLiquid(double xOffset, double yOffset, double zOffset) {
		AxisAlignedBB offsetBox = this.boundingBox.offsetCopy(xOffset, yOffset, zOffset);
		List<AxisAlignedBB> colliding = this.worldObj.getCollidingBoundingBoxes(offsetBox);
		return colliding.size() > 0 ? false : !this.worldObj.getIsAnyLiquid(offsetBox);
	}

	/**
	 * The one shared movement solver. Each axis of the requested
	 * (motionX, motionY, motionZ) is clipped against every collider it would
	 * cross, the survivors are applied and the position is recomputed from the
	 * resulting box. Also handles floor contact (fall damage, stop velocity),
	 * fires on contact, footsteps, and — for walkers — automatic single-step
	 * climbing. Hottest path in the game: deliberately kept on plain loops.
	 */
	public final void moveEntity(double motionX, double motionY, double motionZ) {
		if(this.noClip) {
			this.boundingBox.offset(motionX, motionY, motionZ);
			this.posX = (this.boundingBox.minX + this.boundingBox.maxX) / 2.0D;
			this.posY = this.boundingBox.minY + (double)this.yOffset - (double)this.ySize;
			this.posZ = (this.boundingBox.minZ + this.boundingBox.maxZ) / 2.0D;
		} else {
			double startX = this.posX;
			double startZ = this.posZ;
			double wantedX = motionX;
			double wantedY = motionY;
			double wantedZ = motionZ;
			AxisAlignedBB startBox = this.boundingBox.copy();

			// Sneaking on solid ground: refuse to step over a ledge. Each axis
			// is eased back toward zero (in 0.05 steps) while the cell it would
			// move into has no floor beneath it, so the player skids to a stop
			// instead of walking off the edge.
			boolean groundedSneak = this.onGround && this.isSneaking();
			if(groundedSneak) {
				double sneakStep = 0.05D;
				for(; motionX != 0.0D && this.worldObj.getCollidingBoundingBoxes(this.boundingBox.offsetCopy(motionX, -1.0D, 0.0D)).size() == 0; wantedX = motionX) {
					if(motionX < sneakStep && motionX >= -sneakStep) {
						motionX = 0.0D;
					} else if(motionX > 0.0D) {
						motionX -= sneakStep;
					} else {
						motionX += sneakStep;
					}
				}

				for(; motionZ != 0.0D && this.worldObj.getCollidingBoundingBoxes(this.boundingBox.offsetCopy(0.0D, -1.0D, motionZ)).size() == 0; wantedZ = motionZ) {
					if(motionZ < sneakStep && motionZ >= -sneakStep) {
						motionZ = 0.0D;
					} else if(motionZ > 0.0D) {
						motionZ -= sneakStep;
					} else {
						motionZ += sneakStep;
					}
				}
			}

			List<AxisAlignedBB> colliders = this.worldObj.getCollidingBoundingBoxes(this.boundingBox.addCoord(motionX, motionY, motionZ));

			// Clip vertically first so the box is on solid ground before the sides.
			for(AxisAlignedBB collider : colliders) {
				motionY = collider.calculateYOffset(this.boundingBox, motionY);
			}

			this.boundingBox.offset(0.0D, motionY, 0.0D);
			if(!this.keepMovingOnCollide && wantedY != motionY) {
				motionZ = 0.0D;
				motionY = 0.0D;
				motionX = 0.0D;
			}

			boolean touchDown = this.onGround || wantedY != motionY && wantedY < 0.0D;

			for(AxisAlignedBB collider : colliders) {
				motionX = collider.calculateXOffset(this.boundingBox, motionX);
			}

			this.boundingBox.offset(motionX, 0.0D, 0.0D);
			if(!this.keepMovingOnCollide && wantedX != motionX) {
				motionZ = 0.0D;
				motionY = 0.0D;
				motionX = 0.0D;
			}

			for(AxisAlignedBB collider : colliders) {
				motionZ = collider.calculateZOffset(this.boundingBox, motionZ);
			}

			this.boundingBox.offset(0.0D, 0.0D, motionZ);
			if(!this.keepMovingOnCollide && wantedZ != motionZ) {
				motionZ = 0.0D;
				motionY = 0.0D;
				motionX = 0.0D;
			}

			double sideResultX;
			double sideResultY;
			double sideResultZ;
			// Step-up: a walker blocked horizontally while grounded replays the same
			// move from the original box raised by one stepHeight, and keeps the
			// steppy result only if it actually advanced further sideways.
			if(this.stepHeight > 0.0F && touchDown && this.ySize < 0.05F && (wantedX != motionX || wantedZ != motionZ)) {
				sideResultX = motionX;
				sideResultY = motionY;
				sideResultZ = motionZ;
				motionX = wantedX;
				motionY = (double)this.stepHeight;
				motionZ = wantedZ;
				AxisAlignedBB steppedBox = this.boundingBox.copy();
				this.boundingBox = startBox.copy();
				colliders = this.worldObj.getCollidingBoundingBoxes(this.boundingBox.addCoord(wantedX, motionY, wantedZ));

				for(AxisAlignedBB collider : colliders) {
					motionY = collider.calculateYOffset(this.boundingBox, motionY);
				}

				this.boundingBox.offset(0.0D, motionY, 0.0D);
				if(!this.keepMovingOnCollide && wantedY != motionY) {
					motionZ = 0.0D;
					motionY = 0.0D;
					motionX = 0.0D;
				}

				for(AxisAlignedBB collider : colliders) {
					motionX = collider.calculateXOffset(this.boundingBox, motionX);
				}

				this.boundingBox.offset(motionX, 0.0D, 0.0D);
				if(!this.keepMovingOnCollide && wantedX != motionX) {
					motionZ = 0.0D;
					motionY = 0.0D;
					motionX = 0.0D;
				}

				for(AxisAlignedBB collider : colliders) {
					motionZ = collider.calculateZOffset(this.boundingBox, motionZ);
				}

				this.boundingBox.offset(0.0D, 0.0D, motionZ);
				if(!this.keepMovingOnCollide && wantedZ != motionZ) {
					motionZ = 0.0D;
					motionY = 0.0D;
					motionX = 0.0D;
				}

				// The step path won; otherwise fall back to the side-hit result.
				if(sideResultX * sideResultX + sideResultZ * sideResultZ >= motionX * motionX + motionZ * motionZ) {
					motionX = sideResultX;
					motionY = sideResultY;
					motionZ = sideResultZ;
					this.boundingBox = steppedBox.copy();
				} else {
					this.ySize = (float)((double)this.ySize + 0.5D);
				}
			}

			this.posX = (this.boundingBox.minX + this.boundingBox.maxX) / 2.0D;
			this.posY = this.boundingBox.minY + (double)this.yOffset - (double)this.ySize;
			this.posZ = (this.boundingBox.minZ + this.boundingBox.maxZ) / 2.0D;
			this.isCollidedHorizontally = wantedX != motionX || wantedZ != motionZ;
			this.onGround = wantedY != motionY && wantedY < 0.0D;
			if(this.onGround) {
				if(this.fallDistance > 0.0F) {
					this.fall(this.fallDistance);
					this.fallDistance = 0.0F;
				}
			} else if(motionY < 0.0D) {
				this.fallDistance = (float)((double)this.fallDistance - motionY);
			}

			// An axis that collided is fully stopped: kill that velocity component.
			if(wantedX != motionX) {
				this.motionX = 0.0D;
			}

			if(wantedY != motionY) {
				this.motionY = 0.0D;
			}

			if(wantedZ != motionZ) {
				this.motionZ = 0.0D;
			}

			// Accumulate horizontal metres travelled, for footstep pacing and
			// block walk/contact callbacks.
			double travelledX = this.posX - startX;
			double travelledZ = this.posZ - startZ;
			this.distanceWalkedModified = (float)((double)this.distanceWalkedModified + (double)MathHelper.sqrt_double(travelledX * travelledX + travelledZ * travelledZ) * 0.6D);
			if(this.entityWalks) {
				int footX = MathHelper.floor_double(this.posX);
				int footY = MathHelper.floor_double(this.posY - (double)0.2F - (double)this.yOffset);
				int footZ = MathHelper.floor_double(this.posZ);
				int blockId = this.worldObj.getBlockId(footX, footY, footZ);
				if(this.distanceWalkedModified > (float)this.nextStepDistance && blockId > 0) {
					++this.nextStepDistance;
					StepSound stepSound = Block.blocksList[blockId].stepSound;
					if(!Block.blocksList[blockId].blockMaterial.getIsLiquid()) {
						this.worldObj.playSoundAtEntity(this, stepSound.getStepSound(), stepSound.stepSoundVolume * 0.15F, stepSound.stepSoundPitch);
					}

					Block.blocksList[blockId].onEntityWalking(this.worldObj, footX, footY, footZ);
				}
			}

			this.ySize *= 0.4F;
			boolean inWater = this.handleWaterMovement();
			if(this.worldObj.isBoundingBoxBurning(this.boundingBox)) {
				this.dealFireDamage(1);
				if(!inWater) {
					++this.fire;
					if(this.fire == 0) {
						this.fire = 300;
					}
				}
			} else if(this.fire <= 0) {
				this.fire = -this.fireResistance;
			}

			if(inWater && this.fire > 0) {
				this.worldObj.playSoundAtEntity(this, "random.fizz", 0.7F, 1.6F + (this.rand.nextFloat() - this.rand.nextFloat()) * 0.4F);
				this.fire = -this.fireResistance;
			}

		}
	}

	/** Deals the periodic damage a burning entity takes. */
	protected void dealFireDamage(int damage) {
		this.attackEntityFrom((Entity)null, 1);
	}

	/** Called with the accumulated fall distance when the entity lands. */
	protected void fall(float distance) {
	}

	public final boolean handleWaterMovement() {
		return this.worldObj.isMaterialInBB(this.boundingBox.expand(0.0D, (double)-0.4F, 0.0D), Material.water);
	}

	/** True when the head (at eye height) is inside water. */
	public final boolean isInsideOfMaterial() {
		int blockId = this.worldObj.getBlockId(MathHelper.floor_double(this.posX), MathHelper.floor_double(this.posY + (double)this.getEyeHeight()), MathHelper.floor_double(this.posZ));
		return blockId != 0 ? Block.blocksList[blockId].blockMaterial == Material.water : false;
	}

	protected float getEyeHeight() {
		return 0.0F;
	}

	public final boolean handleLavaMovement() {
		return this.worldObj.isMaterialInBB(this.boundingBox.expand(0.0D, (double)-0.4F, 0.0D), Material.lava);
	}

	/**
	 * Mixes the strafe (left/right) and forward (run/walk) inputs into
	 * horizontal motion, rotated by the current yaw and scaled so the result
	 * stays within {@code maxSpeed}.
	 */
	public final void moveFlying(float strafe, float forward, float maxSpeed) {
		float magnitude = MathHelper.sqrt_float(strafe * strafe + forward * forward);
		if(magnitude >= 0.01F) {
			if(magnitude < 1.0F) {
				magnitude = 1.0F;
			}

			magnitude = maxSpeed / magnitude;
			strafe *= magnitude;
			forward *= magnitude;
			float sinYaw = MathHelper.sin(this.rotationYaw * (float)Math.PI / 180.0F);
			float cosYaw = MathHelper.cos(this.rotationYaw * (float)Math.PI / 180.0F);
			this.motionX += (double)(strafe * cosYaw - forward * sinYaw);
			this.motionZ += (double)(forward * cosYaw + strafe * sinYaw);
		}
	}

	/** Daylight at the entity's position; the {@code partialTick} is unused by the base implementation. */
	public float getEntityBrightness(float partialTick) {
		int blockX = MathHelper.floor_double(this.posX);
		int blockY = MathHelper.floor_double(this.posY + (double)(this.yOffset / 2.0F));
		int blockZ = MathHelper.floor_double(this.posZ);
		return this.worldObj.getBrightness(blockX, blockY, blockZ);
	}

	/** Teleport-and-align; y is offset by yOffset because the caller passes the "feet" position. */
	public final void setLocationAndAngles(double x, double y, double z, float yaw, float pitch) {
		this.prevPosX = this.posX = x;
		this.prevPosY = this.posY = y + (double)this.yOffset;
		this.prevPosZ = this.posZ = z;
		this.rotationYaw = yaw;
		this.rotationPitch = pitch;
		this.setPosition(this.posX, this.posY, this.posZ);
	}

	public final double getDistanceSqToEntity(Entity target) {
		double deltaX = this.posX - target.posX;
		double deltaY = this.posY - target.posY;
		double deltaZ = this.posZ - target.posZ;
		return deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ;
	}

	public void onCollideWithPlayer(EntityPlayer player) {
	}

	/** Gently shoves two overlapping entities apart, each a fraction of the gap. */
	public final void applyEntityCollision(Entity pushed) {
		double deltaX = pushed.posX - this.posX;
		double deltaZ = pushed.posZ - this.posZ;
		double distanceSq = deltaX * deltaX + deltaZ * deltaZ;
		if(distanceSq >= (double)0.01F) {
			distanceSq = (double)MathHelper.sqrt_double(distanceSq);
			deltaX /= distanceSq;
			deltaZ /= distanceSq;
			deltaX /= distanceSq;
			deltaZ /= distanceSq;
			deltaX *= (double)0.05F;
			deltaZ *= (double)0.05F;
			this.addVelocity(-deltaX, 0.0D, -deltaZ);
			pushed.addVelocity(deltaX, 0.0D, deltaZ);
		}

	}

	/** Adds to horizontal motion; the y component exists but is unused. */
	private void addVelocity(double deltaX, double unusedDeltaY, double deltaZ) {
		this.motionX += deltaX;
		this.motionZ += deltaZ;
	}

	public boolean attackEntityFrom(Entity attacker, int damage) {
		return false;
	}

	public boolean canBeCollidedWith() {
		return false;
	}

	public boolean canBePushed() {
		return false;
	}

	public String getEntityTexture() {
		return null;
	}

	/** Writes the entity's registry id plus full state; returns false when dead or unknown to the registry. */
	public final boolean addEntityID(NBTTagCompound tag) {
		String id = EntityList.getEntityString(this);
		if(!this.isDead && id != null) {
			tag.setString("id", id);
			this.writeToNBT(tag);
			return true;
		} else {
			return false;
		}
	}

	public final void writeToNBT(NBTTagCompound tag) {
		tag.setTag("Pos", newDoubleNBTList(new double[]{this.posX, this.posY, this.posZ}));
		tag.setTag("Motion", newDoubleNBTList(new double[]{this.motionX, this.motionY, this.motionZ}));
		NBTTagList rotationTag = new NBTTagList();

		for(float angle : new float[]{this.rotationYaw, this.rotationPitch}) {
			rotationTag.setTag(new NBTTagFloat(angle));
		}

		tag.setTag("Rotation", rotationTag);
		tag.setFloat("FallDistance", this.fallDistance);
		tag.setShort("Fire", (short)this.fire);
		tag.setShort("Air", (short)this.air);
		this.writeEntityToNBT(tag);
	}

	public final void readFromNBT(NBTTagCompound tag) {
		NBTTagList posTag = tag.getTagList("Pos");
		NBTTagList motionTag = tag.getTagList("Motion");
		NBTTagList rotationTag = tag.getTagList("Rotation");
		this.prevPosX = this.lastTickPosX = this.posX = posTag.tagCount() > 0 ? ((NBTTagDouble)posTag.tagAt(0)).doubleValue : 0.0D;
		this.prevPosY = this.lastTickPosY = this.posY = posTag.tagCount() > 1 ? ((NBTTagDouble)posTag.tagAt(1)).doubleValue : 0.0D;
		this.prevPosZ = this.lastTickPosZ = this.posZ = posTag.tagCount() > 2 ? ((NBTTagDouble)posTag.tagAt(2)).doubleValue : 0.0D;
		this.motionX = motionTag.tagCount() > 0 ? ((NBTTagDouble)motionTag.tagAt(0)).doubleValue : 0.0D;
		this.motionY = motionTag.tagCount() > 1 ? ((NBTTagDouble)motionTag.tagAt(1)).doubleValue : 0.0D;
		this.motionZ = motionTag.tagCount() > 2 ? ((NBTTagDouble)motionTag.tagAt(2)).doubleValue : 0.0D;
		this.prevRotationYaw = this.rotationYaw = rotationTag.tagCount() > 0 ? ((NBTTagFloat)rotationTag.tagAt(0)).floatValue : 0.0F;
		this.prevRotationPitch = this.rotationPitch = rotationTag.tagCount() > 1 ? ((NBTTagFloat)rotationTag.tagAt(1)).floatValue : 0.0F;
		this.fallDistance = tag.getFloat("FallDistance");
		this.fire = tag.getShort("Fire");
		this.air = tag.getShort("Air");
		this.setLocationAndAngles(this.posX, this.posY, this.posZ, this.rotationYaw, this.rotationPitch);
		this.readEntityFromNBT(tag);
	}

	protected abstract void readEntityFromNBT(NBTTagCompound tag);

	protected abstract void writeEntityToNBT(NBTTagCompound tag);

	private static NBTTagList newDoubleNBTList(double... values) {
		NBTTagList tag = new NBTTagList();

		for(double value : values) {
			tag.setTag(new NBTTagDouble(value));
		}

		return tag;
	}

	/** Drops one of the item at the entity's feet (the amount argument is ignored — always a single item). */
	public final EntityItem dropItemWithOffset(int itemID, int amount) {
		return this.entityDropItem(itemID, 1, 0.0F);
	}

	/** Drops a pickable item entity at the entity's feet, nudged up by {@code offsetY}. */
	public final EntityItem entityDropItem(int itemID, int count, float offsetY) {
		EntityItem itemEntity = new EntityItem(this.worldObj, this.posX, this.posY + (double)offsetY, this.posZ, new ItemStack(itemID, count));
		itemEntity.delayBeforeCanPickup = 10;
		this.worldObj.spawnEntityInWorld(itemEntity);
		return itemEntity;
	}

	public boolean isEntityAlive() {
		return !this.isDead;
	}

	/** True while the entity ducks into a sneaking crouch (the player only); the base never sneaks. */
	public boolean isSneaking() {
		return false;
	}
}