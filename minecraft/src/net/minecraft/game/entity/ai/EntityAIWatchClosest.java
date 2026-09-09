package net.minecraft.game.entity.ai;

import java.util.List;
import net.minecraft.game.entity.Entity;
import net.minecraft.game.entity.EntityLiving;

/**
 * Head-tracks the nearest entity of a given class within a radius.  Chooses a
 * new target 2% of the time and holds it for {@code 40 + rand(40)} ticks.
 */
public class EntityAIWatchClosest extends EntityAIBase {
	private final EntityLiving theEntity;
	private final Class<?> watchedClass;
	private final float maxDistance;
	private Entity closestEntity;
	private final float chance;
	private int lookTime;

	public EntityAIWatchClosest(EntityLiving entity, Class<?> watchedClass, float maxDistance) {
		this(entity, watchedClass, maxDistance, 0.02F);
	}

	public EntityAIWatchClosest(EntityLiving entity, Class<?> watchedClass, float maxDistance, float chance) {
		this.theEntity = entity;
		this.watchedClass = watchedClass;
		this.maxDistance = maxDistance;
		this.chance = chance;
		this.setMutexBits(2);
	}

	@Override
	public boolean shouldExecute() {
		if (this.theEntity.getRNG().nextFloat() >= this.chance) {
			return false;
		}
		if (this.theEntity.getAttackTarget() != null) {
			this.closestEntity = this.theEntity.getAttackTarget();
		} else {
			List<Entity> nearby = this.theEntity.worldObj.getEntitiesWithinAABBExcludingEntity(null,
				this.theEntity.boundingBox.expand((double)this.maxDistance, (double)this.maxDistance, (double)this.maxDistance));
			Entity closest = null;
			double closestDist = Double.MAX_VALUE;
			for (Entity entity : nearby) {
				if (!this.watchedClass.isInstance(entity)) {
					continue;
				}
				double dist = this.theEntity.getDistanceSqToEntity(entity);
				if (dist < closestDist) {
					closestDist = dist;
					closest = entity;
				}
			}
			if (closest == null) {
				return false;
			}
			this.closestEntity = closest;
		}
		return this.closestEntity != null;
	}

	@Override
	public boolean continueExecuting() {
		if (!this.closestEntity.isEntityAlive()) {
			return false;
		}
		double dist = this.theEntity.getDistanceSqToEntity(this.closestEntity);
		return dist <= (double)(this.maxDistance * this.maxDistance) && this.lookTime > 0;
	}

	@Override
	public void startExecuting() {
		this.lookTime = 40 + this.theEntity.getRNG().nextInt(40);
	}

	@Override
	public void resetTask() {
		this.closestEntity = null;
	}

	@Override
	public void updateTask() {
		this.theEntity.lookHelper.setLookPositionWithEntity(this.closestEntity, 10.0F, 10.0F);
		--this.lookTime;
	}
}
