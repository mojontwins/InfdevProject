package net.minecraft.game.entity.ai;

import net.minecraft.game.entity.Entity;
import net.minecraft.game.entity.EntityLiving;

/**
 * Smoothly interpolates an entity's head (yaw/pitch) toward a target
 * position.  The model reads {@code rotationYawHead} for the helmet angle
 * and {@code rotationPitch} for the vertical tilt.
 */
public class EntityLookHelper {
	private final EntityLiving entity;
	private float deltaYaw;
	private float deltaPitch;
	private boolean isLooking;
	private double posX;
	private double posY;
	private double posZ;

	public EntityLookHelper(EntityLiving entity) {
		this.entity = entity;
	}

	/** Aim the head toward the nearest point on {@code target}'s bounding box. */
	public void setLookPositionWithEntity(Entity target, float deltaYaw, float deltaPitch) {
		this.posX = target.posX;
		this.posY = (double)target.boundingBox.maxY + (double)target.getEyeHeight() * 0.5D;
		this.posZ = target.posZ;
		this.deltaYaw = deltaYaw;
		this.deltaPitch = deltaPitch;
		this.isLooking = true;
	}

	/** Aim the head toward an arbitrary world position. */
	public void setLookPosition(double x, double y, double z, float deltaYaw, float deltaPitch) {
		this.posX = x;
		this.posY = y;
		this.posZ = z;
		this.deltaYaw = deltaYaw;
		this.deltaPitch = deltaPitch;
		this.isLooking = true;
	}

	/** Called every tick to ease the head angles toward the target. */
	public void onUpdateLook() {
		if (this.isLooking) {
			this.isLooking = false;
			this.entity.rotationYawHead = this.updateRotation(this.entity.rotationYawHead,
				this.getYaw(this.entity.posX, this.entity.posZ, this.posX, this.posZ), this.deltaYaw);

			// Vertical tilt uses the real horizontal distance to the target, so an
			// approaching player no longer drives the head into a steep, unnatural
			// pitch the closer they get.
			double dx = this.posX - this.entity.posX;
			double dy = this.posY - (double)(this.entity.posY + this.entity.getEyeHeight());
			double dz = this.posZ - this.entity.posZ;
			double horizontal = Math.sqrt(dx * dx + dz * dz);
			this.entity.rotationPitch = this.updateRotation(this.entity.rotationPitch,
				(float)(-(Math.atan2(dy, horizontal) * 180.0D / Math.PI)), this.deltaPitch);
		} else {
			// Not tracking anything: ease the head back in line with the body.
			this.entity.rotationYawHead = this.updateRotation(this.entity.rotationYawHead, this.entity.renderYawOffset, 10.0F);
		}

		// A moving creature keeps its head swivel within a sane arc of its body.
		float headFromBody = wrapAngle(this.entity.rotationYawHead - this.entity.renderYawOffset);
		if (!this.entity.getNavigator().noPath()) {
			if (headFromBody < -75.0F) {
				this.entity.rotationYawHead = this.entity.renderYawOffset - 75.0F;
			}
			if (headFromBody > 75.0F) {
				this.entity.rotationYawHead = this.entity.renderYawOffset + 75.0F;
			}
		}
	}

	private float getYaw(double fromX, double fromZ, double toX, double toZ) {
		double dx = toX - fromX;
		double dz = toZ - fromZ;
		return (float)(Math.atan2(dz, dx) * 180.0D / Math.PI) - 90.0F;
	}

	private float updateRotation(float current, float target, float maxDelta) {
		float delta = wrapAngle(target - current);
		if (delta > maxDelta) {
			delta = maxDelta;
		}
		if (delta < -maxDelta) {
			delta = -maxDelta;
		}
		return current + delta;
	}

	private static float wrapAngle(float angle) {
		while (angle < -180.0F) {
			angle += 360.0F;
		}
		while (angle >= 180.0F) {
			angle -= 360.0F;
		}
		return angle;
	}
}
