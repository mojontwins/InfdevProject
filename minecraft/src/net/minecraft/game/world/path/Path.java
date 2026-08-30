package net.minecraft.game.world.path;

/**
 * A binary min-heap keyed on {@link PathPoint#distanceToTarget} (the A* F
 * cost), used as the open list: {@link #dequeue()} always returns the cheapest
 * node. The heap lives in a plain growable array in which the children of slot
 * {@code i} are {@code 2*i+1} and {@code 2*i+2}; each point remembers its slot
 * so a re-weighted node can be bubbled up or sifted down in place.
 */
public final class Path {
	private PathPoint[] pathpoints = new PathPoint[1024];
	private int count = 0;

	/**
	 * Inserts {@code point} at the bottom of the heap and bubbles it up until
	 * it is no cheaper than its parent. Throws if the point is already on the
	 * heap - the pathfinder always hands over fresh nodes.
	 */
	public final PathPoint addPoint(PathPoint point) {
		if(point.index >= 0) {
			throw new IllegalStateException("OW KNOWS!");
		} else {
			if(this.count == this.pathpoints.length) {
				PathPoint[] grown = new PathPoint[this.count << 1];
				System.arraycopy(this.pathpoints, 0, grown, 0, this.count);
				this.pathpoints = grown;
			}

			this.pathpoints[this.count] = point;
			point.index = this.count;
			this.sortBack(this.count++);
			return point;
		}
	}

	/**
	 * Empties the open list between search runs. The backing array is kept and
	 * simply re-filled by the next {@link #addPoint} call.
	 */
	public final void clearPath() {
		this.count = 0;
	}

	/**
	 * Removes and returns the root (cheapest) node by moving the last element
	 * up to the root and sifting it back down.
	 */
	public final PathPoint dequeue() {
		PathPoint root = this.pathpoints[0];
		this.pathpoints[0] = this.pathpoints[--this.count];
		this.pathpoints[this.count] = null;
		if(this.count > 0) {
			this.sortForward(0);
		}

		root.index = -1;
		return root;
	}

	/**
	 * Updates {@code point}'s F cost and restores the heap invariant from its
	 * current slot - bubbling up when the cost dropped, sifting down when it
	 * rose.
	 */
	public final void changeDistance(PathPoint point, float distanceToTarget) {
		float oldCost = point.distanceToTarget;
		point.distanceToTarget = distanceToTarget;
		if(distanceToTarget < oldCost) {
			this.sortBack(point.index);
		} else {
			this.sortForward(point.index);
		}
	}

	/** Bubbles the element at {@code index} upward, toward the cheaper root. */
	private void sortBack(int index) {
		PathPoint point = this.pathpoints[index];

		int parentIndex;
		for(float cost = point.distanceToTarget; index > 0; index = parentIndex) {
			parentIndex = index - 1 >> 1;
			PathPoint parent = this.pathpoints[parentIndex];
			if(cost >= parent.distanceToTarget) {
				break;
			}

			this.pathpoints[index] = parent;
			parent.index = index;
		}

		this.pathpoints[index] = point;
		point.index = index;
	}

	/** Sifts the element at {@code index} downward, toward the more expensive leaves. */
	private void sortForward(int index) {
		PathPoint point = this.pathpoints[index];
		float cost = point.distanceToTarget;

		while(true) {
			int childLeft = 1 + (index << 1);
			int childRight = childLeft + 1;
			if(childLeft >= this.count) {
				break;
			}

			PathPoint leftChild = this.pathpoints[childLeft];
			float leftCost = leftChild.distanceToTarget;
			PathPoint cheaperChild;
			float cheaperCost;
			if(childRight >= this.count) {
				// Only a left child exists; treat the missing right child as infinitely expensive.
				cheaperChild = null;
				cheaperCost = Float.POSITIVE_INFINITY;
			} else {
				cheaperChild = this.pathpoints[childRight];
				cheaperCost = cheaperChild.distanceToTarget;
			}

			if(leftCost < cheaperCost) {
				if(leftCost >= cost) {
					break;
				}

				this.pathpoints[index] = leftChild;
				leftChild.index = index;
				index = childLeft;
			} else {
				if(cheaperCost >= cost) {
					break;
				}

				this.pathpoints[index] = cheaperChild;
				cheaperChild.index = index;
				index = childRight;
			}
		}

		this.pathpoints[index] = point;
		point.index = index;
	}

	public final boolean isPathEmpty() {
		return this.count == 0;
	}
}