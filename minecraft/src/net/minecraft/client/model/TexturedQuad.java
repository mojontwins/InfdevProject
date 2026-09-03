package net.minecraft.client.model;

import net.minecraft.client.render.Tessellator;
import net.minecraft.game.physics.Vec3D;

/**
 * A textured four-sided polygon of the model. It holds four ordered vertices
 * and knows how to UV-map them onto the model's texture grid.
 *
 * <p>Two constructors are provided:
 * <ul>
 *   <li>The 4-arg form ({@code (uMin, vMin, uMax, vMax)}) hardcodes the
 *       64&times;32 texture grid used by every infdev entity model. It
 *       applies a small inset on each corner so adjacent quads do not
 *       bleed into each other (the classic "texture seam" trick).</li>
 *   <li>The 6-arg form ({@code (uMin, vMin, uMax, vMax, textureWidth,
 *       textureHeight)}) takes an explicit texture resolution, used by
 *       r1.2.5's {@link ModelBox} so model textures of any size can be
 *       sampled at the right scale.</li>
 * </ul>
 */
public final class TexturedQuad {
	public PositionTextureVertex[] vertexPositions;
	public int nVertices;
	private boolean invertNormal;

	private TexturedQuad(PositionTextureVertex[] vertices) {
		this.nVertices = 0;
		this.invertNormal = false;
		this.vertexPositions = vertices;
		this.nVertices = vertices.length;
	}

	/**
	 * Maps the quad's four corners onto the texture rectangle bounded by
	 * (uMin, vMin) in the top-left and (uMax, vMax) in the bottom-right of the
	 * 64&times;32 texture. A tiny inset is baked into each corner's UVs so
	 * adjacent quads do not bleed into each other.
	 *
	 * @param vertices the four ordered corners of the quad
	 * @param uMin     left edge of the texture rectangle
	 * @param vMin     top edge of the texture rectangle
	 * @param uMax     right edge of the texture rectangle
	 * @param vMax     bottom edge of the texture rectangle
	 */
	public TexturedQuad(PositionTextureVertex[] vertices, int uMin, int vMin, int uMax, int vMax) {
		this(vertices);
		vertices[0] = vertices[0].setTexturePosition((float)uMax / 64.0F - 0.0015625F, (float)vMin / 32.0F + 0.003125F);
		vertices[1] = vertices[1].setTexturePosition((float)uMin / 64.0F + 0.0015625F, (float)vMin / 32.0F + 0.003125F);
		vertices[2] = vertices[2].setTexturePosition((float)uMin / 64.0F + 0.0015625F, (float)vMax / 32.0F - 0.003125F);
		vertices[3] = vertices[3].setTexturePosition((float)uMax / 64.0F - 0.0015625F, (float)vMax / 32.0F - 0.003125F);
	}

	/**
	 * Maps the quad's four corners onto a texture rectangle of the given
	 * resolution. The inset scales with the texture size so it remains a
	 * single texel at any resolution. Used by {@link ModelBox} when a model
	 * is set up with a non-default {@code textureWidth}/{@code textureHeight}.
	 */
	public TexturedQuad(PositionTextureVertex[] vertices, int uMin, int vMin, int uMax, int vMax, float textureWidth, float textureHeight) {
		this(vertices);
		float insetU = 0.0F / textureWidth;
		float insetV = 0.0F / textureHeight;
		vertices[0] = vertices[0].setTexturePosition((float)uMax / textureWidth - insetU, (float)vMin / textureHeight + insetV);
		vertices[1] = vertices[1].setTexturePosition((float)uMin / textureWidth + insetU, (float)vMin / textureHeight + insetV);
		vertices[2] = vertices[2].setTexturePosition((float)uMin / textureWidth + insetU, (float)vMax / textureHeight - insetV);
		vertices[3] = vertices[3].setTexturePosition((float)uMax / textureWidth - insetU, (float)vMax / textureHeight - insetV);
	}

	/**
	 * Reverses the vertex order of the quad, flipping its winding. Used by
	 * {@link ModelBox} when the parent renderer is mirrored so each face
	 * still points outward.
	 */
	public void flipFace() {
		PositionTextureVertex[] reversed = new PositionTextureVertex[this.vertexPositions.length];
		for(int i = 0; i < this.vertexPositions.length; ++i) {
			reversed[i] = this.vertexPositions[this.vertexPositions.length - i - 1];
		}
		this.vertexPositions = reversed;
	}

	/**
	 * Emits the quad into the given tessellator, scaled by {@code scale}.
	 * Computes the face normal from the cross product of two of its edges
	 * and hands it to the tessellator for lighting.
	 */
	public void draw(Tessellator tessellator, float scale) {
		Vec3D edgeA = this.vertexPositions[1].vector3D.subtract(this.vertexPositions[0].vector3D);
		Vec3D edgeB = this.vertexPositions[1].vector3D.subtract(this.vertexPositions[2].vector3D);
		Vec3D normal = new Vec3D(
			edgeA.yCoord * edgeB.zCoord - edgeA.zCoord * edgeB.yCoord,
			edgeA.zCoord * edgeB.xCoord - edgeA.xCoord * edgeB.zCoord,
			edgeA.xCoord * edgeB.yCoord - edgeA.yCoord * edgeB.xCoord).normalize();
		tessellator.startDrawingQuads();
		if(this.invertNormal) {
			tessellator.setNormal(-(float)normal.xCoord, -(float)normal.yCoord, -(float)normal.zCoord);
		} else {
			tessellator.setNormal((float)normal.xCoord, (float)normal.yCoord, (float)normal.zCoord);
		}

		for(int i = 0; i < 4; ++i) {
			PositionTextureVertex vertex = this.vertexPositions[i];
			tessellator.addVertexWithUV(
				(double)((float)vertex.vector3D.xCoord * scale),
				(double)((float)vertex.vector3D.yCoord * scale),
				(double)((float)vertex.vector3D.zCoord * scale),
				(double)vertex.texturePositionX,
				(double)vertex.texturePositionY);
		}

		tessellator.draw();
	}
}
