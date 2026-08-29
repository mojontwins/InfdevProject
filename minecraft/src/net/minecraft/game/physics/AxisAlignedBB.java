package net.minecraft.game.physics;

/**
 * An axis-aligned bounding box, defined by its min and max corner. These are the
 * collision volumes of the game: every block keeps one
 * ({@link net.minecraft.game.world.block.Block#getCollisionBoundingBoxFromPool}) and
 * so does every entity ({@link net.minecraft.game.entity.Entity#boundingBox}). The
 * offset calculations in this class implement the sliding collision resolution used
 * by {@code Entity.moveEntity}.
 */
public final class AxisAlignedBB {

	public double minX;
	public double minY;
	public double minZ;
	public double maxX;
	public double maxY;
	public double maxZ;

	public AxisAlignedBB(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
		this.minX = minX;
		this.minY = minY;
		this.minZ = minZ;
		this.maxX = maxX;
		this.maxY = maxY;
		this.maxZ = maxZ;
	}

	/**
	 * The swept volume of this box while moving by (x, y, z): each face extends,
	 * but only into the direction of travel (negative travel grows the min side,
	 * positive travel the max side). Used to move a box's collision test through
	 * one frame of motion.
	 */
	public final AxisAlignedBB addCoord(double travelX, double travelY, double travelZ) {
		double newMinX = this.minX;
		double newMinY = this.minY;
		double newMinZ = this.minZ;
		double newMaxX = this.maxX;
		double newMaxY = this.maxY;
		double newMaxZ = this.maxZ;
		if (travelX < 0.0D) {
			newMinX += travelX;
		}
		if (travelX > 0.0D) {
			newMaxX += travelX;
		}
		if (travelY < 0.0D) {
			newMinY += travelY;
		}
		if (travelY > 0.0D) {
			newMaxY += travelY;
		}
		if (travelZ < 0.0D) {
			newMinZ += travelZ;
		}
		if (travelZ > 0.0D) {
			newMaxZ += travelZ;
		}
		return new AxisAlignedBB(newMinX, newMinY, newMinZ, newMaxX, newMaxY, newMaxZ);
	}

	/**
	 * This box inflated by the given offsets on both sides of each axis. The guard
	 * against Y-inverted boxes is a long-standing part of the original code (and its
	 * famous exception message is kept verbatim).
	 */
	public final AxisAlignedBB expand(double offsetX, double offsetY, double offsetZ) {
		if (this.minY > this.maxY) {
			throw new IllegalArgumentException("NOOOOOO!");
		}
		return new AxisAlignedBB(
			this.minX - offsetX, this.minY - offsetY, this.minZ - offsetZ,
			this.maxX + offsetX, this.maxY + offsetY, this.maxZ + offsetZ
		);
	}

	/** A copy of this box translated by the given offsets. */
	public final AxisAlignedBB offsetCopy(double offsetX, double offsetY, double offsetZ) {
		return new AxisAlignedBB(
			this.minX + offsetX, this.minY + offsetY, this.minZ + offsetZ,
			this.maxX + offsetX, this.maxY + offsetY, this.maxZ + offsetZ
		);
	}

	/**
	 * The largest X step {@code movingBox} may take before it touches this box
	 * (the collider): the requested {@code stepX}, clamped to the gap between the
	 * two boxes when they already overlap in Y and Z and the step would push the
	 * mover into this one. When there is no overlap on one of the other axes, or
	 * the step moves away, it is returned unchanged.
	 */
	public final double calculateXOffset(AxisAlignedBB movingBox, double stepX) {
		if (movingBox.maxY > this.minY && movingBox.minY < this.maxY && movingBox.maxZ > this.minZ && movingBox.minZ < this.maxZ) {
			double maxStep;
			if (stepX > 0.0D && movingBox.maxX <= this.minX) {
				maxStep = this.minX - movingBox.maxX;
				if (maxStep < stepX) {
					stepX = maxStep;
				}
			}
			if (stepX < 0.0D && movingBox.minX >= this.maxX) {
				maxStep = this.maxX - movingBox.minX;
				if (maxStep > stepX) {
					stepX = maxStep;
				}
			}
		}
		return stepX;
	}

	/** The largest Y step, see {@link #calculateXOffset}. */
	public final double calculateYOffset(AxisAlignedBB movingBox, double stepY) {
		if (movingBox.maxX > this.minX && movingBox.minX < this.maxX && movingBox.maxZ > this.minZ && movingBox.minZ < this.maxZ) {
			double maxStep;
			if (stepY > 0.0D && movingBox.maxY <= this.minY) {
				maxStep = this.minY - movingBox.maxY;
				if (maxStep < stepY) {
					stepY = maxStep;
				}
			}
			if (stepY < 0.0D && movingBox.minY >= this.maxY) {
				maxStep = this.maxY - movingBox.minY;
				if (maxStep > stepY) {
					stepY = maxStep;
				}
			}
		}
		return stepY;
	}

