package net.minecraft.client.render.block;

import net.minecraft.client.render.RenderBlocks;
import net.minecraft.client.render.Tessellator;
import net.minecraft.game.world.IBlockAccess;
import net.minecraft.game.world.block.Block;
import util.AtlasUV;
import util.TextureAtlas;

/**
 * Render type 3 — fire. Contrary to the usual coordinate ordering in this
 * handler x/y/z keep their original meaning below: the flame spreads into each
 * burnable neighbour (a couple of crossed two-sided quads hovered slightly
 * above the floor) and licks up the block above, using the alternate texture
 * row for the flicker tile.
 */
public final class RenderBlockFire implements BlockRenderHandler {
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
		AtlasUV.calc(textureId, TextureAtlas.TERRAIN);
		// The U span is constant for the whole flame; the two texture rows swap in V.
		double uFullLo = AtlasUV.u1;
		double uFullHi = AtlasUV.u2;
		double vRowLo = AtlasUV.v1;
		double vRowHi = AtlasUV.v2;
		// The flicker tile sits one tile row below the main fire tile.
		double vFlickerLo = AtlasUV.v1 + (double)TextureAtlas.TILE / (double)TextureAtlas.TERRAIN.height;
		double vFlickerHi = AtlasUV.v2 + (double)TextureAtlas.TILE / (double)TextureAtlas.TERRAIN.height;
		double uLo = uFullLo;
		double uHi = uFullHi;
		double vLo = vRowLo;
		double vHi = vRowHi;
		double uTmp;
		if(!blockAccess.isSolid(x, y - 1, z) && !Block.fire.canBlockCatchFire(blockAccess, x, y - 1, z)) {
			// Small fire: crossed two-sided quads flickering between texture rows.
			if((x + y + z & 1) == 1) {
				uLo = uFullLo;
				uHi = uFullHi;
				vLo = vFlickerLo;
				vHi = vFlickerHi;
			}

			if((x / 2 + y / 2 + z / 2 & 1) == 1) {
				uTmp = uHi;
				uHi = uLo;
				uLo = uTmp;
			}

			// Flames spill out through each burnable neighbour.
			if(Block.fire.canBlockCatchFire(blockAccess, x - 1, y, z)) {
				tessellator.addVertexWithUV((double)((float)x + 0.2F), (double)((float)y + 1.4F + 1.0F / 16.0F), (double)(z + 1), uHi, vLo);
				tessellator.addVertexWithUV((double)x, (double)((float)y + 1.0F / 16.0F), (double)(z + 1), uHi, vHi);
				tessellator.addVertexWithUV((double)x, (double)((float)y + 1.0F / 16.0F), (double)z, uLo, vHi);
				tessellator.addVertexWithUV((double)((float)x + 0.2F), (double)((float)y + 1.4F + 1.0F / 16.0F), (double)z, uLo, vLo);
				tessellator.addVertexWithUV((double)((float)x + 0.2F), (double)((float)y + 1.4F + 1.0F / 16.0F), (double)z, uLo, vLo);
				tessellator.addVertexWithUV((double)x, (double)((float)y + 1.0F / 16.0F), (double)z, uLo, vHi);
				tessellator.addVertexWithUV((double)x, (double)((float)y + 1.0F / 16.0F), (double)(z + 1), uHi, vHi);
				tessellator.addVertexWithUV((double)((float)x + 0.2F), (double)((float)y + 1.4F + 1.0F / 16.0F), (double)(z + 1), uHi, vLo);
			}

			if(Block.fire.canBlockCatchFire(blockAccess, x + 1, y, z)) {
				tessellator.addVertexWithUV((double)((float)(x + 1) - 0.2F), (double)((float)y + 1.4F + 1.0F / 16.0F), (double)z, uLo, vLo);
				tessellator.addVertexWithUV((double)(x + 1), (double)((float)y + 1.0F / 16.0F), (double)z, uLo, vHi);
				tessellator.addVertexWithUV((double)(x + 1), (double)((float)y + 1.0F / 16.0F), (double)(z + 1), uHi, vHi);
				tessellator.addVertexWithUV((double)((float)(x + 1) - 0.2F), (double)((float)y + 1.4F + 1.0F / 16.0F), (double)(z + 1), uHi, vLo);
				tessellator.addVertexWithUV((double)((float)(x + 1) - 0.2F), (double)((float)y + 1.4F + 1.0F / 16.0F), (double)(z + 1), uHi, vLo);
				tessellator.addVertexWithUV((double)(x + 1), (double)((float)y + 1.0F / 16.0F), (double)(z + 1), uHi, vHi);
				tessellator.addVertexWithUV((double)(x + 1), (double)((float)y + 1.0F / 16.0F), (double)z, uLo, vHi);
				tessellator.addVertexWithUV((double)((float)(x + 1) - 0.2F), (double)((float)y + 1.4F + 1.0F / 16.0F), (double)z, uLo, vLo);
			}

			if(Block.fire.canBlockCatchFire(blockAccess, x, y, z - 1)) {
				tessellator.addVertexWithUV((double)x, (double)((float)y + 1.4F + 1.0F / 16.0F), (double)((float)z + 0.2F), uHi, vLo);
				tessellator.addVertexWithUV((double)x, (double)((float)y + 1.0F / 16.0F), (double)z, uHi, vHi);
				tessellator.addVertexWithUV((double)(x + 1), (double)((float)y + 1.0F / 16.0F), (double)z, uLo, vHi);
				tessellator.addVertexWithUV((double)(x + 1), (double)((float)y + 1.4F + 1.0F / 16.0F), (double)((float)z + 0.2F), uLo, vLo);
				tessellator.addVertexWithUV((double)(x + 1), (double)((float)y + 1.4F + 1.0F / 16.0F), (double)((float)z + 0.2F), uLo, vLo);
				tessellator.addVertexWithUV((double)(x + 1), (double)((float)y + 1.0F / 16.0F), (double)z, uLo, vHi);
				tessellator.addVertexWithUV((double)x, (double)((float)y + 1.0F / 16.0F), (double)z, uHi, vHi);
				tessellator.addVertexWithUV((double)x, (double)((float)y + 1.4F + 1.0F / 16.0F), (double)((float)z + 0.2F), uHi, vLo);
			}

			if(Block.fire.canBlockCatchFire(blockAccess, x, y, z + 1)) {
				tessellator.addVertexWithUV((double)(x + 1), (double)((float)y + 1.4F + 1.0F / 16.0F), (double)((float)(z + 1) - 0.2F), uLo, vLo);
				tessellator.addVertexWithUV((double)(x + 1), (double)((float)y + 1.0F / 16.0F), (double)(z + 1), uLo, vHi);
				tessellator.addVertexWithUV((double)x, (double)((float)y + 1.0F / 16.0F), (double)(z + 1), uHi, vHi);
				tessellator.addVertexWithUV((double)x, (double)((float)y + 1.4F + 1.0F / 16.0F), (double)((float)(z + 1) - 0.2F), uHi, vLo);
				tessellator.addVertexWithUV((double)x, (double)((float)y + 1.4F + 1.0F / 16.0F), (double)((float)(z + 1) - 0.2F), uHi, vLo);
				tessellator.addVertexWithUV((double)x, (double)((float)y + 1.0F / 16.0F), (double)(z + 1), uHi, vHi);
				tessellator.addVertexWithUV((double)(x + 1), (double)((float)y + 1.0F / 16.0F), (double)(z + 1), uLo, vHi);
				tessellator.addVertexWithUV((double)(x + 1), (double)((float)y + 1.4F + 1.0F / 16.0F), (double)((float)(z + 1) - 0.2F), uLo, vLo);
			}

			// Tiny licking flame on top of the block.
			if(Block.fire.canBlockCatchFire(blockAccess, x, y + 1, z)) {
				double xHigh = (double)x + 0.5D + 0.5D;
				double xLow = (double)x + 0.5D - 0.5D;
				double zHigh = (double)z + 0.5D + 0.5D;
				double zLow = (double)z + 0.5D - 0.5D;
				uLo = uFullLo;
				uHi = uFullHi;
				vLo = vRowLo;
				vHi = vRowHi;
				int flameY = y + 1;
				if((x + flameY + z & 1) == 0) {
					tessellator.addVertexWithUV(xLow, (double)((float)flameY + -0.2F), (double)z, uHi, vLo);
					tessellator.addVertexWithUV(xHigh, (double)flameY, (double)z, uHi, vHi);
					tessellator.addVertexWithUV(xHigh, (double)flameY, (double)(z + 1), uLo, vHi);
					tessellator.addVertexWithUV(xLow, (double)((float)flameY + -0.2F), (double)(z + 1), uLo, vLo);
					uLo = uFullLo;
					uHi = uFullHi;
					vLo = vFlickerLo;
					vHi = vFlickerHi;
					tessellator.addVertexWithUV(xHigh, (double)((float)flameY + -0.2F), (double)(z + 1), uHi, vLo);
					tessellator.addVertexWithUV(xLow, (double)flameY, (double)(z + 1), uHi, vHi);
					tessellator.addVertexWithUV(xLow, (double)flameY, (double)z, uLo, vHi);
					tessellator.addVertexWithUV(xHigh, (double)((float)flameY + -0.2F), (double)z, uLo, vLo);
				} else {
					tessellator.addVertexWithUV((double)x, (double)((float)flameY + -0.2F), zHigh, uHi, vLo);
					tessellator.addVertexWithUV((double)x, (double)flameY, zLow, uHi, vHi);
					tessellator.addVertexWithUV((double)(x + 1), (double)flameY, zLow, uLo, vHi);
					tessellator.addVertexWithUV((double)(x + 1), (double)((float)flameY + -0.2F), zHigh, uLo, vLo);
					uLo = uFullLo;
					uHi = uFullHi;
					vLo = vFlickerLo;
					vHi = vFlickerHi;
					tessellator.addVertexWithUV((double)(x + 1), (double)((float)flameY + -0.2F), zLow, uHi, vLo);
					tessellator.addVertexWithUV((double)(x + 1), (double)flameY, zHigh, uHi, vHi);
					tessellator.addVertexWithUV((double)x, (double)flameY, zHigh, uLo, vHi);
					tessellator.addVertexWithUV((double)x, (double)((float)flameY + -0.2F), zLow, uLo, vLo);
				}
			}
		} else {
			// Full fire: four crossed oversized quads around a lean, notched
			// top, using both texture rows for flicker.
			double xHigh = (double)x + 0.5D + 0.2D;
			double xLow = (double)x + 0.5D - 0.2D;
			double zHigh = (double)z + 0.5D + 0.2D;
			double zLow = (double)z + 0.5D - 0.2D;
			uTmp = (double)x + 0.5D - 0.3D;
			double xWideHigh = (double)x + 0.5D + 0.3D;
			double zWideLow = (double)z + 0.5D - 0.3D;
			double zWideHigh = (double)z + 0.5D + 0.3D;
			tessellator.addVertexWithUV(uTmp, (double)((float)y + 1.4F), (double)(z + 1), uHi, vLo);
			tessellator.addVertexWithUV(xHigh, (double)y, (double)(z + 1), uHi, vHi);
			tessellator.addVertexWithUV(xHigh, (double)y, (double)z, uLo, vHi);
			tessellator.addVertexWithUV(uTmp, (double)((float)y + 1.4F), (double)z, uLo, vLo);
			tessellator.addVertexWithUV(xWideHigh, (double)((float)y + 1.4F), (double)z, uHi, vLo);
			tessellator.addVertexWithUV(xLow, (double)y, (double)z, uHi, vHi);
			tessellator.addVertexWithUV(xLow, (double)y, (double)(z + 1), uLo, vHi);
			tessellator.addVertexWithUV(xWideHigh, (double)((float)y + 1.4F), (double)(z + 1), uLo, vLo);
			uLo = uFullLo;
			uHi = uFullHi;
			vLo = vFlickerLo;
			vHi = vFlickerHi;
			tessellator.addVertexWithUV((double)(x + 1), (double)((float)y + 1.4F), zWideHigh, uHi, vLo);
			tessellator.addVertexWithUV((double)(x + 1), (double)y, zLow, uHi, vHi);
			tessellator.addVertexWithUV((double)x, (double)y, zLow, uLo, vHi);
			tessellator.addVertexWithUV((double)x, (double)((float)y + 1.4F), zWideHigh, uLo, vLo);
			tessellator.addVertexWithUV((double)x, (double)((float)y + 1.4F), zWideLow, uHi, vLo);
			tessellator.addVertexWithUV((double)x, (double)y, zHigh, uHi, vHi);
			tessellator.addVertexWithUV((double)(x + 1), (double)y, zHigh, uLo, vHi);
			tessellator.addVertexWithUV((double)(x + 1), (double)((float)y + 1.4F), zWideLow, uLo, vLo);
			xHigh = (double)x + 0.5D - 0.5D;
			xLow = (double)x + 0.5D + 0.5D;
			zHigh = (double)z + 0.5D - 0.5D;
			zLow = (double)z + 0.5D + 0.5D;
			uTmp = (double)x + 0.5D - 0.4D;
			xWideHigh = (double)x + 0.5D + 0.4D;
			zWideLow = (double)z + 0.5D - 0.4D;
			zWideHigh = (double)z + 0.5D + 0.4D;
			uLo = uFullLo;
			uHi = uFullHi;
			vLo = vRowLo;
			vHi = vRowHi;
			tessellator.addVertexWithUV(uTmp, (double)((float)y + 1.4F), (double)z, uLo, vLo);
			tessellator.addVertexWithUV(xHigh, (double)y, (double)z, uLo, vHi);
			tessellator.addVertexWithUV(xHigh, (double)y, (double)(z + 1), uHi, vHi);
			tessellator.addVertexWithUV(uTmp, (double)((float)y + 1.4F), (double)(z + 1), uHi, vLo);
			tessellator.addVertexWithUV(xWideHigh, (double)((float)y + 1.4F), (double)(z + 1), uLo, vLo);
			tessellator.addVertexWithUV(xLow, (double)y, (double)(z + 1), uLo, vHi);
			tessellator.addVertexWithUV(xLow, (double)y, (double)z, uHi, vHi);
			tessellator.addVertexWithUV(xWideHigh, (double)((float)y + 1.4F), (double)z, uHi, vLo);
			uLo = uFullLo;
			uHi = uFullHi;
			vLo = vFlickerLo;
			vHi = vFlickerHi;
			tessellator.addVertexWithUV((double)x, (double)((float)y + 1.4F), zWideHigh, uLo, vLo);
			tessellator.addVertexWithUV((double)x, (double)y, zLow, uLo, vHi);
			tessellator.addVertexWithUV((double)(x + 1), (double)y, zLow, uHi, vHi);
			tessellator.addVertexWithUV((double)(x + 1), (double)((float)y + 1.4F), zWideHigh, uHi, vLo);
			tessellator.addVertexWithUV((double)(x + 1), (double)((float)y + 1.4F), zWideLow, uLo, vLo);
			tessellator.addVertexWithUV((double)(x + 1), (double)y, zHigh, uLo, vHi);
			tessellator.addVertexWithUV((double)x, (double)y, zHigh, uHi, vHi);
			tessellator.addVertexWithUV((double)x, (double)((float)y + 1.4F), zWideLow, uHi, vLo);
		}

		return true;
	}
}