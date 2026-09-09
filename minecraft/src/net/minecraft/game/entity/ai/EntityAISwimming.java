package net.minecraft.game.entity.ai;

import net.minecraft.game.entity.EntityLiving;

/**
 * Makes an entity jump restlessly while it is in water or lava.  The highest
 * priority ambient task — nothing else should take over while the host is
 * drowning.
 */
public class EntityAISwimming extends EntityAIBase {
	private final EntityLiving theEntity;

	public EntityAISwimming(EntityLiving entity) {
		this.theEntity = entity;
		this.setMutexBits(4);
		this.theEntity.navigator.setCanSwim(true);
	}

	@Override
	public boolean shouldExecute() {
		return this.theEntity.isInWater() || this.theEntity.isInLava();
	}

	@Override
	public void updateTask() {
		if (this.theEntity.getRNG().nextFloat() < 0.8F) {
			this.theEntity.isJumping = true;
		}
	}
}
