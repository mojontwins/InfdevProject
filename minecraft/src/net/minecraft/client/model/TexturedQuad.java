package net.minecraft.client.model;

/**
 * A textured four-sided polygon of the model. It holds four ordered vertices
 * and knows how to UV-map them onto the 64x32 pixel texture grid that all
 * entity models share.
 */
public final class TexturedQuad {
	public PositionTextureVertex[] vertexPositions;

	private TexturedQuad(PositionTextureVertex[] vertices) {
		this.vertexPositions = vertices;
	}

	/**
	 * Maps the quad's four corners onto the texture rectangle bounded by
	 * (uMin, vMin) in the top-left and (uMax, vMax) in the bottom-right of the
	 * 64x32 texture. A tiny inset is baked into each corner's UVs so adjacent
	 * quads do not bleed into each other (the classic "texture seam" trick).
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
}
