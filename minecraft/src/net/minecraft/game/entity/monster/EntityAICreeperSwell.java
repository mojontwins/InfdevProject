package net.minecraft.game.entity.monster;

import net.minecraft.game.entity.EntityLiving;
import net.minecraft.game.entity.ai.EntityAIBase;

/**
 * The creeper's attack: stalks its target until it gets within blast range,
 * then lights the fuse and plants itself in place while it swells.  The fuse
 * is defused the moment the target strays too far or hides behind a block.
 */
public class EntityAICreeperSwell extends EntityAIBase {
	private final EntityCreeper swellingCreeper;
	private EntityLiving creeperAttackTarget;

	public EntityAICreeperSwell(EntityCreeper creeper) {
		this.swellingCreeper = creeper;
		this.setMutexBits(1);
	}

	@Override
	public boolean shouldExecute() {
		EntityLiving target = this.swellingCreeper.getAttackTarget();
		return this.swellingCreeper.getCreeperState() > 0
			|| (target != null && this.swellingCreeper.getDistanceSqToEntity(target) < 9.0D);
	}

	@Override
	public void startExecuting() {
		this.swellingCreeper.getNavigator().clearPathEntity();
		this.creeperAttackTarget = this.swellingCreeper.getAttackTarget();
	}

	@Override
	public void resetTask() {
		this.creeperAttackTarget = null;
	}

	@Override
	public void updateTask() {
		if (this.creeperAttackTarget == null) {
			this.swellingCreeper.setCreeperState(-1);
		} else if (this.swellingCreeper.getDistanceSqToEntity(this.creeperAttackTarget) > 49.0D) {
			this.swellingCreeper.setCreeperState(-1);
		} else if (!this.swellingCreeper.canEntityBeSeen(this.creeperAttackTarget)) {
			this.swellingCreeper.setCreeperState(-1);
		} else {
			this.swellingCreeper.setCreeperState(1);
		}
	}
}
