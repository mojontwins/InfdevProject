package net.minecraft.client.model;

import net.minecraft.game.physics.Vec3D;

/**
 * A single 3D vertex of a textured quad: it stores a position in model space
 * plus the (u, v) texture coordinates of the point the vertex samples from.
 */
public final class PositionTextureVertex {
	public Vec3D vector3D;
	public float texturePositionX;
	public float texturePositionY;

	/**
	 * @param x, y, z             position of the vertex in model space
	 * @param texturePositionX    u texture coordinate
	 * @param texturePositionY    v texture coordinate
	 */
	public PositionTextureVertex(float x, float y, float z, float texturePositionX, float texturePositionY) {
		this(new Vec3D((double)x, (double)y, (double)z), texturePositionX, texturePositionY);
	}

	/**
	 * Returns a copy of this vertex sharing the same position but with new
	 * texture coordinates (used when UV-mapping the corners of a quad).
	 *
	 * @param u new u texture coordinate
	 * @param v new v texture coordinate
	 */
	public final PositionTextureVertex setTexturePosition(float u, float v) {
		return new PositionTextureVertex(this, u, v);
	}

	private PositionTextureVertex(PositionTextureVertex vertex, float u, float v) {
		this.vector3D = vertex.vector3D;
		this.texturePositionX = u;
		this.texturePositionY = v;
	}

	private PositionTextureVertex(Vec3D vector, float u, float v) {
		this.vector3D = vector;
		this.texturePositionX = u;
		this.texturePositionY = v;
	}
}
