package net.minecraft.client.render.block;

import net.minecraft.client.render.RenderBlocks;
import net.minecraft.client.render.Tessellator;
import net.minecraft.game.world.IBlockAccess;
import net.minecraft.game.world.block.Block;
import util.AtlasTexel;
import util.TexelScale;
import util.TextureAtlas;

/**
 * Render type 5 — thin wall-mounted panels (ladders and the gear block).
 * Two texture columns are used for the rungs and swapped by world parity, and
 * a panel is drawn for every neighbouring cell that is solid enough to attach
 * to. Four panels at once form the crossed gear.
 */
public final class RenderBlockLadder implements BlockRenderHandler {
	@Override
	public final boolean renderBlock(RenderBlocks renderBlocks, Block block, int x, int y, int z) {
		IBlockAccess blockAccess = renderBlocks.blockAccess;
		Tessellator tessellator = Tessellator.instance;
		int textureId = block.getBlockTextureFromSide(0);
		if(renderBlocks.overrideBlockTexture >= 0) {
			textureId = renderBlocks.overrideBlockTexture;
		}

		float torchBrightness = block.getBlockBrightness(blockAccess, x, y, z);
		tessellator.setColorOpaque_F(torchBrightness, torchBrightness, torchBrightness);
		TextureAtlas terrain = TextureAtlas.TERRAIN;
		AtlasTexel.calc(textureId, terrain);
		int uTile = AtlasTexel.u;
		int vTileRow = AtlasTexel.v;
		// Two side-by-side 16-pixel columns form a ladder tile; the parity swap
		// decides which column carries the rungs.
		int uLadderColumn = uTile + 16;
		int uBaseColumn = uTile;
		if((x + y + z & 1) == 1) {
			uLadderColumn = uTile;
			uBaseColumn = uTile + 16;
		}

		double uColLo = TexelScale.ud(terrain, uLadderColumn);
		double uColHi = TexelScale.ud(terrain, (double)uLadderColumn + terrain.tileSpan);
		double vTileLo = TexelScale.vd(terrain, vTileRow);
		double vTileHi = TexelScale.vd(terrain, (double)vTileRow + terrain.tileSpan);
		double uBaseLo = TexelScale.ud(terrain, uBaseColumn);
		double uBaseHi = TexelScale.ud(terrain, (double)uBaseColumn + terrain.tileSpan);
		double vBaseLo = vTileLo;
		double vBaseHi = vTileHi;
		if(blockAccess.isSolid(x - 1, y, z)) {
			tessellator.addVertexWithUV((double)((float)x + 0.05F), (double)((float)(y + 1) + 2.0F / 16.0F), (double)((float)(z + 1) + 2.0F / 16.0F), uColLo, vTileLo);
			tessellator.addVertexWithUV((double)((float)x + 0.05F), (double)((float)y - 2.0F / 16.0F), (double)((float)(z + 1) + 2.0F / 16.0F), uColLo, vTileHi);
			tessellator.addVertexWithUV((double)((float)x + 0.05F), (double)((float)y - 2.0F / 16.0F), (double)((float)z - 2.0F / 16.0F), uColHi, vTileHi);
			tessellator.addVertexWithUV((double)((float)x + 0.05F), (double)((float)(y + 1) + 2.0F / 16.0F), (double)((float)z - 2.0F / 16.0F), uColHi, vTileLo);
		}

		if(blockAccess.isSolid(x + 1, y, z)) {
			tessellator.addVertexWithUV((double)((float)(x + 1) - 0.05F), (double)((float)y - 2.0F / 16.0F), (double)((float)(z + 1) + 2.0F / 16.0F), uColHi, vTileHi);
			tessellator.addVertexWithUV((double)((float)(x + 1) - 0.05F), (double)((float)(y + 1) + 2.0F / 16.0F), (double)((float)(z + 1) + 2.0F / 16.0F), uColHi, vTileLo);
			tessellator.addVertexWithUV((double)((float)(x + 1) - 0.05F), (double)((float)(y + 1) + 2.0F / 16.0F), (double)((float)z - 2.0F / 16.0F), uColLo, vTileLo);
			tessellator.addVertexWithUV((double)((float)(x + 1) - 0.05F), (double)((float)y - 2.0F / 16.0F), (double)((float)z - 2.0F / 16.0F), uColLo, vTileHi);
		}

		if(blockAccess.isSolid(x, y, z - 1)) {
			tessellator.addVertexWithUV((double)((float)(x + 1) + 2.0F / 16.0F), (double)((float)y - 2.0F / 16.0F), (double)((float)z + 0.05F), uBaseHi, vBaseHi);
			tessellator.addVertexWithUV((double)((float)(x + 1) + 2.0F / 16.0F), (double)((float)(y + 1) + 2.0F / 16.0F), (double)((float)z + 0.05F), uBaseHi, vBaseLo);
			tessellator.addVertexWithUV((double)((float)x - 2.0F / 16.0F), (double)((float)(y + 1) + 2.0F / 16.0F), (double)((float)z + 0.05F), uBaseLo, vBaseLo);
			tessellator.addVertexWithUV((double)((float)x - 2.0F / 16.0F), (double)((float)y - 2.0F / 16.0F), (double)((float)z + 0.05F), uBaseLo, vBaseHi);
		}

		if(blockAccess.isSolid(x, y, z + 1)) {
			tessellator.addVertexWithUV((double)((float)(x + 1) + 2.0F / 16.0F), (double)((float)(y + 1) + 2.0F / 16.0F), (double)((float)(z + 1) - 0.05F), uBaseLo, vBaseLo);
			tessellator.addVertexWithUV((double)((float)(x + 1) + 2.0F / 16.0F), (double)((float)y - 2.0F / 16.0F), (double)((float)(z + 1) - 0.05F), uBaseLo, vBaseHi);
			tessellator.addVertexWithUV((double)((float)x - 2.0F / 16.0F), (double)((float)y - 2.0F / 16.0F), (double)((float)(z + 1) - 0.05F), uBaseHi, vBaseHi);
			tessellator.addVertexWithUV((double)((float)x - 2.0F / 16.0F), (double)((float)(y + 1) + 2.0F / 16.0F), (double)((float)(z + 1) - 0.05F), uBaseHi, vBaseLo);
		}

		return true;
	}
}