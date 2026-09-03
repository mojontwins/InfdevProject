package net.minecraft.client.model;

import net.minecraft.client.render.Tessellator;

/**
 * A named sub-box of a {@link ModelRenderer}, holding six textured faces that
 * share a common origin. The renderer keeps a list of boxes and renders each
 * one from its own list of {@link TexturedQuad} faces.
 *
 * <p>Ported from r1.2.5. A box is built from the eight corners of an
 * axis-aligned cuboid (inflated by {@code expansion}), with each of the six
 * faces mapping onto its rectangle of the model's texture. When the owning
 * renderer is mirrored, every face's vertex winding is flipped so the
 * quad still faces outward.
 */
public class ModelBox {
	private PositionTextureVertex[] vertexPositions;
	private TexturedQuad[] quadList;
	public final float posX1;
	public final float posY1;
	public final float posZ1;
	public final float posX2;
	public final float posY2;
	public final float posZ2;
	public String boxName;

	public ModelBox(ModelRenderer renderer, int textureX, int textureY, float x, float y, float z, int width, int height, int depth, float expansion) {
		this.posX1 = x;
		this.posY1 = y;
		this.posZ1 = z;
		this.posX2 = x + (float)width;
		this.posY2 = y + (float)height;
		this.posZ2 = z + (float)depth;
		this.vertexPositions = new PositionTextureVertex[8];
		this.quadList = new TexturedQuad[6];
		float endX = x + (float)width;
		float endY = y + (float)height;
		float endZ = z + (float)depth;
		x -= expansion;
		y -= expansion;
		z -= expansion;
		endX += expansion;
		endY += expansion;
		endZ += expansion;
		if(renderer.mirror) {
			float swap = endX;
			endX = x;
			x = swap;
		}

		PositionTextureVertex minXMinYMinZ = new PositionTextureVertex(x, y, z, 0.0F, 0.0F);
		PositionTextureVertex maxXMinYMinZ = new PositionTextureVertex(endX, y, z, 0.0F, 8.0F);
		PositionTextureVertex maxXMaxYMinZ = new PositionTextureVertex(endX, endY, z, 8.0F, 8.0F);
		PositionTextureVertex minXMaxYMinZ = new PositionTextureVertex(x, endY, z, 8.0F, 0.0F);
		PositionTextureVertex minXMinYMaxZ = new PositionTextureVertex(x, y, endZ, 0.0F, 0.0F);
		PositionTextureVertex maxXMinYMaxZ = new PositionTextureVertex(endX, y, endZ, 0.0F, 8.0F);
		PositionTextureVertex maxXMaxYMaxZ = new PositionTextureVertex(endX, endY, endZ, 8.0F, 8.0F);
		PositionTextureVertex minXMaxYMaxZ = new PositionTextureVertex(x, endY, endZ, 8.0F, 0.0F);
		this.vertexPositions[0] = minXMinYMinZ;
		this.vertexPositions[1] = maxXMinYMinZ;
		this.vertexPositions[2] = maxXMaxYMinZ;
		this.vertexPositions[3] = minXMaxYMinZ;
		this.vertexPositions[4] = minXMinYMaxZ;
		this.vertexPositions[5] = maxXMinYMaxZ;
		this.vertexPositions[6] = maxXMaxYMaxZ;
		this.vertexPositions[7] = minXMaxYMaxZ;
		float texW = renderer.textureWidth;
		float texH = renderer.textureHeight;
		this.quadList[0] = new TexturedQuad(new PositionTextureVertex[]{maxXMinYMaxZ, maxXMinYMinZ, maxXMaxYMinZ, maxXMaxYMaxZ}, textureX + depth + width, textureY + depth, textureX + depth + width + depth, textureY + depth + height, texW, texH);
		this.quadList[1] = new TexturedQuad(new PositionTextureVertex[]{minXMinYMinZ, minXMinYMaxZ, minXMaxYMaxZ, minXMaxYMinZ}, textureX, textureY + depth, textureX + depth, textureY + depth + height, texW, texH);
		this.quadList[2] = new TexturedQuad(new PositionTextureVertex[]{maxXMinYMaxZ, minXMinYMaxZ, minXMinYMinZ, maxXMinYMinZ}, textureX + depth, textureY, textureX + depth + width, textureY + depth, texW, texH);
		this.quadList[3] = new TexturedQuad(new PositionTextureVertex[]{maxXMaxYMinZ, minXMaxYMinZ, minXMaxYMaxZ, maxXMaxYMaxZ}, textureX + depth + width, textureY + depth, textureX + depth + width + width, textureY, texW, texH);
		this.quadList[4] = new TexturedQuad(new PositionTextureVertex[]{maxXMinYMinZ, minXMinYMinZ, minXMaxYMinZ, maxXMaxYMinZ}, textureX + depth, textureY + depth, textureX + depth + width, textureY + depth + height, texW, texH);
		this.quadList[5] = new TexturedQuad(new PositionTextureVertex[]{minXMinYMaxZ, maxXMinYMaxZ, maxXMaxYMaxZ, minXMaxYMaxZ}, textureX + depth + width + depth, textureY + depth, textureX + depth + width + depth + width, textureY + depth + height, texW, texH);
		if(renderer.mirror) {
			for(int i = 0; i < this.quadList.length; ++i) {
				this.quadList[i].flipFace();
			}
		}
	}

	/**
	 * Draws every face of the box into the given tessellator, scaled by
	 * {@code scale}. The face normals are computed by {@link TexturedQuad#draw}
	 * so lighting is correct.
	 */
	public void render(Tessellator tessellator, float scale) {
		for(int i = 0; i < this.quadList.length; ++i) {
			this.quadList[i].draw(tessellator, scale);
		}
	}

	/**
	 * Tags this box with a name so a renderer can look it up by name later
	 * (used by zombie headwear and other overlay parts).
	 */
	public ModelBox setBoxName(String name) {
		this.boxName = name;
		return this;
	}
}
