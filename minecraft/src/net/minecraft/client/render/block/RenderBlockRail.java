package net.minecraft.client.render.block;

import net.minecraft.client.render.RenderBlocks;
import net.minecraft.client.render.Tessellator;
import net.minecraft.game.world.block.Block;
import util.AtlasUV;
import util.TextureAtlas;

/**
 * Render type 9 — minecart track, ported from the Alpha 1.1.2 renderer. The
 * track is a single two-sided quad one sixteenth of a block above its cell
 * floor; metadata 0-7 are the straight/curved layouts, 8/9 swap the quad to
 * the opposite diagonal, and curved pieces lift the two angled corners.
 */
public final class RenderBlockRail implements BlockRenderHandler {
	@Override
	public final boolean renderBlock(RenderBlocks renderBlocks, Block block, int x, int y, int z) {
		Tessellator tessellator = Tessellator.instance;
		int metadata = renderBlocks.blockAccess.getBlockMetadata(x, y, z);
		int textureId = block.getBlockTextureFromSideAndMetadata(0, metadata);
		if(renderBlocks.overrideBlockTexture >= 0) {
			textureId = renderBlocks.overrideBlockTexture;
		}

		float brightness = block.getBlockBrightness(renderBlocks.blockAccess, x, y, z);
		tessellator.setColorOpaque_F(brightness, brightness, brightness);
		AtlasUV.calc(textureId, TextureAtlas.TERRAIN);
		double uLo = AtlasUV.u1;
		double uHi = AtlasUV.u2;
		double vLo = AtlasUV.v1;
		double vHi = AtlasUV.v2;
		float height = 1.0F / 16.0F;
		float x0 = (float)(x + 1);
		float x1 = (float)(x + 1);
		float x2 = (float)x;
		float x3 = (float)x;
		float z0 = (float)z;
		float z1 = (float)(z + 1);
		float z2 = (float)(z + 1);
		float z3 = (float)z;
		float levelA = (float)y + height;
		float levelB = (float)y + height;
		float levelC = (float)y + height;
		float levelD = (float)y + height;
		if(metadata != 1 && metadata != 2 && metadata != 3 && metadata != 7) {
			if(metadata == 8) {
				x1 = (float)x;
				x0 = x1;
				x3 = (float)(x + 1);
				x2 = x3;
				z3 = (float)(z + 1);
				z0 = z3;
				z2 = (float)z;
				z1 = z2;
			} else if(metadata == 9) {
				x3 = (float)x;
				x0 = x3;
				x2 = (float)(x + 1);
				x1 = x2;
				z1 = (float)z;
				z0 = z1;
				z3 = (float)(z + 1);
				z2 = z3;
			}
		} else {
			x3 = (float)(x + 1);
			x0 = x3;
			x2 = (float)x;
			x1 = x2;
			z1 = (float)(z + 1);
			z0 = z1;
			z3 = (float)z;
			z2 = z3;
		}

		if(metadata != 2 && metadata != 4) {
			if(metadata == 3 || metadata == 5) {
				++levelB;
				++levelC;
			}
		} else {
			++levelA;
			++levelD;
		}

		tessellator.addVertexWithUV((double)x0, (double)levelA, (double)z0, uHi, vLo);
		tessellator.addVertexWithUV((double)x1, (double)levelB, (double)z1, uHi, vHi);
		tessellator.addVertexWithUV((double)x2, (double)levelC, (double)z2, uLo, vHi);
		tessellator.addVertexWithUV((double)x3, (double)levelD, (double)z3, uLo, vLo);
		tessellator.addVertexWithUV((double)x3, (double)levelD, (double)z3, uLo, vLo);
		tessellator.addVertexWithUV((double)x2, (double)levelC, (double)z2, uLo, vHi);
		tessellator.addVertexWithUV((double)x1, (double)levelB, (double)z1, uHi, vHi);
		tessellator.addVertexWithUV((double)x0, (double)levelA, (double)z0, uHi, vLo);
		return true;
	}
}