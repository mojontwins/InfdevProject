package net.minecraft.client.render.block;

import net.minecraft.client.render.RenderBlocks;
import net.minecraft.game.world.IBlockAccess;
import net.minecraft.game.world.block.Block;

/**
 * Render type 11 — fence, ported from the Alpha 1.1.2 renderer. A centre post
 * spans the full height, then a single horizontal rail is drawn on the X and Z
 * axes: a full-length rail when a fence continues that way, otherwise a short
 * stub between the post and the cell wall. Half of the outer face is skipped on
 * purpose so the rail seams line up with the neighbouring fence.
 */
public final class RenderBlockFence implements BlockRenderHandler {
	@Override
	public final boolean renderBlock(RenderBlocks renderBlocks, Block block, int x, int y, int z) {
		IBlockAccess blockAccess = renderBlocks.blockAccess;
		float railThickness = 6.0F / 16.0F;
		float railPadding = 10.0F / 16.0F;
		RenderBlockUtil.setBounds(block, railThickness, 0.0F, railThickness, railPadding, 1.0F, railPadding);
		RenderBlockUtil.renderStandardBlock(renderBlocks, block, x, y, z);
		boolean railX = blockAccess.getBlockId(x - 1, y, z) == block.blockID || blockAccess.getBlockId(x + 1, y, z) == block.blockID;
		boolean railZ = blockAccess.getBlockId(x, y, z - 1) == block.blockID || blockAccess.getBlockId(x, y, z + 1) == block.blockID;
		boolean west = blockAccess.getBlockId(x - 1, y, z) == block.blockID;
		boolean east = blockAccess.getBlockId(x + 1, y, z) == block.blockID;
		boolean north = blockAccess.getBlockId(x, y, z - 1) == block.blockID;
		boolean south = blockAccess.getBlockId(x, y, z + 1) == block.blockID;
		if(!railX && !railZ) {
			railX = true;
		}

		railThickness = 7.0F / 16.0F;
		railPadding = 9.0F / 16.0F;
		float railMinY = 12.0F / 16.0F;
		float railMaxY = 15.0F / 16.0F;
		float westMinX = west ? 0.0F : railThickness;
		float eastMaxX = east ? 1.0F : railPadding;
		float northMinZ = north ? 0.0F : railThickness;
		float southMaxZ = south ? 1.0F : railPadding;
		if(railX) {
			RenderBlockUtil.setBounds(block, westMinX, railMinY, railThickness, eastMaxX, railMaxY, railPadding);
			RenderBlockUtil.renderStandardBlock(renderBlocks, block, x, y, z);
		}

		if(railZ) {
			RenderBlockUtil.setBounds(block, railThickness, railMinY, northMinZ, railPadding, railMaxY, southMaxZ);
			RenderBlockUtil.renderStandardBlock(renderBlocks, block, x, y, z);
		}

		railMinY = 6.0F / 16.0F;
		railMaxY = 9.0F / 16.0F;
		if(railX) {
			RenderBlockUtil.setBounds(block, westMinX, railMinY, railThickness, eastMaxX, railMaxY, railPadding);
			RenderBlockUtil.renderStandardBlock(renderBlocks, block, x, y, z);
		}

		if(railZ) {
			RenderBlockUtil.setBounds(block, railThickness, railMinY, northMinZ, railPadding, railMaxY, southMaxZ);
			RenderBlockUtil.renderStandardBlock(renderBlocks, block, x, y, z);
		}

		RenderBlockUtil.resetBounds(block);
		return false;
	}
}