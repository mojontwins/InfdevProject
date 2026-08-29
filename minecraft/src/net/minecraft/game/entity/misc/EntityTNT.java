package net.minecraft.game.entity.misc;

import com.mojang.nbt.NBTTagCompound;
import net.minecraft.game.entity.Entity;
import net.minecraft.game.world.World;
import util.MathHelper;

/**
 * A stick of primed TNT: a bouncing, smoking projectile laid down by the player
 * that turns into a 4-power explosion once the fuse runs out. The fuse counts
 * down on its own; only the initial push is random.
 */
public class EntityTNT extends Entity {
	/** Ticks left before the detonation. Public because the renderer reads it for the flashing texture. */
	public int fuse = 0;

	public EntityTNT(World world, float x, float y, float z) {
		super(world);
		this.preventEntitySpawning = true;
		this.setSize(0.98F, 0.98F);
		this.yOffset = this.height / 2.0F;
		this.setPosition((double) x, (double) y, (double) z);
		float explosiveYaw = (float) (Math.random() * (double) ((float) Math.PI) * 2.0D);
		this.motionX = (double) (-MathHelper.sin(explosiveYaw * (float) Math.PI / 180.0F) * 0.02F);
		this.motionY = (double) 0.2F;
		this.motionZ = (double) (-MathHelper.cos(explosiveYaw * (float) Math.PI / 180.0F) * 0.02F);
		this.entityWalks = false;
		this.fuse = 80;
		this.prevPosX = (double) x;
		this.prevPosY = (double) y;
		this.prevPosZ = (double) z;
	}

	public final boolean canBeCollidedWith() {
		return !this.isDead;
	}

	public final void onUpdate() {
		this.prevPosX = this.posX;
		this.prevPosY = this.posY;
		this.prevPosZ = this.posZ;
		this.motionY -= (double) 0.04F;
		this.moveEntity(this.motionX, this.motionY, this.motionZ);
		this.motionX *= (double) 0.98F;
		this.motionY *= (double) 0.98F;
		this.motionZ *= (double) 0.98F;
		if (this.onGround) {
			this.motionX *= (double) 0.7F;
			this.motionZ *= (double) 0.7F;
			this.motionY *= -0.5D;
		}

		// Post-decrement: the fuse reaches zero exactly 81 ticks after being lit.
		if (this.fuse-- <= 0) {
			super.isDead = true;
			this.worldObj.createExplosion((Entity) null, this.posX, this.posY, this.posZ, 4.0F);
		} else {
			this.worldObj.spawnParticle("smoke", this.posX, this.posY + 0.5D, this.posZ, 0.0D, 0.0D, 0.0D);
		}
	}

	protected final void writeEntityToNBT(NBTTagCompound tag) {
		tag.setByte("Fuse", (byte) this.fuse);
	}

	protected final void readEntityFromNBT(NBTTagCompound tag) {
		this.fuse = tag.getByte("Fuse");
	}
}