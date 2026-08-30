package net.minecraft.game.world.path;

import util.MathHelper;

/**
 * A single node of the A* search grid: one walkable block coordinate plus the
 * bookkeeping the {@link Path} priority queue and the {@link Pathfinder} need
 * while a path is being built.
 *
 * <p>Two points are equal when their packed {@link #hash} values match, i.e.
 * when all three coordinates agree - a world cell is represented by exactly one
 * {@code PathPoint} instance during a search.
 */
public final class PathPoint {
	/** Block x of this node. */
	public final int xCoord;
	/** Block y of this node. */
	public final int yCoord;
	/** Block z of this node. */
	public final int zCoord;
	/** The three coordinates packed into one int ({@code x | y << 10 | z << 20}) so a whole cell fits into a single {@code Map} key. */
	public final int hash;
	/**
	 * Slot inside the {@link Path} heap array, or {@code -1} when the point is
	 * not on the open list. Doubles as the "already enqueued" flag tested by
	 * {@link #isAssigned()}.
	 */
	int index = -1;
	/** G cost: distance travelled from the start along {@link #previous}. */
	float totalPathDistance;
	/** H cost: straight-line distance from here to the target (not yet weighted). */
	float distanceToNext;
	/** F cost ({@link #totalPathDistance} + {@link #distanceToNext}) - the heap sort key. */
	float distanceToTarget;
	/** The node this one was reached from; walked backwards when re-tracing the finished path. */
	PathPoint previous;
	/** True once the search came here and expanded it - an A* "closed" marker. */
	public boolean isFirst = false;

	public PathPoint(int xCoord, int yCoord, int zCoord) {
		this.xCoord = xCoord;
		this.yCoord = yCoord;
		this.zCoord = zCoord;
		this.hash = xCoord | yCoord << 10 | zCoord << 20;
	}

	/** Euclidean distance between the centres of this point and {@code point}. */
	public final float distanceTo(PathPoint point) {
		float deltaX = (float)(point.xCoord - this.xCoord);
		float deltaY = (float)(point.yCoord - this.yCoord);
		float deltaZ = (float)(point.zCoord - this.zCoord);
		return MathHelper.sqrt_float(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
	}

	/**
	 * Two points are equal iff their cells match. The cast is direct (no
	 * {@code instanceof}) - the pathfinder only ever compares its own points.
	 */
	public final boolean equals(Object point) {
		return ((PathPoint)point).hash == this.hash;
	}

	public final int hashCode() {
		return this.hash;
	}

	/** True while this node sits in the {@link Path} open list. */
	public final boolean isAssigned() {
		return this.index >= 0;
	}

	@Override
	public final String toString() {
		return this.xCoord + ", " + this.yCoord + ", " + this.zCoord;
	}
}