package net.minecraft.game.entity.monster;

import net.minecraft.game.entity.ai.EntityAIAttackOnCollide;
import net.minecraft.game.entity.ai.EntityAIHurtByTarget;
import net.minecraft.game.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.game.entity.ai.EntityAISwimming;
import net.minecraft.game.entity.ai.EntityAIWander;
import net.minecraft.game.entity.ai.EntityAIWatchClosest;
import net.minecraft.game.entity.ai.EntityAILookIdle;
import net.minecraft.game.entity.player.EntityPlayer;
import net.minecraft.game.item.Item;
import net.minecraft.game.world.World;

/**
 * The creeper: sneaks up close, then hisses and swells while the fuse burns
 * before detonating in a 3.0-force explosion.
 *
 * <p>The fuse state is {@code -1} (idle) or {@code 1} (ignited) and is driven
 * by {@link EntityAICreeperSwell}.  Each tick {@link #onLivingUpdate} folds
 * that state into {@code timeSinceIgnited} ({@code 1} burns, {@code -1}
 * cools) and detonates when the burn reaches the fuse length.</p>
 */
public class EntityCreeper extends EntityMonster {
	/** Ticks the fuse has already burned (starts counting up from 0 when lit). */
	private int timeSinceIgnited;
	/** The value {@code timeSinceIgnited} had at the start of the current tick, for renderer interpolation. */
	private int lastActiveTime;
	/** Total fuse length in ticks before the creeper blows. */
	private int fuseTime = 30;
	/** Fuse state: -1 idle, 1 lit (driven by {@link EntityAICreeperSwell}). */
	private int fuseState = -1;

	public EntityCreeper(World world) {
		super(world);
		this.texture = "/mob/creeper.png";

		this.tasks.addTask(0, new EntityAISwimming(this));
		this.tasks.addTask(1, new EntityAICreeperSwell(this));
		this.tasks.addTask(2, new EntityAIAttackOnCollide(this, EntityPlayer.class, 0.8F, false));
		this.tasks.addTask(3, new EntityAIWander(this, 0.2F));
		this.tasks.addTask(4, new EntityAIWatchClosest(this, EntityPlayer.class, 8.0F));
		this.tasks.addTask(5, new EntityAILookIdle(this));
		this.targetTasks.addTask(1, new EntityAIHurtByTarget(this, false));
		this.targetTasks.addTask(2, new EntityAINearestAttackableTarget(this, EntityPlayer.class, 16.0F, 0, true));
	}

	@Override
	public void onLivingUpdate() {
		if (this.isEntityAlive()) {
			this.lastActiveTime = this.timeSinceIgnited;
			if (this.fuseState > 0 && this.timeSinceIgnited == 0) {
				this.worldObj.playSoundAtEntity(this, "random.fuse", 1.0F, 0.5F);
			}

			this.timeSinceIgnited += this.fuseState;
			if (this.timeSinceIgnited < 0) {
				this.timeSinceIgnited = 0;
			}

			if (this.timeSinceIgnited >= this.fuseTime) {
				this.timeSinceIgnited = this.fuseTime;
				this.worldObj.createExplosion(this, this.posX, this.posY, this.posZ, 3.0F);
				this.isDead = true;
			}
		}

		super.onLivingUpdate();
	}

	/** Fixes the renderer interpolation source to {@code timeSinceIgnited}. */
	public final float getFuseProgress(float partialTick) {
		return ((float)this.lastActiveTime + (float)(this.timeSinceIgnited - this.lastActiveTime) * partialTick) / (float)(this.fuseTime - 2);
	}

	/** The creeper never bites — contact is handled by the fuse (see {@link EntityAICreeperSwell}). */
	@Override
	public void attackEntity(net.minecraft.game.entity.Entity target, float distance) {
	}

	/** The current fuse state: -1 idle, 1 lit. */
	public int getCreeperState() {
		return this.fuseState;
	}

	/** Sets the fuse state: -1 idle, 1 lit. */
	public void setCreeperState(int state) {
		this.fuseState = state;
	}

	protected final int getDroppedItem() {
		return Item.gunpowder.shiftedIndex;
	}

	protected final String getHurtSound() {
		return "mob.creeper";
	}

	protected final String getDeathSound() {
		return "mob.creeperdeath";
	}
}
