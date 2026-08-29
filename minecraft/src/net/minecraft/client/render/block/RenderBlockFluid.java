package net.minecraft.client.render.block;

import net.minecraft.client.render.RenderBlocks;
import net.minecraft.client.render.Tessellator;
import net.minecraft.game.world.IBlockAccess;
import net.minecraft.game.world.block.Block;
import net.minecraft.game.world.material.Material;

/**
 * Render type 4 — liquids (water/lava). The top surface sits at the liquid
 * level while the four side walls run down from this cell's level to each
 * neighbour's, so a fully contained source is only drawn as a flat slab.
 *
 * The vanilla block set in this version does not produce this render type, but
 * it is kept for fidelity and for blocks that opt in via {@code getRenderType()
 * == 4}. The shared block bounds are temporarily rewritten per neighbour and
 * always restored.
 */
public final class RenderBlockFluid implements BlockRenderHandler {
	@Override
	public final boolean renderBlock(RenderBlocks renderBlocks, Block block, int x, int y, int z) {
		IBlockAccess blockAccess = renderBlocks.blockAccess;
		Tessellator tessellator = Tessellator.instance;
		boolean anyFaceRendered = false;
		double savedMinY = block.minY;
		double savedMaxY = block.maxY;
		block.maxY = savedMaxY - (double)RenderBlockFluid.materialNotWater(blockAccess, x, y, z);
		if(renderBlocks.flipTexture || block.shouldSideBeRendered(blockAccess, x, y - 1, z, 0)) {
			float sideBrightness = block.getBlockBrightness(blockAccess, x, y - 1, z);
			tessellator.setColorOpaque_F(0.5F * sideBrightness, 0.5F * sideBrightness, 0.5F * sideBrightness);
			renderBlocks.renderFace(block, 0, x, y, z, block.getBlockTextureFromSide(0));
			anyFaceRendered = true;
		}

		if(renderBlocks.flipTexture || block.shouldSideBeRendered(blockAccess, x, y + 1, z, 1)) {
			float sideBrightness = block.getBlockBrightness(blockAccess, x, y + 1, z);
			tessellator.setColorOpaque_F(sideBrightness, sideBrightness, sideBrightness);
			renderBlocks.renderFace(block, 1, x, y, z, block.getBlockTextureFromSide(1));
			anyFaceRendered = true;
		}

		for(int i = 0; i < 4; ++i) {
			int side = 2 + i;
			int[] neighbor = RenderBlocks.NEIGHBOR_OFFSETS[side];
			block.minY = savedMaxY - (double)RenderBlockFluid.materialNotWater(blockAccess, x + neighbor[0], y + neighbor[1], z + neighbor[2]);
			if(renderBlocks.flipTexture || block.maxY > block.minY || block.shouldSideBeRendered(blockAccess, x + neighbor[0], y + neighbor[1], z + neighbor[2], side)) {
				float sideBrightness = block.getBlockBrightness(blockAccess, x + neighbor[0], y + neighbor[1], z + neighbor[2]);
				float light = RenderBlocks.SIDE_LIGHT[side];
				tessellator.setColorOpaque_F(light * sideBrightness, light * sideBrightness, light * sideBrightness);
				renderBlocks.renderFace(block, side, x, y, z, block.getBlockTextureFromSide(side));
				anyFaceRendered = true;
			}
		}

		block.minY = savedMinY;
		block.maxY = savedMaxY;
		return anyFaceRendered;
	}

	/** How far a cell of this neighbour blocks the flow: 1.0 for solids, water level/9 for water. */
	private static float materialNotWater(IBlockAccess blockAccess, int x, int y, int z) {
		return blockAccess.getBlockMaterial(x, y, z) != Material.water ? 1.0F : (float)blockAccess.getBlockMetadata(x, y, z) / 9.0F;
	}
}