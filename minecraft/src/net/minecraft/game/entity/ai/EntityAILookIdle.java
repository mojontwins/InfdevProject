package net.minecraft.game.entity.ai;

import net.minecraft.game.entity.EntityLiving;

/**
 * Idle behaviour: pick a random yaw and hold it for {@code 20 + rand(20)}
 * ticks.  Starts 2% of the time.
 */
public class EntityAILookIdle extends EntityAIBase {
	private final EntityLiving theEntity;
	private double xPosition;
	private double zPosition;
	private int idleCountdown;

	public EntityAILookIdle(EntityLiving entity) {
		this.theEntity = entity;
		this.setMutexBits(3);
	}

	@Override
	public boolean shouldExecute() {
		return this.theEntity.getRNG().nextFloat() < 0.02F;
	}

	@Override
	public boolean continueExecuting() {
		return this.idleCountdown >= 0;
	}

	@Override
	public void startExecuting() {
		double maxOffset = 6.283185307179586D;
		this.xPosition = this.theEntity.posX + Math.cos(this.theEntity.rotationYaw) * maxOffset;
		this.zPosition = this.theEntity.posZ + Math.sin(this.theEntity.rotationYaw) * maxOffset;
		this.idleCountdown = 20 + this.theEntity.getRNG().nextInt(20);
	}

	@Override
	public void updateTask() {
		--this.idleCountdown;
		this.theEntity.rotationYaw = (float)(Math.atan2(this.zPosition - this.theEntity.posZ, this.xPosition - this.theEntity.posX) * 180.0D / Math.PI) - 90.0F;
	}
}
