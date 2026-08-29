package net.minecraft.game.physics;

import util.MathHelper;

/**
 * An immutable 3D double-precision vector — the everyday math primitive of the
 * physics package. Instances are short-lived: every method that behaves like a
 * point transform returns a fresh vector, so the objects used for ray tracing
 * and distance checks can be shared freely.
 */
public final class Vec3D {

	public double xCoord;
	public double yCoord;
	public double zCoord;

	public Vec3D(double xCoord, double yCoord, double zCoord) {
		this.xCoord = xCoord;
		this.yCoord = yCoord;
		this.zCoord = zCoord;
	}

	/** The vector pointing from {@code other} to this vector. */
	public final Vec3D subtract(Vec3D other) {
		return new Vec3D(this.xCoord - other.xCoord, this.yCoord - other.yCoord, this.zCoord - other.zCoord);
	}

	/** A unit vector pointing in the same direction as this one. */
	public final Vec3D normalize() {
		double length = MathHelper.sqrt_double(this.xCoord * this.xCoord + this.yCoord * this.yCoord + this.zCoord * this.zCoord);
		return new Vec3D(this.xCoord / length, this.yCoord / length, this.zCoord / length);
	}

	/** This vector translated by the given offsets. */
	public final Vec3D addVector(double offsetX, double offsetY, double offsetZ) {
		return new Vec3D(this.xCoord + offsetX, this.yCoord + offsetY, this.zCoord + offsetZ);
	}

	/** Euclidean distance to {@code other}. */
	public final double distance(Vec3D other) {
		double dx = other.xCoord - this.xCoord;
		double dy = other.yCoord - this.yCoord;
		double dz = other.zCoord - this.zCoord;
		return MathHelper.sqrt_double(dx * dx + dy * dy + dz * dz);
	}

	/** Squared Euclidean distance to {@code other}; avoids the square root when only the ranking matters. */
	public final double squareDistanceTo(Vec3D other) {
		double dx = other.xCoord - this.xCoord;
		double dy = other.yCoord - this.yCoord;
		double dz = other.zCoord - this.zCoord;
		return dx * dx + dy * dy + dz * dz;
	}

	/**
	 * The point along the segment from this vector to {@code end} that crosses the
	 * given X plane, or {@code null} when the segment stays entirely on one side of
	 * that plane (runs parallel to it or does not reach it). The three
	 * {@code getIntermediateWith*} helpers share one implementation.
	 */
	public final Vec3D getIntermediateWithXValue(Vec3D end, double targetX) {
		return this.getIntermediateOnAxis(end, targetX, Axis.X);
	}

	/** The point crossing {@code targetY}, see {@link #getIntermediateWithXValue}. */
	public final Vec3D getIntermediateWithYValue(Vec3D end, double targetY) {
		return this.getIntermediateOnAxis(end, targetY, Axis.Y);
	}

	/** The point crossing {@code targetZ}, see {@link #getIntermediateWithXValue}. */
	public final Vec3D getIntermediateWithZValue(Vec3D end, double targetZ) {
		return this.getIntermediateOnAxis(end, targetZ, Axis.Z);
	}

	/** Shared interpolation behind the three {@code getIntermediateWith*} helpers. */
	private Vec3D getIntermediateOnAxis(Vec3D end, double targetValue, Axis axis) {
		double dx = end.xCoord - this.xCoord;
		double dy = end.yCoord - this.yCoord;
		double dz = end.zCoord - this.zCoord;
		double axisDelta = axis.component(end) - axis.component(this);
		if (axisDelta * axisDelta < (double) 1.0E-7F) {
			// The segment runs parallel to that axis' planes — it never crosses them.
			return null;
		}
		double progress = (targetValue - axis.component(this)) / axisDelta;
		if (progress < 0.0D || progress > 1.0D) {
			// The crossing lies outside the segment.
			return null;
		}
		return new Vec3D(this.xCoord + dx * progress, this.yCoord + dy * progress, this.zCoord + dz * progress);
	}

	/** One of the three coordinate axes, used to parameterize the shared helper above. */
	private enum Axis {
		X, Y, Z;

		/** The component of {@code vec} on this axis. */
		double component(Vec3D vec) {
			switch (this) {
				case X:
					return vec.xCoord;
				case Y:
					return vec.yCoord;
				default:
					return vec.zCoord;
			}
		}
	}

	@Override
	public String toString() {
		return "(" + this.xCoord + ", " + this.yCoord + ", " + this.zCoord + ")";
	}
}