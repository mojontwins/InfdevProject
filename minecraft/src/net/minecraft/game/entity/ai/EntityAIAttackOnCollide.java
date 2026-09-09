package net.minecraft.game.entity.ai;

import net.minecraft.game.entity.EntityLiving;
import net.minecraft.game.world.path.PathEntity;

/**
 * Chases the owner's current attack target and strikes melee when it comes
 * within reach.  The navigator walks a path toward the target; contact is
 * detected via the bounding-box intersection used by {@code EntityLiving}.
 * {@code isContinuous()} is {@code false} so it can be preempted by an
 * equal-priority task.
 */
public class EntityAIAttackOnCollide extends EntityAIBase {
	private final EntityLiving theEntity;
	private final Class<?> targetClass;
	private final float moveSpeed;
	private final boolean memory;
	private PathEntity currentPath;
	private int delayCounter;

	public EntityAIAttackOnCollide(EntityLiving host, Class<?> targetClass, float moveSpeed, boolean memory) {
		this.theEntity = host;
		this.targetClass = targetClass;
		this.moveSpeed = moveSpeed;
		this.memory = memory;
		this.setMutexBits(3);
	}

	@Override
	public boolean isContinuous() {
		return false;
	}

	@Override
	public boolean shouldExecute() {
		EntityLiving target = this.theEntity.getAttackTarget();
		if (target == null) {
			return false;
		}
		if (!this.targetClass.isAssignableFrom(target.getClass())) {
			return false;
		}
		this.currentPath = this.theEntity.getNavigator().getPathToEntityLiving(target);
		return this.currentPath != null;
	}

	@Override
	public boolean continueExecuting() {
		EntityLiving target = this.theEntity.getAttackTarget();
		if (target == null) {
			return false;
		}
		if (!target.isEntityAlive()) {
			return false;
		}
		if (!this.memory && this.theEntity.getDistanceSqToEntity(target) > 256.0D) {
			return false;
		}
		return !this.theEntity.getNavigator().noPath();
	}

	@Override
	public void startExecuting() {
		this.theEntity.getNavigator().setPath(this.currentPath, this.moveSpeed);
		this.delayCounter = 0;
	}

	@Override
	public void resetTask() {
		this.theEntity.getNavigator().clearPathEntity();
	}

	@Override
	public void updateTask() {
		EntityLiving target = this.theEntity.getAttackTarget();
		if (target == null) {
			return;
		}
		this.theEntity.getLookHelper().setLookPositionWithEntity(target, 30.0F, 30.0F);

		if (--this.delayCounter <= 0) {
			if (this.theEntity.getDistanceSqToEntity(target) <= 256.0D) {
				this.currentPath = this.theEntity.getNavigator().getPathToEntityLiving(target);
				this.theEntity.getNavigator().setPath(this.currentPath, this.moveSpeed);
				this.delayCounter = 4 + this.theEntity.getRNG().nextInt(7);
			}
		}

		double reach = (double)(this.theEntity.width * this.theEntity.width);
		if (this.theEntity.boundingBox.expand(reach, reach, reach).intersectsWith(target.boundingBox)) {
			this.theEntity.attackEntityAsMob(target);
		}
	}
}
