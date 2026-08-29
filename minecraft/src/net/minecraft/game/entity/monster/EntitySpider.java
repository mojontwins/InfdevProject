package net.minecraft.game.entity.monster;

import net.minecraft.game.entity.Entity;
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
	}

	/** Spiders only hunt while it is dark enough. */
	protected final Entity findPlayerToAttack() {
		float brightness = this.getEntityBrightness(1.0F);
		if(brightness < 0.5F) {
			double distanceSq = this.worldObj.playerEntity.getDistanceSqToEntity(this);
			if(distanceSq < 256.0D) {
				return this.worldObj.playerEntity;
			}
		}

		return null;
	}

	/**
	 * Bright light makes an attacking spider give up on its target entirely;
	 * in the dark, a nearby spider occasionally pounces, then falls back to
	 * the plain bite.
	 */
	protected final void attackEntity(Entity target, float distance) {
		float brightness = this.getEntityBrightness(1.0F);
		if(brightness > 0.5F && this.rand.nextInt(100) == 0) {
			this.playerToAttack = null;
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

	protected final int getDroppedItem() {
		return Item.silk.shiftedIndex;
	}
}