package net.minecraft.game.entity.projectile;

import com.mojang.nbt.NBTTagCompound;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import net.minecraft.game.entity.Entity;
import net.minecraft.game.entity.EntityLiving;
import net.minecraft.game.entity.player.EntityPlayer;
import net.minecraft.game.item.Item;
import net.minecraft.game.item.ItemStack;
import net.minecraft.game.physics.AxisAlignedBB;
import net.minecraft.game.physics.MovingObjectPosition;
import net.minecraft.game.physics.Vec3D;
import net.minecraft.game.world.World;
import util.MathHelper;

/**
 * An arrow in flight. Each tick it advances along its velocity, tests the line
 * segment against blocks first and then against the nearest entity whose
 * (slightly inflated) box the ray crosses; whatever it hits it either sticks
 * into or bounces off of. Once it has lodged in a block the owner can walk over
 * it to collect it as an item again.
 */
public class EntityArrow extends Entity {
	private static final boolean ARROW_DEBUG = false;
	private int xTile = -1;
	private int yTile = -1;
	private int zTile = -1;
	private int inTile = 0;
	private boolean inGround = false;
	/** Frames of wobbly vibration shown by the renderer while the arrow is shaking in a block. */
	public int arrowShake = 0;
	private EntityLiving shootingEntity;
	private int ticksInGround;
	private int ticksInAir = 0;

	public EntityArrow(World world, EntityLiving shooter) {
		super(world);
		this.shootingEntity = shooter;
		this.setSize(0.5F, 0.5F);
		this.setLocationAndAngles(shooter.posX, shooter.posY, shooter.posZ, shooter.rotationYaw, shooter.rotationPitch);
		this.posX -= (double) (MathHelper.cos(this.rotationYaw / 180.0F * (float) Math.PI) * 0.16F);
		this.posY -= (double) 0.1F;
		this.posZ -= (double) (MathHelper.sin(this.rotationYaw / 180.0F * (float) Math.PI) * 0.16F);
		this.setPosition(this.posX, this.posY, this.posZ);
		this.yOffset = 0.0F;
		this.motionX = (double) (-MathHelper.sin(this.rotationYaw / 180.0F * (float) Math.PI) * MathHelper.cos(this.rotationPitch / 180.0F * (float) Math.PI));
		this.motionZ = (double) (MathHelper.cos(this.rotationYaw / 180.0F * (float) Math.PI) * MathHelper.cos(this.rotationPitch / 180.0F * (float) Math.PI));
		this.motionY = (double) (-MathHelper.sin(this.rotationPitch / 180.0F * (float) Math.PI));
		this.setArrowHeading(this.motionX, this.motionY, this.motionZ, 1.5F, 1.0F);
	}

	/**
	 * Normalises the given direction, adds a small amount of random spread to it,
	 * scales it to the given speed and points the arrow along the result.
	 */
	public final void setArrowHeading(double directionX, double directionY, double directionZ, float speed, float spread) {
		float length = MathHelper.sqrt_double(directionX * directionX + directionY * directionY + directionZ * directionZ);
		directionX /= (double) length;
		directionY /= (double) length;
		directionZ /= (double) length;
		directionX += this.rand.nextGaussian() * (double) 0.0075F * (double) spread;
		directionY += this.rand.nextGaussian() * (double) 0.0075F * (double) spread;
		directionZ += this.rand.nextGaussian() * (double) 0.0075F * (double) spread;
		directionX *= (double) speed;
		directionY *= (double) speed;
		directionZ *= (double) speed;
		this.motionX = directionX;
		this.motionY = directionY;
		this.motionZ = directionZ;
		float horizontalDistance = MathHelper.sqrt_double(directionX * directionX + directionZ * directionZ);
		this.prevRotationYaw = this.rotationYaw = (float) (Math.atan2(directionX, directionZ) * 180.0D / (double) ((float) Math.PI));
		this.prevRotationPitch = this.rotationPitch = (float) (Math.atan2(directionY, (double) horizontalDistance) * 180.0D / (double) ((float) Math.PI));
		this.ticksInGround = 0;
	}

