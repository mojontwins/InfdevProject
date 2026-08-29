package net.minecraft.client.render.block;

import net.minecraft.client.render.RenderBlocks;
import net.minecraft.client.render.Tessellator;
import net.minecraft.game.world.block.Block;
import util.AtlasUV;
import util.TextureAtlas;

/**
 * Render type 8 — the wall-mounted ladder, ported from the Alpha 1.1.2
 * renderer. The ladder is a single two-sided panel floating 0.05 of a block
 * off the wall it hangs on, and the metadata selects which wall:
 * meta 5 = -X, 4 = +X, 3 = -Z, 2 = +Z (the same wall scheme as torches).
 */
public final class RenderBlockLadderWall implements BlockRenderHandler {
	@Override
	public final boolean renderBlock(RenderBlocks renderBlocks, Block block, int x, int y, int z) {
		Tessellator tessellator = Tessellator.instance;
		int textureId = block.getBlockTextureFromSide(0);
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
		int side = renderBlocks.blockAccess.getBlockMetadata(x, y, z);
		float overhang = 0.0F;
		float setBack = 0.05F;
		if(side == 5) {
			tessellator.addVertexWithUV((double)((float)x + setBack), (double)((float)(y + 1) + overhang), (double)((float)(z + 1) + overhang), uLo, vLo);
			tessellator.addVertexWithUV((double)((float)x + setBack), (double)((float)y - overhang), (double)((float)(z + 1) + overhang), uLo, vHi);
			tessellator.addVertexWithUV((double)((float)x + setBack), (double)((float)y - overhang), (double)((float)z - overhang), uHi, vHi);
			tessellator.addVertexWithUV((double)((float)x + setBack), (double)((float)(y + 1) + overhang), (double)((float)z - overhang), uHi, vLo);
		}

		if(side == 4) {
			tessellator.addVertexWithUV((double)((float)(x + 1) - setBack), (double)((float)y - overhang), (double)((float)(z + 1) + overhang), uHi, vHi);
			tessellator.addVertexWithUV((double)((float)(x + 1) - setBack), (double)((float)(y + 1) + overhang), (double)((float)(z + 1) + overhang), uHi, vLo);
			tessellator.addVertexWithUV((double)((float)(x + 1) - setBack), (double)((float)(y + 1) + overhang), (double)((float)z - overhang), uLo, vLo);
			tessellator.addVertexWithUV((double)((float)(x + 1) - setBack), (double)((float)y - overhang), (double)((float)z - overhang), uLo, vHi);
		}

		if(side == 3) {
			tessellator.addVertexWithUV((double)((float)(x + 1) + overhang), (double)((float)y - overhang), (double)((float)z + setBack), uHi, vHi);
			tessellator.addVertexWithUV((double)((float)(x + 1) + overhang), (double)((float)(y + 1) + overhang), (double)((float)z + setBack), uHi, vLo);
			tessellator.addVertexWithUV((double)((float)x - overhang), (double)((float)(y + 1) + overhang), (double)((float)z + setBack), uLo, vLo);
			tessellator.addVertexWithUV((double)((float)x - overhang), (double)((float)y - overhang), (double)((float)z + setBack), uLo, vHi);
		}

		if(side == 2) {
			tessellator.addVertexWithUV((double)((float)(x + 1) + overhang), (double)((float)(y + 1) + overhang), (double)((float)(z + 1) - setBack), uLo, vLo);
			tessellator.addVertexWithUV((double)((float)(x + 1) + overhang), (double)((float)y - overhang), (double)((float)(z + 1) - setBack), uLo, vHi);
			tessellator.addVertexWithUV((double)((float)x - overhang), (double)((float)y - overhang), (double)((float)(z + 1) - setBack), uHi, vHi);
			tessellator.addVertexWithUV((double)((float)x - overhang), (double)((float)(y + 1) + overhang), (double)((float)(z + 1) - setBack), uHi, vLo);
		}

		return true;
	}
}