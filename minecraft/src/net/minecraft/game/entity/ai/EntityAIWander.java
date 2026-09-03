package net.minecraft.game.entity.ai;

import net.minecraft.game.entity.EntityCreature;
import net.minecraft.game.entity.EntityLiving;
import net.minecraft.game.physics.Vec3D;

/**
 * Randomly wanders toward a nearby spot weighted by the creature's
 * {@code getBlockPathWeight}.  A 1-in-120 chance per tick, and only while
 * the creature is young (entityAge < 100).
 */
public class EntityAIWander extends EntityAIBase {
	private final EntityCreature theEntity;
	private double xPosition;
	private double yPosition;
	private double zPosition;
	private final float speed;

	public EntityAIWander(EntityCreature entity, float speed) {
		this.theEntity = entity;
		this.speed = speed;
		this.setMutexBits(1);
	}

	@Override
	public boolean shouldExecute() {
		if (this.theEntity.getRNG().nextInt(120) != 0) {
			return false;
		}
		if (this.theEntity.entityAge >= 100) {
			return false;
		}
		Vec3D target = RandomPositionGenerator.findRandomTarget(this.theEntity, 10, 7);
		if (target == null) {
			return false;
		}
		this.xPosition = target.xCoord;
		this.yPosition = target.yCoord;
		this.zPosition = target.zCoord;
		return true;
	}

	@Override
	public boolean continueExecuting() {
		return !this.theEntity.navigator.noPath();
	}

	@Override
	public void startExecuting() {
		this.theEntity.navigator.tryMoveToXYZ(this.xPosition, this.yPosition, this.zPosition, this.speed);
	}
}