	public final void onUpdate() {
		if(this.worldObj == null) {
			return;
		}
		super.onUpdate();
		if (this.arrowShake > 0) {
			--this.arrowShake;
		}

		if (this.inGround) {
			int blockID = this.worldObj.getBlockId(this.xTile, this.yTile, this.zTile);
			if (blockID == this.inTile) {
				++this.ticksInGround;
				if (this.ticksInGround == 1200) {
					super.isDead = true;
				}

				return;
			}

			this.inGround = false;
			this.motionX *= (double) (this.rand.nextFloat() * 0.2F);
			this.motionY *= (double) (this.rand.nextFloat() * 0.2F);
			this.motionZ *= (double) (this.rand.nextFloat() * 0.2F);
			this.ticksInGround = 0;
			this.ticksInAir = 0;
		} else {
			++this.ticksInAir;
		}

		if(ARROW_DEBUG && this.ticksInAir > 200 && this.ticksInAir % 60 == 0) {
			System.err.println("[EntityArrow] stuck in air for " + this.ticksInAir + " ticks at " + this.posX + ", " + this.posY + ", " + this.posZ);
		}

		Vec3D arrowPos = new Vec3D(this.posX, this.posY, this.posZ);
		Vec3D arrowEnd = new Vec3D(this.posX + this.motionX, this.posY + this.motionY, this.posZ + this.motionZ);
		MovingObjectPosition hit = this.worldObj.rayTraceBlocks(arrowPos, arrowEnd);
		final Vec3D rayStart = new Vec3D(this.posX, this.posY, this.posZ);
		final Vec3D rayEnd = hit != null ? new Vec3D(hit.hitVec.xCoord, hit.hitVec.yCoord, hit.hitVec.zCoord) : arrowEnd;

		// Find the closest entity whose inflated box is crossed by the flight ray;
		// the shooter only counts once the arrow has been in the air for 5 ticks.
		List<Entity> nearbyEntities = this.worldObj.getEntitiesWithinAABBExcludingEntity(this, this.boundingBox.addCoord(this.motionX, this.motionY, this.motionZ).expand(1.0D, 1.0D, 1.0D));
		EntityHit closestTarget = nearbyEntities.stream()
				.filter(entity -> entity.canBeCollidedWith() && (entity != this.shootingEntity || this.ticksInAir >= 5))
				.map(entity -> {
					MovingObjectPosition intercept = entity.boundingBox.expand(0.3F, 0.3F, 0.3F).calculateIntercept(rayStart, rayEnd);
					if (intercept == null) {
						return null;
					}
					return new EntityHit(entity, rayStart.distance(intercept.hitVec));
				})
				.filter(Objects::nonNull)
				.min(Comparator.comparingDouble(entityHit -> entityHit.distance))
				.orElse(null);

		if (closestTarget != null) {
			hit = new MovingObjectPosition(closestTarget.entity);
		}

		if (hit != null) {
			if (hit.entityHit != null) {
				// Struck a creature: either it absorbs the arrow or it bounces off.
				if (hit.entityHit.attackEntityFrom(this, 4)) {
					this.worldObj.playSoundAtEntity(this, "random.drr", 1.0F, 1.2F / (this.rand.nextFloat() * 0.2F + 0.9F));
					super.isDead = true;
				} else {
					this.motionX *= (double) -0.1F;
					this.motionY *= (double) -0.1F;
					this.motionZ *= (double) -0.1F;
					this.rotationYaw += 180.0F;
					this.prevRotationYaw += 180.0F;
					this.ticksInAir = 0;
				}
			} else {
				// Lodged into a block face: pin the arrow against the hit surface.
				this.xTile = hit.blockX;
				this.yTile = hit.blockY;
				this.zTile = hit.blockZ;
				this.inTile = this.worldObj.getBlockId(this.xTile, this.yTile, this.zTile);
				this.motionX = (double) ((float) (hit.hitVec.xCoord - this.posX));
				this.motionY = (double) ((float) (hit.hitVec.yCoord - this.posY));
				this.motionZ = (double) ((float) (hit.hitVec.zCoord - this.posZ));
				float impactDistance = MathHelper.sqrt_double(this.motionX * this.motionX + this.motionY * this.motionY + this.motionZ * this.motionZ);
				this.posX -= this.motionX / (double) impactDistance * (double) 0.05F;
				this.posY -= this.motionY / (double) impactDistance * (double) 0.05F;
				this.posZ -= this.motionZ / (double) impactDistance * (double) 0.05F;
				this.worldObj.playSoundAtEntity(this, "random.drr", 1.0F, 1.2F / (this.rand.nextFloat() * 0.2F + 0.9F));
				this.inGround = true;
				this.arrowShake = 7;
			}
		}

		this.posX += this.motionX;
		this.posY += this.motionY;
		this.posZ += this.motionZ;
		float horizontalSpeed = MathHelper.sqrt_double(this.motionX * this.motionX + this.motionZ * this.motionZ);
		this.rotationYaw = (float) (Math.atan2(this.motionX, this.motionZ) * 180.0D / (double) ((float) Math.PI));

		// Smooth the spin of the arrow by unwinding the previous angle so the
		// two never differ by more than one full turn.
		for (this.rotationPitch = (float) (Math.atan2(this.motionY, (double) horizontalSpeed) * 180.0D / (double) ((float) Math.PI)); this.rotationPitch - this.prevRotationPitch < -180.0F; this.prevRotationPitch -= 360.0F) {
		}

		while (this.rotationPitch - this.prevRotationPitch >= 180.0F) {
			this.prevRotationPitch += 360.0F;
		}

		while (this.rotationYaw - this.prevRotationYaw < -180.0F) {
			this.prevRotationYaw -= 360.0F;
		}

		while (this.rotationYaw - this.prevRotationYaw >= 180.0F) {
			this.prevRotationYaw += 360.0F;
		}

		this.rotationPitch = this.prevRotationPitch + (this.rotationPitch - this.prevRotationPitch) * 0.2F;
		this.rotationYaw = this.prevRotationYaw + (this.rotationYaw - this.prevRotationYaw) * 0.2F;
		float drag = 0.99F;
		if (this.handleWaterMovement()) {
			for (int bubble = 0; bubble < 4; ++bubble) {
				this.worldObj.spawnParticle("bubble", this.posX - this.motionX * 0.25D, this.posY - this.motionY * 0.25D, this.posZ - this.motionZ * 0.25D, this.motionX, this.motionY, this.motionZ);
			}

			drag = 0.8F;
		}

		this.motionX *= (double) drag;
		this.motionY *= (double) drag;
		this.motionZ *= (double) drag;
		this.motionY -= (double) 0.03F;
		this.setPosition(this.posX, this.posY, this.posZ);
	}

