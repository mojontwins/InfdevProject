package net.minecraft.client.render.block;

import net.minecraft.client.render.RenderBlocks;
import net.minecraft.game.world.block.Block;

/**
 * Render type 10 — stairs, ported from the Alpha 1.1.2 renderer. Each stair is
 * the union of two standard cubes inside its cell: a half-step and a full-thick
 * run, rotated by the metadata. The shared bounds are written before each cube
 * and the full unit cube is restored afterwards.
 */
public final class RenderBlockStairs implements BlockRenderHandler {
	@Override
	public final boolean renderBlock(RenderBlocks renderBlocks, Block block, int x, int y, int z) {
		int metadata = renderBlocks.blockAccess.getBlockMetadata(x, y, z);
		switch(metadata) {
		case 0:
			RenderBlockUtil.setBounds(block, 0.0F, 0.0F, 0.0F, 0.5F, 0.5F, 1.0F);
			RenderBlockUtil.renderStandardBlock(renderBlocks, block, x, y, z);
			RenderBlockUtil.setBounds(block, 0.5F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
			RenderBlockUtil.renderStandardBlock(renderBlocks, block, x, y, z);
			break;
		case 1:
			RenderBlockUtil.setBounds(block, 0.0F, 0.0F, 0.0F, 0.5F, 1.0F, 1.0F);
			RenderBlockUtil.renderStandardBlock(renderBlocks, block, x, y, z);
			RenderBlockUtil.setBounds(block, 0.5F, 0.0F, 0.0F, 1.0F, 0.5F, 1.0F);
			RenderBlockUtil.renderStandardBlock(renderBlocks, block, x, y, z);
			break;
		case 2:
			RenderBlockUtil.setBounds(block, 0.0F, 0.0F, 0.0F, 1.0F, 0.5F, 0.5F);
			RenderBlockUtil.renderStandardBlock(renderBlocks, block, x, y, z);
			RenderBlockUtil.setBounds(block, 0.0F, 0.0F, 0.5F, 1.0F, 1.0F, 1.0F);
			RenderBlockUtil.renderStandardBlock(renderBlocks, block, x, y, z);
			break;
		case 3:
			RenderBlockUtil.setBounds(block, 0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 0.5F);
			RenderBlockUtil.renderStandardBlock(renderBlocks, block, x, y, z);
			RenderBlockUtil.setBounds(block, 0.0F, 0.0F, 0.5F, 1.0F, 0.5F, 1.0F);
			RenderBlockUtil.renderStandardBlock(renderBlocks, block, x, y, z);
			break;
		}

		RenderBlockUtil.resetBounds(block);
		return false;
	}
}