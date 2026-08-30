package net.minecraft.game.world.path;

import net.minecraft.game.entity.Entity;
import net.minecraft.game.physics.Vec3D;

/**
 * The result of a completed A* search: an ordered list of waypoint cells (from
 * the target cell back through the {@code previous} chain), with a cursor that
 * advances as the walking creature reaches each waypoint.
 */
public final class PathEntity {
	private final PathPoint[] points;
	private int pathIndex;

	public PathEntity(PathPoint[] points) {
		this.points = points;
	}

	/** Moves the cursor on to the next waypoint. */
	public final void incrementPathIndex() {
		++this.pathIndex;
	}

	/** True once the cursor has moved past the last waypoint. */
	public final boolean isFinished() {
		return this.pathIndex >= this.points.length;
	}

	/**
	 * The world position the walking entity should head for right now: the cell
	 * at the cursor nudged toward the entity's centre by half of its padded
	 * width, so creatures of any size steer toward the middle of the block.
	 */
	public final Vec3D getPosition(Entity entity) {
		float paddedWidthHalf = (float)((int)(entity.width + 1.0F)) * 0.5F;
		float x = (float)this.points[this.pathIndex].xCoord + paddedWidthHalf;
		float y = (float)this.points[this.pathIndex].yCoord;
		float z = (float)this.points[this.pathIndex].zCoord + paddedWidthHalf;
		return new Vec3D((double)x, (double)y, (double)z);
	}
}