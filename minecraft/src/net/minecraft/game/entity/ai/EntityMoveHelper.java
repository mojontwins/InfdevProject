package net.minecraft.game.entity.ai;

import net.minecraft.game.entity.EntityLiving;

/**
 * Smoothly interpolates an entity's yaw and forwards a movement target
 * to the navigation system.
 */
public class EntityMoveHelper {
	private final EntityLiving entity;
	private double posX;
	private double posY;
	private double posZ;
	private float speed;
	private boolean hasPath;

	public EntityMoveHelper(EntityLiving entity) {
		this.entity = entity;
	}

	public void setMoveTo(double x, double y, double z, float speed) {
		this.posX = x;
		this.posY = y;
		this.posZ = z;
		this.speed = speed;
		this.hasPath = true;
	}

	public void onUpdateMoveHelper() {
		if (this.hasPath) {
			this.hasPath = false;
			double dx = this.posX - this.entity.posX;
			//double dy = this.posY - this.entity.posY;
			double dz = this.posZ - this.entity.posZ;
			double dist = Math.sqrt(dx * dx + dz * dz);
			if (dist > 0.0D) {
				this.entity.rotationYaw = this.updateRotation(this.entity.rotationYaw,
					(float)(Math.atan2(dz, dx) * 180.0D / Math.PI) - 90.0F);
			}
		}
	}

	private float updateRotation(float current, float target) {
		float delta = wrapAngle(target - current);
		if (delta > 30.0F) {
			delta = 30.0F;
		}
		if (delta < -30.0F) {
			delta = -30.0F;
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

	public double getPosZ() { return this.posZ; }
	public double getPosY() { return this.posY; }
	public double getPosX() { return this.posX; }
	public float getSpeed() { return this.speed; }
	public boolean isUpdating() { return this.hasPath; }
}
