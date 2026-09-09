package net.minecraft.game.entity.ai;

import java.util.ArrayList;
import java.util.List;

/**
 * The per-entity task scheduler.  Two independent lists exist on every
 * {@link net.minecraft.game.entity.EntityLiving}: {@code tasks} (movement /
 * action goals) and {@code targetTasks} (targeting goals).
 *
 * <p>Each {@link EntityAIBase} carries a {@code mutexBits} bitmask.  Two
 * tasks whose bits overlap cannot coexist in the same priority tier; if
 * their priorities differ the higher-priority one wins.</p>
 */
public class EntityAITasks {
	private final List<EntityAITaskEntry> taskEntries = new ArrayList<>();
	private final List<EntityAITaskEntry> executingTaskEntries = new ArrayList<>();

	/**
	 * Registers a task at the given priority tier.  Lower numbers mean higher
	 * priority (tier 0 outranks tier 1).
	 */
	public void addTask(int priority, EntityAIBase task) {
		this.taskEntries.add(new EntityAITaskEntry(this, priority, task));
	}

	/** Called every entity tick to start, stop and tick all registered tasks. */
	public void onUpdateTasks() {
		List<EntityAITaskEntry> newTasks = null;

		// Walk the full list — entries may be added or removed by tasks that
		// start or stop during this pass, so we iterate over a snapshot.
		List<EntityAITaskEntry> snapshot = new ArrayList<>(this.taskEntries);
		for (EntityAITaskEntry entry : snapshot) {
			boolean isRunning = this.executingTaskEntries.contains(entry);
			if (isRunning) {
				if (!this.canUse(entry) || !entry.action.continueExecuting()) {
					entry.action.resetTask();
					this.executingTaskEntries.remove(entry);
				}
			} else {
				if (this.canUse(entry) && entry.action.shouldExecute()) {
					if (newTasks == null) {
						newTasks = new ArrayList<>();
					}
					newTasks.add(entry);
					this.executingTaskEntries.add(entry);
				}
			}
		}

		if (newTasks != null) {
			for (EntityAITaskEntry entry : newTasks) {
				entry.action.startExecuting();
			}
		}

		// Tick every running task.
		for (EntityAITaskEntry entry : this.executingTaskEntries) {
			entry.action.updateTask();
		}
	}

	/**
	 * Returns {@code true} when {@code entry} is allowed to start given
	 * every task that is currently executing.
	 */
	private boolean canUse(EntityAITaskEntry entry) {
		for (EntityAITaskEntry other : this.executingTaskEntries) {
			if (other == entry) {
				continue;
			}

			// Mutex conflict: same tier or lower-priority running task blocks us.
			if (!this.areTasksCompatible(entry, other)) {
				if (entry.priority >= other.priority) {
					return false;
				}
				if (other.action.isContinuous()) {
					return false;
				}
			}
		}
		return true;
	}

	/** Two tasks are compatible when their mutex bits do not overlap. */
	private boolean areTasksCompatible(EntityAITaskEntry a, EntityAITaskEntry b) {
		return (a.action.getMutexBits() & b.action.getMutexBits()) == 0;
	}
}
