package net.minecraft.game.entity.monster;

import net.minecraft.game.entity.Entity;
import net.minecraft.game.entity.projectile.EntityArrow;
import net.minecraft.game.item.Item;
import net.minecraft.game.world.World;
import util.MathHelper;

/**
 * The undead archer: tracks the target up to 10 m away and looses a
 * fully-aimed arrow every 30 ticks. Burns up in the daylight.
 */
public class EntitySkeleton extends EntityMonster {
	public EntitySkeleton(World world) {
		super(world);
		this.texture = "/mob/skeleton.png";
	}

	public final void onLivingUpdate() {
		this.tryBurnInDaylight();
		super.onLivingUpdate();
	}

	protected final void attackEntity(Entity target, float distance) {
		if(distance < 10.0F) {
			double deltaX = target.posX - this.posX;
			double deltaZ = target.posZ - this.posZ;
			if(this.attackTime == 0) {
				System.out.println ("About to shoot arrow");
				EntityArrow arrow = new EntityArrow(this.worldObj, this);
				arrow.posY += (double)1.4F;
				double deltaY = target.posY - (double)0.2F - arrow.posY;
				float drop = MathHelper.sqrt_double(deltaX * deltaX + deltaZ * deltaZ) * 0.2F;
				this.worldObj.playSoundAtEntity(this, "random.bow", 1.0F, 1.0F / (this.rand.nextFloat() * 0.4F + 0.8F));
				this.worldObj.spawnEntityInWorld(arrow);
				arrow.setArrowHeading(deltaX, deltaY + (double)drop, deltaZ, 0.6F, 12.0F);
				this.attackTime = 30;
			}

			// Keep aiming at the target even between shots.
			this.rotationYaw = (float)(Math.atan2(deltaZ, deltaX) * 180.0D / (double)((float)Math.PI)) - 90.0F;
			this.hasAttacked = true;
		}

	}

	protected final int getDroppedItem() {
		return Item.arrow.shiftedIndex;
	}
}