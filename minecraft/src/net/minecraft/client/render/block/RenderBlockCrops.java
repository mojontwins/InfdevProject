package net.minecraft.client.render.block;

import net.minecraft.client.render.RenderBlocks;
import net.minecraft.client.render.Tessellator;
import net.minecraft.game.world.block.Block;
import util.AtlasUV;
import util.TextureAtlas;

/**
 * Render type 6 — crops: a double cross (both X- and Z-parallel planes) with
 * the growth-stage tile chosen via metadata. Plants sit one sixteenth of a
 * block below their cell so the stems meet the tilled soil. Also used for the
 * inventory preview.
 */
public final class RenderBlockCrops implements BlockRenderHandler {
	@Override
	public final boolean renderBlock(RenderBlocks renderBlocks, Block block, int x, int y, int z) {
		Tessellator tessellator = Tessellator.instance;
		float brightness = block.getBlockBrightness(renderBlocks.blockAccess, x, y, z);
		tessellator.setColorOpaque_F(brightness, brightness, brightness);
		this.renderBlockCrops(renderBlocks, block, renderBlocks.blockAccess.getBlockMetadata(x, y, z), x, (double)((float)y - 1.0F / 16.0F), z);
		return true;
	}

	@Override
	public final void renderBlockOnInventory(RenderBlocks renderBlocks, Block block, int metadata) {
		Tessellator tessellator = Tessellator.instance;
		tessellator.startDrawingQuads();
		Tessellator.setNormal(0.0F, -1.0F, 0.0F);
		this.renderBlockCrops(renderBlocks, block, -1, -0.5D, -0.5D, -0.5D);
		tessellator.draw();
	}

	private void renderBlockCrops(RenderBlocks renderBlocks, Block block, int metadata, double x, double y, double z) {
		Tessellator tessellator = Tessellator.instance;
		int textureId = block.getBlockTextureFromSideAndMetadata(0, metadata);
		if(renderBlocks.overrideBlockTexture >= 0) {
			textureId = renderBlocks.overrideBlockTexture;
		}

		AtlasUV.calc(textureId, TextureAtlas.TERRAIN);
		double uLo = AtlasUV.u1;
		double uHi = AtlasUV.u2;
		double vLo = AtlasUV.v1;
		double vHi = AtlasUV.v2;
		// X-parallel planes, half a block deep in Z.
		double xLow = x + 0.5D - 0.25D;
		double xHigh = x + 0.5D + 0.25D;
		double zLow = z + 0.5D - 0.5D;
		double zHigh = z + 0.5D + 0.5D;
		tessellator.addVertexWithUV(xLow, y + 1.0D, zLow, uLo, vLo);
		tessellator.addVertexWithUV(xLow, y, zLow, uLo, vHi);
		tessellator.addVertexWithUV(xLow, y, zHigh, uHi, vHi);
		tessellator.addVertexWithUV(xLow, y + 1.0D, zHigh, uHi, vLo);
		tessellator.addVertexWithUV(xLow, y + 1.0D, zHigh, uLo, vLo);
		tessellator.addVertexWithUV(xLow, y, zHigh, uLo, vHi);
		tessellator.addVertexWithUV(xLow, y, zLow, uHi, vHi);
		tessellator.addVertexWithUV(xLow, y + 1.0D, zLow, uHi, vLo);
		tessellator.addVertexWithUV(xHigh, y + 1.0D, zHigh, uLo, vLo);
		tessellator.addVertexWithUV(xHigh, y, zHigh, uLo, vHi);
		tessellator.addVertexWithUV(xHigh, y, zLow, uHi, vHi);
		tessellator.addVertexWithUV(xHigh, y + 1.0D, zLow, uHi, vLo);
		tessellator.addVertexWithUV(xHigh, y + 1.0D, zLow, uLo, vLo);
		tessellator.addVertexWithUV(xHigh, y, zLow, uLo, vHi);
		tessellator.addVertexWithUV(xHigh, y, zHigh, uHi, vHi);
		tessellator.addVertexWithUV(xHigh, y + 1.0D, zHigh, uHi, vLo);
		// Z-parallel planes, half a block deep in X.
		xLow = x + 0.5D - 0.5D;
		xHigh = x + 0.5D + 0.5D;
		zLow = z + 0.5D - 0.25D;
		zHigh = z + 0.5D + 0.25D;
		tessellator.addVertexWithUV(xLow, y + 1.0D, zLow, uLo, vLo);
		tessellator.addVertexWithUV(xLow, y, zLow, uLo, vHi);
		tessellator.addVertexWithUV(xHigh, y, zLow, uHi, vHi);
		tessellator.addVertexWithUV(xHigh, y + 1.0D, zLow, uHi, vLo);
		tessellator.addVertexWithUV(xHigh, y + 1.0D, zLow, uLo, vLo);
		tessellator.addVertexWithUV(xHigh, y, zLow, uLo, vHi);
		tessellator.addVertexWithUV(xLow, y, zLow, uHi, vHi);
		tessellator.addVertexWithUV(xLow, y + 1.0D, zLow, uHi, vLo);
		tessellator.addVertexWithUV(xHigh, y + 1.0D, zHigh, uLo, vLo);
		tessellator.addVertexWithUV(xHigh, y, zHigh, uLo, vHi);
		tessellator.addVertexWithUV(xLow, y, zHigh, uHi, vHi);
		tessellator.addVertexWithUV(xLow, y + 1.0D, zHigh, uHi, vLo);
		tessellator.addVertexWithUV(xLow, y + 1.0D, zHigh, uLo, vLo);
		tessellator.addVertexWithUV(xLow, y, zHigh, uLo, vHi);
		tessellator.addVertexWithUV(xHigh, y, zHigh, uHi, vHi);
		tessellator.addVertexWithUV(xHigh, y + 1.0D, zHigh, uHi, vLo);
	}
}