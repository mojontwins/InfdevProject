package net.minecraft.game.entity.monster;

import net.minecraft.game.entity.Entity;
import net.minecraft.game.item.Item;
import net.minecraft.game.world.World;

/**
 * The creeper: sneaks up close, then hisses and swells while the fuse burns
 * before detonating in a 3.0-force explosion.
 *
 * <p>The fuse state is kept to {-1, 1} between ticks: -1 means idle, 1 means
 * the fuse has been lit. The transient 2 stamped at the start of
	 * {@code updateEntityActionState} merely marks "was ignited" and collapses
 * back to -1 by the end of the tick.
 */
public class EntityCreeper extends EntityMonster {
	/** Ticks the fuse has already burned (starts counting up from 0 when lit). */
	private int timeSinceIgnited;
	/** The value {@code timeSinceIgnited} had at the start of the current tick, for renderer interpolation. */
	private int lastActiveTime;
	/** Total fuse length in ticks before the creeper blows. */
	private int fuseTime = 30;
	/** Fuse state: -1 idle, 1 lit (see the class comment for the transient 2). */
	private int fuseState = -1;

	public EntityCreeper(World world) {
		super(world);
		this.texture = "/mob/creeper.png";
	}

	protected final void updateEntityActionState() {
		this.lastActiveTime = this.timeSinceIgnited;
		if(this.timeSinceIgnited > 0 && this.fuseState < 0) {
			--this.timeSinceIgnited;
		}

		if(this.fuseState >= 0) {
			this.fuseState = 2;
		}

		super.updateEntityActionState();
		if(this.fuseState != 1) {
			this.fuseState = -1;
		}

	}

	/** Closes in for the kill until the target is inside blast range, then lights the fuse. */
	protected final void attackEntity(Entity target, float distance) {
		if(this.fuseState <= 0 && distance < 3.0F || this.fuseState > 0 && distance < 7.0F) {
			if(this.timeSinceIgnited == 0) {
				this.worldObj.playSoundAtEntity(this, "random.fuse", 1.0F, 0.5F);
			}

			this.fuseState = 1;
			++this.timeSinceIgnited;
			if(this.timeSinceIgnited == this.fuseTime) {
				this.worldObj.createExplosion(this, this.posX, this.posY, this.posZ, 3.0F);
				this.isDead = true;
			}

			this.hasAttacked = true;
		}

	}

	/**
	 * The 0..1 fuse progress smoothed by {@code partialTick}, used by the
	 * renderer to swell the body. The denominator is one fuse tick shorter so
	 * the flash peaks a hair before the boom.
	 */
	public final float getFuseProgress(float partialTick) {
		return ((float)this.lastActiveTime + (float)(this.timeSinceIgnited - this.lastActiveTime) * partialTick) / (float)(this.fuseTime - 2);
	}

	protected final int getDroppedItem() {
		return Item.gunpowder.shiftedIndex;
	}
}