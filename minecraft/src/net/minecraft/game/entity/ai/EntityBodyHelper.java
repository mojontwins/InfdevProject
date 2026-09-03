package net.minecraft.game.entity.ai;

import net.minecraft.game.entity.EntityLiving;

/**
 * Eases the body yaw ({@code renderYawOffset}) to follow the head yaw
 * ({@code rotationYawHead}), so a living creature turns as a whole instead of
 * craning its head through a full 360 degrees.
 *
 * <p>While the entity is moving the body snaps to the travel heading and the
 * head is free to swivel up to a comfortable arc from it. While it stands
 * still, the body slowly turns to re-align with wherever the head is looking,
 * and once the head has held a direction for a while the allowed offset
 * shrinks so the creature naturally settles back toward facing its target.</p>
 */
public class EntityBodyHelper {
	private final EntityLiving theEntity;
	/** Ticks the head has been pointing within a few degrees of its last heading. */
	private int idleTickCount = 0;
	/** The head yaw last time it was pointing somewhere new. */
	private float lastHeadYaw = 0.0F;

	public EntityBodyHelper(EntityLiving theEntity) {
		this.theEntity = theEntity;
	}

	/** Called every tick to keep the body in step with the head. */
	public void onUpdateBody() {
		double movedX = this.theEntity.posX - this.theEntity.prevPosX;
		double movedZ = this.theEntity.posZ - this.theEntity.prevPosZ;
		if(movedX * movedX + movedZ * movedZ > 2.500000277905201E-7D) {
			// Moving: the body runs with the travel heading, and the head may
			// turn up to a set arc off it.
			this.theEntity.renderYawOffset = this.theEntity.rotationYaw;
			this.theEntity.rotationYawHead = this.easeToward(this.theEntity.renderYawOffset, this.theEntity.rotationYawHead, 75.0F);
			this.lastHeadYaw = this.theEntity.rotationYawHead;
			this.idleTickCount = 0;
		} else {
			// Standing still: turn the body toward where the head is pointing.
			float allowedTurn = 75.0F;
			if(Math.abs(this.theEntity.rotationYawHead - this.lastHeadYaw) > 15.0F) {
				this.idleTickCount = 0;
				this.lastHeadYaw = this.theEntity.rotationYawHead;
			} else {
				++this.idleTickCount;
				if(this.idleTickCount > 10) {
					allowedTurn = Math.max(1.0F - (float)(this.idleTickCount - 10) / 10.0F, 0.0F) * 75.0F;
				}
			}

			this.theEntity.renderYawOffset = this.easeToward(this.theEntity.rotationYawHead, this.theEntity.renderYawOffset, allowedTurn);
		}
	}

	/**
	 * Moves {@code current} up to {@code maxDelta} degrees toward {@code target}
	 * (measuring across the 360-degree wrap), returning the eased value.
	 */
	private float easeToward(float target, float current, float maxDelta) {
		float delta = target - current;
		while(delta < -180.0F) {
			delta += 360.0F;
		}

		while(delta >= 180.0F) {
			delta -= 360.0F;
		}

		if(delta < -maxDelta) {
			delta = -maxDelta;
		}

		if(delta >= maxDelta) {
			delta = maxDelta;
		}

		return target - delta;
	}
}
