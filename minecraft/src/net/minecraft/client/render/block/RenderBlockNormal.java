package net.minecraft.client.render.block;

import net.minecraft.client.render.RenderBlocks;
import net.minecraft.client.render.Tessellator;
import net.minecraft.game.world.IBlockAccess;
import net.minecraft.game.world.block.Block;
import org.lwjgl.opengl.GL11;

/**
 * Render type 0 — the standard six-faced cube used by most blocks (stone,
 * dirt, slabs, glass, leaves, …). Every face is emitted with per-side shading
 * (bottom 0.5, top 1.0, n/s 0.8, e/w 0.6) and skipped when the neighbour can
 * not be seen through it.
 */
public final class RenderBlockNormal implements BlockRenderHandler {
	@Override
	public final boolean renderBlock(RenderBlocks renderBlocks, Block block, int x, int y, int z) {
		IBlockAccess blockAccess = renderBlocks.blockAccess;
		Tessellator tessellator = Tessellator.instance;
		boolean anyFaceRendered = false;
		float selfBrightness = block.getBlockBrightness(blockAccess, x, y, z);

		for(int side = 0; side < 6; ++side) {
			int[] neighbor = RenderBlocks.NEIGHBOR_OFFSETS[side];
			if(!renderBlocks.flipTexture && !block.shouldSideBeRendered(blockAccess, x + neighbor[0], y + neighbor[1], z + neighbor[2], side)) {
				continue;
			}

			float brightness = neighborBrightness(renderBlocks, block, x, y, z, side, selfBrightness);
			float light = RenderBlocks.SIDE_LIGHT[side];
			tessellator.setColorOpaque_F(light * brightness, light * brightness, light * brightness);
			renderBlocks.renderFace(block, side, x, y, z, block.getBlockTexture(blockAccess, x, y, z, side));
			anyFaceRendered = true;
		}

		return anyFaceRendered;
	}

	@Override
	public final void renderBlockOnInventory(RenderBlocks renderBlocks, Block block) {
		Tessellator tessellator = Tessellator.instance;
		GL11.glTranslatef(-0.5F, -0.5F, -0.5F);

		for(int side = 0; side < 6; ++side) {
			tessellator.startDrawingQuads();
			float[] normal = RenderBlocks.SIDE_NORMALS[side];
			Tessellator.setNormal(normal[0], normal[1], normal[2]);
			renderBlocks.renderFace(block, side, 0.0D, 0.0D, 0.0D, block.getBlockTextureFromSide(side));
			tessellator.draw();
		}

		GL11.glTranslatef(0.5F, 0.5F, 0.5F);
	}

	/** Neighbour brightness with the block's own full-bright light baked in; the top face falls back to the block's own brightness unless it is a full cube. */
	private static float neighborBrightness(RenderBlocks renderBlocks, Block block, int x, int y, int z, int side, float selfBrightness) {
		int[] neighbor = RenderBlocks.NEIGHBOR_OFFSETS[side];
		float brightness = block.getBlockBrightness(renderBlocks.blockAccess, x + neighbor[0], y + neighbor[1], z + neighbor[2]);
		if(side == 1 && block.maxY != 1.0D && !block.blockMaterial.getIsLiquid()) {
			brightness = selfBrightness;
		}

		return Block.lightValue[block.blockID] > 0 ? 1.0F : brightness;
	}
}