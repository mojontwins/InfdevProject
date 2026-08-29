package net.minecraft.client.render.block;

import net.minecraft.client.render.RenderBlocks;
import net.minecraft.client.render.Tessellator;
import net.minecraft.game.world.IBlockAccess;
import net.minecraft.game.world.block.Block;
import util.AtlasUV;
import util.TextureAtlas;

/**
 * Render type 20 — vines, ported from the Beta 1.7.3 renderer. Each metadata
 * bit (1 south, 2 north, 4 east, 8 west in the B1.7.3 scheme) hangs a
 * two-sided leaf panel one twentieth of a block off the corresponding wall;
 * when the cube above is solid, the vine also closes its top with an eave.
 * The colour multiplier of later versions is absent here, so the panels take
 * the block's own brightness, tinted plain white.
 */
public final class RenderBlockVine implements BlockRenderHandler {
	@Override
	public final boolean renderBlock(RenderBlocks renderBlocks, Block block, int x, int y, int z) {
		IBlockAccess blockAccess = renderBlocks.blockAccess;
		Tessellator tessellator = Tessellator.instance;
		int textureId = block.getBlockTextureFromSide(0);
		if(renderBlocks.overrideBlockTexture >= 0) {
			textureId = renderBlocks.overrideBlockTexture;
		}

		float brightness = block.getBlockBrightness(blockAccess, x, y, z);
		tessellator.setColorOpaque_F(brightness, brightness, brightness);
		AtlasUV.calc(textureId, TextureAtlas.TERRAIN);
		double uLo = AtlasUV.u1;
		double uHi = AtlasUV.u2;
		double vLo = AtlasUV.v1;
		double vHi = AtlasUV.v2;
		double inset = 0.05D;
		int metadata = blockAccess.getBlockMetadata(x, y, z);
		if((metadata & 2) != 0) {
			// Panel hanging on the north side (-Z wall).
			tessellator.addVertexWithUV((double)x + inset, y + 1, z + 1, uLo, vLo);
			tessellator.addVertexWithUV((double)x + inset, y, z + 1, uLo, vHi);
			tessellator.addVertexWithUV((double)x + inset, y, z, uHi, vHi);
			tessellator.addVertexWithUV((double)x + inset, y + 1, z, uHi, vLo);
			tessellator.addVertexWithUV((double)x + inset, y + 1, z, uHi, vLo);
			tessellator.addVertexWithUV((double)x + inset, y, z, uHi, vHi);
			tessellator.addVertexWithUV((double)x + inset, y, z + 1, uLo, vHi);
			tessellator.addVertexWithUV((double)x + inset, y + 1, z + 1, uLo, vLo);
		}

		if((metadata & 8) != 0) {
			// Panel hanging on the south side (+Z wall).
			tessellator.addVertexWithUV((double)(x + 1) - inset, y, z + 1, uHi, vHi);
			tessellator.addVertexWithUV((double)(x + 1) - inset, y + 1, z + 1, uHi, vLo);
			tessellator.addVertexWithUV((double)(x + 1) - inset, y + 1, z, uLo, vLo);
			tessellator.addVertexWithUV((double)(x + 1) - inset, y, z, uLo, vHi);
			tessellator.addVertexWithUV((double)(x + 1) - inset, y, z, uLo, vHi);
			tessellator.addVertexWithUV((double)(x + 1) - inset, y + 1, z, uLo, vLo);
			tessellator.addVertexWithUV((double)(x + 1) - inset, y + 1, z + 1, uHi, vLo);
			tessellator.addVertexWithUV((double)(x + 1) - inset, y, z + 1, uHi, vHi);
		}

		if((metadata & 4) != 0) {
			// Panel hanging on the west side (-X wall).
			tessellator.addVertexWithUV(x + 1, y, (double)z + inset, uHi, vHi);
			tessellator.addVertexWithUV(x + 1, y + 1, (double)z + inset, uHi, vLo);
			tessellator.addVertexWithUV(x, y + 1, (double)z + inset, uLo, vLo);
			tessellator.addVertexWithUV(x, y, (double)z + inset, uLo, vHi);
			tessellator.addVertexWithUV(x, y, (double)z + inset, uLo, vHi);
			tessellator.addVertexWithUV(x, y + 1, (double)z + inset, uLo, vLo);
			tessellator.addVertexWithUV(x + 1, y + 1, (double)z + inset, uHi, vLo);
			tessellator.addVertexWithUV(x + 1, y, (double)z + inset, uHi, vHi);
		}

		if((metadata & 1) != 0) {
			// Panel hanging on the east side (+X wall).
			tessellator.addVertexWithUV(x + 1, y + 1, (double)(z + 1) - inset, uLo, vLo);
			tessellator.addVertexWithUV(x + 1, y, (double)(z + 1) - inset, uLo, vHi);
			tessellator.addVertexWithUV(x, y, (double)(z + 1) - inset, uHi, vHi);
			tessellator.addVertexWithUV(x, y + 1, (double)(z + 1) - inset, uHi, vLo);
			tessellator.addVertexWithUV(x, y + 1, (double)(z + 1) - inset, uHi, vLo);
			tessellator.addVertexWithUV(x, y, (double)(z + 1) - inset, uHi, vHi);
			tessellator.addVertexWithUV(x + 1, y, (double)(z + 1) - inset, uLo, vHi);
			tessellator.addVertexWithUV(x + 1, y + 1, (double)(z + 1) - inset, uLo, vLo);
		}

		if(blockAccess.isSolid(x, y + 1, z)) {
			// Eave closing the top whenever a cube sits above the vine.
			tessellator.addVertexWithUV((double)(x + 1), (double)(y + 1) - inset, (double)z, uLo, vLo);
			tessellator.addVertexWithUV((double)(x + 1), (double)(y + 1) - inset, (double)(z + 1), uLo, vHi);
			tessellator.addVertexWithUV((double)x, (double)(y + 1) - inset, (double)(z + 1), uHi, vHi);
			tessellator.addVertexWithUV((double)x, (double)(y + 1) - inset, (double)z, uHi, vLo);
		}

		return true;
	}
}