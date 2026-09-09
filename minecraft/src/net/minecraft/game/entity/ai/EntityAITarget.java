package net.minecraft.game.entity.ai;

import net.minecraft.game.entity.EntityLiving;

/**
 * Abstract base for tasks that set {@link EntityLiving#setAttackTarget}.
 *
 * <p>Manages the common "keep a target alive and in sight" bookkeeping that
 * both {@link EntityAIHurtByTarget} and {@link EntityAINearestAttackableTarget}
 * share.</p>
 */
public abstract class EntityAITarget extends EntityAIBase {
	protected final EntityLiving taskOwner;
	private final float targetSearchRadius;
	private final boolean shouldCheckSight;
	private final boolean nearbyOnly;
	private int canTargetCountdown;
	private int reachabilityCheckCooldown;
	private int sightLossTimer;

	protected int getTargetSearchRadius() {
		return (int)this.targetSearchRadius;
	}

	public EntityAITarget(EntityLiving taskOwner, float targetSearchRadius, boolean shouldCheckSight) {
		this(taskOwner, targetSearchRadius, shouldCheckSight, false);
	}

	public EntityAITarget(EntityLiving taskOwner, float targetSearchRadius, boolean shouldCheckSight, boolean nearbyOnly) {
		this.taskOwner = taskOwner;
		this.targetSearchRadius = targetSearchRadius;
		this.shouldCheckSight = shouldCheckSight;
		this.nearbyOnly = nearbyOnly;
	}

	@Override
	public boolean continueExecuting() {
		EntityLiving target = this.taskOwner.getAttackTarget();
		if (target == null) {
			return false;
		}
		if (!target.isEntityAlive()) {
			return false;
		}
		double distSq = this.taskOwner.getDistanceSqToEntity(target);
		if (distSq > (double)(this.targetSearchRadius * this.targetSearchRadius)) {
			return false;
		}
		if (this.shouldCheckSight) {
			if (this.taskOwner.canEntityBeSeen(target)) {
				this.sightLossTimer = 0;
			} else if (++this.sightLossTimer > 60) {
				return false;
			}
		}
		return true;
	}

	@Override
	public void resetTask() {
		this.taskOwner.setAttackTarget(null);
	}

	/**
	 * Returns {@code true} when {@code target} passes all validation checks
	 * for this task type.
	 */
	protected boolean isValidTarget(EntityLiving target, boolean includePlayers) {
		if (target == null || target == this.taskOwner) {
			return false;
		}
		if (!target.isEntityAlive()) {
			return false;
		}
		return true;
	}

	public int getCanTargetCountdown() {
		return canTargetCountdown;
	}

	public void setCanTargetCountdown(int canTargetCountdown) {
		this.canTargetCountdown = canTargetCountdown;
	}

	public int getReachabilityCheckCooldown() {
		return reachabilityCheckCooldown;
	}

	public void setReachabilityCheckCooldown(int reachabilityCheckCooldown) {
		this.reachabilityCheckCooldown = reachabilityCheckCooldown;
	}

	public boolean isNearbyOnly() {
		return nearbyOnly;
	}
}
