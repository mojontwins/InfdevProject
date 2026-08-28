package net.minecraft.client.render.camera;

import net.minecraft.game.physics.AxisAlignedBB;

/**
 * Tests axis-aligned bounding boxes against the view frustum so the renderer
 * can skip chunks/entities that are entirely off-screen.
 */
public class Frustrum {
	private final ClippingHelper clippingHelper = ClippingHelperImplementation.init();
	private double xPosition;
	private double yPosition;
	private double zPosition;

	public boolean isBoundingBoxInFrustrum(AxisAlignedBB boundingBox) {
		return this.isBoxInFrustum(boundingBox.minX, boundingBox.minY, boundingBox.minZ, boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ);
	}

	public void setPosition(double x, double y, double z) {
		this.xPosition = x;
		this.yPosition = y;
		this.zPosition = z;
	}

	/**
	 * Returns true when any corner of the box is on the inside of every frustum
	 * plane. Coordinates are offset by the frustum position so planes created in
	 * view space can be evaluated directly.
	 */
	public boolean isBoxInFrustum(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
		double relMinX = minX - this.xPosition;
		double relMinY = minY - this.yPosition;
		double relMinZ = minZ - this.zPosition;
		double relMaxX = maxX - this.xPosition; 
		double relMaxY = maxY - this.yPosition;
		double relMaxZ = maxZ - this.zPosition;
		ClippingHelper helper = this.clippingHelper;

		for(int planeId = 0; planeId < 6; ++planeId) {
			float[] plane = helper.frustrum[planeId];
			double aX = (double)plane[0];
			double aY = (double)plane[1];
			double aZ = (double)plane[2];
			double d = (double)plane[3];
			// Reject the box only if ALL eight corners lie outside the same plane.
			if(aX * relMinX + aY * relMinY + aZ * relMinZ + d <= 0.0D
					&& aX * relMaxX + aY * relMinY + aZ * relMinZ + d <= 0.0D
					&& aX * relMinX + aY * relMaxY + aZ * relMinZ + d <= 0.0D
					&& aX * relMaxX + aY * relMaxY + aZ * relMinZ + d <= 0.0D
					&& aX * relMinX + aY * relMinY + aZ * relMaxZ + d <= 0.0D
					&& aX * relMaxX + aY * relMinY + aZ * relMaxZ + d <= 0.0D
					&& aX * relMinX + aY * relMaxY + aZ * relMaxZ + d <= 0.0D
					&& aX * relMaxX + aY * relMaxY + aZ * relMaxZ + d <= 0.0D) {
				return false;
			}
		}

		return true;
	}
}