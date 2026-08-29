package net.minecraft.client.render.block;

import net.minecraft.client.render.RenderBlocks;
import net.minecraft.game.world.IBlockAccess;
import net.minecraft.game.world.block.Block;

/**
 * Render type 18 — glass panes. This is the deliberately compact port of the
 * Beta 1.7.3 pane: instead of laying out every face by hand, the pane is
 * sliced into a centre pillar plus full-length strips, each drawn with the
 * shared standard cube renderer. A strip runs the length of the block whenever
 * a connectable neighbour (any full cube, glass, or another pane) sits on that
 * side, following the B1.7.3 connection rules.
 */
public final class RenderBlockPane implements BlockRenderHandler {
	/** Half of the pane thickness: the strips are one eighth of a block wide. */
	private static final float HALF_THICKNESS = 1.0F / 8.0F;

	@Override
	public final boolean renderBlock(RenderBlocks renderBlocks, Block block, int x, int y, int z) {
		IBlockAccess blockAccess = renderBlocks.blockAccess;
		boolean connectNorth = this.canConnect(blockAccess, block, x, y, z - 1);
		boolean connectSouth = this.canConnect(blockAccess, block, x, y, z + 1);
		boolean connectWest = this.canConnect(blockAccess, block, x - 1, y, z);
		boolean connectEast = this.canConnect(blockAccess, block, x + 1, y, z);
		RenderBlockUtil.setBounds(block, 0.5F - HALF_THICKNESS, 0.0F, 0.5F - HALF_THICKNESS, 0.5F + HALF_THICKNESS, 1.0F, 0.5F + HALF_THICKNESS);
		RenderBlockUtil.renderStandardBlock(renderBlocks, block, x, y, z);
		if(connectNorth || connectSouth) {
			RenderBlockUtil.setBounds(block, 0.5F - HALF_THICKNESS, 0.0F, 0.0F, 0.5F + HALF_THICKNESS, 1.0F, 1.0F);
			RenderBlockUtil.renderStandardBlock(renderBlocks, block, x, y, z);
		}

		if(connectWest || connectEast) {
			RenderBlockUtil.setBounds(block, 0.0F, 0.0F, 0.5F - HALF_THICKNESS, 1.0F, 1.0F, 0.5F + HALF_THICKNESS);
			RenderBlockUtil.renderStandardBlock(renderBlocks, block, x, y, z);
		}

		RenderBlockUtil.resetBounds(block);
		return false;
	}

	/** A pane joins the neighbour when it is a full cube, glass, or another pane. */
	private boolean canConnect(IBlockAccess blockAccess, Block block, int x, int y, int z) {
		int neighborId = blockAccess.getBlockId(x, y, z);
		return Block.opaqueCubeLookup[neighborId] || neighborId == block.blockID || neighborId == Block.glass.blockID;
	}
}