	public final void writeEntityToNBT(NBTTagCompound tag) {
		tag.setShort("xTile", (short) this.xTile);
		tag.setShort("yTile", (short) this.yTile);
		tag.setShort("zTile", (short) this.zTile);
		tag.setByte("inTile", (byte) this.inTile);
		tag.setByte("shake", (byte) this.arrowShake);
		tag.setByte("inGround", (byte) (this.inGround ? 1 : 0));
	}

	public final void readEntityFromNBT(NBTTagCompound tag) {
		this.xTile = tag.getShort("xTile");
		this.yTile = tag.getShort("yTile");
		this.zTile = tag.getShort("zTile");
		this.inTile = tag.getByte("inTile") & 255;
		this.arrowShake = tag.getByte("shake") & 255;
		this.inGround = tag.getByte("inGround") == 1;
	}

	public final void onCollideWithPlayer(EntityPlayer player) {
		if (this.inGround && this.shootingEntity == player && this.arrowShake <= 0 && player.inventory.storePartialItemStack(new ItemStack(Item.arrow.shiftedIndex, 1))) {
			this.worldObj.playSoundAtEntity(this, "random.pop", 0.2F, ((this.rand.nextFloat() - this.rand.nextFloat()) * 0.7F + 1.0F) * 2.0F);
			player.onItemPickup(this);
			super.isDead = true;
		}
	}

	/** Result of the arrow's per-tick entity scan: the target and how far along the ray it sits. */
	private static class EntityHit {
		final Entity entity;
		final double distance;

		EntityHit(Entity entity, double distance) {
			this.entity = entity;
			this.distance = distance;
		}
	}
}