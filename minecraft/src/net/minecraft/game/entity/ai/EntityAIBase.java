package net.minecraft.game.entity.ai;

/**
 * Abstract base for every AI task that can be registered on an
 * {@link net.minecraft.game.entity.ai.EntityAITasks} scheduler.
 *
 * <p>Each task declares a {@link #mutexBits} bitmask — two tasks whose bits
 * overlap cannot run concurrently at the same priority tier.  The scheduler
 * also uses {@link #isContinuous()} to decide whether a running task can be
 * preempted by an equal-priority newcomer.</p>
 */
public abstract class EntityAIBase {
	private int mutexBits = 0;

	/** May this task start right now? */
	public abstract boolean shouldExecute();

	/** Called every tick while the task is running.  Default re-checks {@link #shouldExecute()}. */
	public boolean continueExecuting() {
		return this.shouldExecute();
	}

	/**
	 * True (the default) means a running instance of this task cannot be
	 * interrupted by an equal-priority task that shares a mutex bit.  Set
	 * to {@code false} when the task should yield immediately.
	 */
	public boolean isContinuous() {
		return true;
	}

	/** Called once when the task transitions from idle to running. */
	public void startExecuting() {
	}

	/** Called once when the task stops (either killed or superseded). */
	public void resetTask() {
	}

	/** Called every tick while the task is running. */
	public void updateTask() {
	}

	public void setMutexBits(int mutexBits) {
		this.mutexBits = mutexBits;
	}

	public int getMutexBits() {
		return this.mutexBits;
	}
}