	/** The largest Z step, see {@link #calculateXOffset}. */
	public final double calculateZOffset(AxisAlignedBB movingBox, double stepZ) {
		if (movingBox.maxX > this.minX && movingBox.minX < this.maxX && movingBox.maxY > this.minY && movingBox.minY < this.maxY) {
			double maxStep;
			if (stepZ > 0.0D && movingBox.maxZ <= this.minZ) {
				maxStep = this.minZ - movingBox.maxZ;
				if (maxStep < stepZ) {
					stepZ = maxStep;
				}
			}
			if (stepZ < 0.0D && movingBox.minZ >= this.maxZ) {
				maxStep = this.maxZ - movingBox.minZ;
				if (maxStep > stepZ) {
					stepZ = maxStep;
				}
			}
		}
		return stepZ;
	}

	/** Whether the two boxes overlap in all three axes (boundaries not included). */
	public final boolean intersectsWith(AxisAlignedBB otherBox) {
		return otherBox.maxX > this.minX && otherBox.minX < this.maxX
			&& otherBox.maxY > this.minY && otherBox.minY < this.maxY
			&& otherBox.maxZ > this.minZ && otherBox.minZ < this.maxZ;
	}

	/** Translates this box in place by the given offsets. */
	public final void offset(double offsetX, double offsetY, double offsetZ) {
		this.minX += offsetX;
		this.minY += offsetY;
		this.minZ += offsetZ;
		this.maxX += offsetX;
		this.maxY += offsetY;
		this.maxZ += offsetZ;
	}

	/** A fresh copy of this box. */
	public final AxisAlignedBB copy() {
		return new AxisAlignedBB(this.minX, this.minY, this.minZ, this.maxX, this.maxY, this.maxZ);
	}

	/**
	 * The point at which the ray from {@code startVector} to {@code endVector} first
	 * pierces this box, wrapped as a {@link MovingObjectPosition} whose side code
	 * identifies the pierced face (0 = −Y, 1 = +Y, 2 = −Z, 3 = +Z, 4 = −X, 5 = +X),
	 * or {@code null} when the ray misses the box. Each face plane is intersected
	 * first, then the hit point must actually sit on the face — the nearest such
	 * point wins, with earlier planes breaking ties (the order the original used).
	 */
	public final MovingObjectPosition calculateIntercept(Vec3D startVector, Vec3D endVector) {
		Vec3D[] planePoints = {
			startVector.getIntermediateWithXValue(endVector, this.minX),
			startVector.getIntermediateWithXValue(endVector, this.maxX),
			startVector.getIntermediateWithYValue(endVector, this.minY),
			startVector.getIntermediateWithYValue(endVector, this.maxY),
			startVector.getIntermediateWithZValue(endVector, this.minZ),
			startVector.getIntermediateWithZValue(endVector, this.maxZ)
		};
		int[] sideHits = {4, 5, 0, 1, 2, 3};

		Vec3D closestPoint = null;
		int closestSide = -1;
		for (int plane = 0; plane < planePoints.length; ++plane) {
			Vec3D point = planePoints[plane];
			if (this.isPointOnPlane(plane, point) && (closestPoint == null || startVector.squareDistanceTo(point) < startVector.squareDistanceTo(closestPoint))) {
				closestPoint = point;
				closestSide = sideHits[plane];
			}
		}
		if (closestPoint == null) {
			return null;
		}
		return new MovingObjectPosition(0, 0, 0, closestSide, closestPoint);
	}

	/**
	 * Whether {@code point} lies on the {@code plane}-th face plane of this box, i.e.
	 * within the box's bounds on the two axes the plane does not fix. A {@code null}
	 * point (the segment never crossed that plane) is not on it.
	 */
	private boolean isPointOnPlane(int plane, Vec3D point) {
		if (point == null) {
			return false;
		}
		switch (plane) {
			case 0:
			case 1: // X− / X+ faces: only Y and Z are constrained.
				return point.yCoord >= this.minY && point.yCoord <= this.maxY && point.zCoord >= this.minZ && point.zCoord <= this.maxZ;
			case 2:
			case 3: // Y− / Y+ faces: only X and Z are constrained.
				return point.xCoord >= this.minX && point.xCoord <= this.maxX && point.zCoord >= this.minZ && point.zCoord <= this.maxZ;
			default: // Z− / Z+ faces: only X and Y are constrained.
				return point.xCoord >= this.minX && point.xCoord <= this.maxX && point.yCoord >= this.minY && point.yCoord <= this.maxY;
		}
	}
}