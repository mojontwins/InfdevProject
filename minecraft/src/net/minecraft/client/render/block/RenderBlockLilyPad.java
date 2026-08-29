package net.minecraft.client.render.block;

import net.minecraft.client.render.RenderBlocks;
import net.minecraft.client.render.Tessellator;
import net.minecraft.game.world.IBlockAccess;
import net.minecraft.game.world.block.Block;
import util.AtlasUV;
import util.TextureAtlas;

/**
 * Render type 23 — lily pad, ported from the Beta 1.7.3 renderer. The pad is a
 * rotated square grid of four tiles positioned by a per-cell hash, so the pads
 * never tile identically, and one cell in four gains a small flower, drawn as
 * crossed squares with the flower tile swapped in through the override.
 * Brightness is folded into the vertex colour, as everywhere in this version.
 */
public final class RenderBlockLilyPad implements BlockRenderHandler {
	@Override
	public final boolean renderBlock(RenderBlocks renderBlocks, Block block, int x, int y, int z) {
		IBlockAccess blockAccess = renderBlocks.blockAccess;
		Tessellator tessellator = Tessellator.instance;
		int textureId = block.blockIndexInTexture;
		float padElevation = 0.015625F;
		AtlasUV.calc(textureId, TextureAtlas.TERRAIN);
		float uLo = (float)AtlasUV.u1;
		float uHi = (float)AtlasUV.u2;
		float vLo = (float)AtlasUV.v1;
		float vHi = (float)AtlasUV.v2;
		long hash = (long)(x * 3129871) ^ (long)z * 116129781L ^ (long)y;
		hash = hash * hash * 42321461L + hash * 11L;
		int variant = (int)(hash >> 16 & 7L);
		float brightness = block.getBlockBrightness(blockAccess, x, y, z);
		tessellator.setColorOpaque_F(brightness, brightness, brightness);
		float centreX = (float)x + 0.5F;
		float centreZ = (float)z + 0.5F;
		float tiltA = (float)(variant & 1) * 0.5F * (float)(1 - (variant & 2));
		float tiltB = (float)(variant + 1 & 1) * 0.5F * (float)(1 - ((variant + 1) & 2));
		tessellator.addVertexWithUV((centreX + tiltA) - tiltB, (float)y + padElevation, centreZ + tiltA + tiltB, uLo, vLo);
		tessellator.addVertexWithUV(centreX + tiltA + tiltB, (float)y + padElevation, (centreZ - tiltA) + tiltB, uHi, vLo);
		tessellator.addVertexWithUV((centreX - tiltA) + tiltB, (float)y + padElevation, centreZ - tiltA - tiltB, uHi, vHi);
		tessellator.addVertexWithUV(centreX - tiltA - tiltB, (float)y + padElevation, (centreZ + tiltA) - tiltB, uLo, vHi);
		tessellator.addVertexWithUV(centreX - tiltA - tiltB, (float)y + padElevation, (centreZ + tiltA) - tiltB, uLo, vHi);
		tessellator.addVertexWithUV((centreX - tiltA) + tiltB, (float)y + padElevation, centreZ - tiltA - tiltB, uHi, vHi);
		tessellator.addVertexWithUV(centreX + tiltA + tiltB, (float)y + padElevation, (centreZ - tiltA) + tiltB, uHi, vLo);
		tessellator.addVertexWithUV((centreX + tiltA) - tiltB, (float)y + padElevation, centreZ + tiltA + tiltB, uLo, vLo);
		// One cell in four grows a small flower on top of the pad.
		if((variant & 4) != 0) {
			renderBlocks.overrideBlockTexture = 12 * 16 + 2;
			RenderBlockUtil.renderCrossedSquares(renderBlocks, block, 0, (double)x, (double)y, (double)z);
			renderBlocks.overrideBlockTexture = -1;
		}

		return true;
	}
}