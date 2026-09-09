package net.minecraft.game.entity.monster;

import net.minecraft.game.entity.Entity;
import net.minecraft.game.entity.ai.EntityAIAttackOnCollide;
import net.minecraft.game.entity.ai.EntityAISwimming;
import net.minecraft.game.entity.ai.EntityAIWander;
import net.minecraft.game.entity.ai.EntityAIWatchClosest;
import net.minecraft.game.entity.ai.EntityAILookIdle;
import net.minecraft.game.entity.player.EntityPlayer;
import net.minecraft.game.item.Item;
import net.minecraft.game.world.World;
import util.MathHelper;

/**
 * The swift night hunter: only stalks the player in the dark, and backs off
 * when it comes back out into the light.
 */
public class EntitySpider extends EntityMonster {
	public EntitySpider(World world) {
		super(world);
		this.texture = "/mob/spider.png";
		this.setSize(1.4F, 0.9F);
		this.moveSpeed = 0.8F;

		this.tasks.addTask(0, new EntityAISwimming(this));
		this.tasks.addTask(1, new EntityAIAttackOnCollide(this, EntityPlayer.class, this.moveSpeed, false));
		this.tasks.addTask(2, new EntityAIWander(this, this.moveSpeed));
		this.tasks.addTask(3, new EntityAIWatchClosest(this, EntityPlayer.class, 8.0F));
		this.tasks.addTask(4, new EntityAILookIdle(this));
		this.targetTasks.addTask(2, new EntityAISpiderTarget(this, EntityPlayer.class, 16.0F, 0, false));
	}

	/**
	 * Bright light makes an attacking spider give up on its target entirely;
	 * in the dark, a nearby spider occasionally pounces, then falls back to
	 * the plain bite.
	 */
	@Override
	public void attackEntity(Entity target, float distance) {
		float brightness = this.getEntityBrightness(1.0F);
		if(brightness > 0.5F && this.rand.nextInt(100) == 0) {
			this.setAttackTarget(null);
		} else {
			// The pounce: within 2-6 m there is a 1-in-10 chance of a leap,
			// but only from the ground — an airborne roll at that range simply
			// does nothing at all (a faithful quirk of the original).
			if(distance > 2.0F && distance < 6.0F && this.rand.nextInt(10) == 0) {
				if(this.onGround) {
					double deltaX = target.posX - this.posX;
					double deltaZ = target.posZ - this.posZ;
					float attackRange = MathHelper.sqrt_double(deltaX * deltaX + deltaZ * deltaZ);
					this.motionX = deltaX / (double)attackRange * 0.5D * (double)0.8F + this.motionX * (double)0.2F;
					this.motionZ = deltaZ / (double)attackRange * 0.5D * (double)0.8F + this.motionZ * (double)0.2F;
					this.motionY = (double)0.4F;
					return;
				}
			} else {
				super.attackEntity(target, distance);
			}

		}

	}

	/**
	 * Spiders climb any vertical surface they are pressed against. The
	 * {@link net.minecraft.game.entity.EntityLiving} ladder plumbing (no fall
	 * damage, downward speed cap, upward {@code motionY = 0.2} on horizontal
	 * collision) is already wired in; this override activates it for spiders.
	 * Taken from b1.7.3.
	 */
	@Override
	public boolean isOnLadder() {
		return this.isCollidedHorizontally;
	}

	protected final int getDroppedItem() {
		return Item.silk.shiftedIndex;
	}

	protected final String getLivingSound() {
		return "mob.spider";
	}

	protected final String getHurtSound() {
		return "mob.spider";
	}

	protected final String getDeathSound() {
		return "mob.spiderdeath";
	}
}
