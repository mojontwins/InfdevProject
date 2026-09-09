package net.minecraft.game.entity.ai;

/**
 * A single registration record inside {@link EntityAITasks}: pairs a task
 * instance with its priority tier and a back-reference to the owning scheduler.
 */
class EntityAITaskEntry {
	public final EntityAIBase action;
	public final int priority;
	final EntityAITasks taskList;

	public EntityAITaskEntry(EntityAITasks taskList, int priority, EntityAIBase action) {
		this.taskList = taskList;
		this.priority = priority;
		this.action = action;
	}
}
