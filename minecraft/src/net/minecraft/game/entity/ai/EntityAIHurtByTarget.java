package net.minecraft.game.entity.ai;

import net.minecraft.game.entity.EntityLiving;

/**
 * Target the entity that last hit {@link #taskOwner}.  Optionally propagates
 * the target to nearby same-type entities.
 */
public class EntityAIHurtByTarget extends EntityAITarget {
	private final boolean callsForHelp;

	public EntityAIHurtByTarget(EntityLiving taskOwner, boolean callsForHelp) {
		super(taskOwner, 32.0F, false);
		this.callsForHelp = callsForHelp;
		this.setMutexBits(1);
	}

	@Override
	public boolean shouldExecute() {
		EntityLiving target = this.taskOwner.getAITarget();
		if (target == null) {
			return false;
		}
		return this.isValidTarget(target, false);
	}

	@Override
	public void startExecuting() {
		this.taskOwner.setAttackTarget(this.taskOwner.getAITarget());
		super.startExecuting();
	}
}
