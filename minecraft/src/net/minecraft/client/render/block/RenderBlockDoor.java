package net.minecraft.client.render.block;

import net.minecraft.client.render.RenderBlocks;
import net.minecraft.client.render.Tessellator;
import net.minecraft.game.world.IBlockAccess;
import net.minecraft.game.world.block.Block;

/**
 * Render type 7 — doors, ported from the Alpha 1.1.2 renderer. A door is a
 * thin panel (three sixteenths thick) standing against one wall of its cell;
 * two stacked cells form the full door. The panel is drawn as the six faces of
 * the current bounds, each shaded from the neighbour's brightness and, when a
 * side faces an open gap, from the block's own light. Door tiles carry a
 * negative index to request the mirrored texture, which is resolved here by
 * toggling the renderer's mirror flag around the face.
 */
public final class RenderBlockDoor implements BlockRenderHandler {
	@Override
	public final boolean renderBlock(RenderBlocks renderBlocks, Block block, int x, int y, int z) {
		IBlockAccess blockAccess = renderBlocks.blockAccess;
		Tessellator tessellator = Tessellator.instance;
		boolean anyFace = false;
		int metadata = blockAccess.getBlockMetadata(x, y, z);
		// The three rotating states, matching the A1.1.2 BlockDoor latching table.
		int state = (metadata & 4) == 0 ? (metadata - 1) & 3 : metadata & 3;
		switch(state) {
		case 0:
			RenderBlockUtil.setBounds(block, 0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 3.0F / 16.0F);
			break;
		case 1:
			RenderBlockUtil.setBounds(block, 13.0F / 16.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
			break;
		case 2:
			RenderBlockUtil.setBounds(block, 0.0F, 0.0F, 13.0F / 16.0F, 1.0F, 1.0F, 1.0F);
			break;
		case 3:
			RenderBlockUtil.setBounds(block, 0.0F, 0.0F, 0.0F, 3.0F / 16.0F, 1.0F, 1.0F);
			break;
		}

		float selfBrightness = block.getBlockBrightness(blockAccess, x, y, z);
		float brightness = block.getBlockBrightness(blockAccess, x, y - 1, z);
		if(block.minY > 0.0D) {
			brightness = selfBrightness;
		}

		if(block.getLightValue(metadata) > 0) {
			brightness = 1.0F;
		}

		tessellator.setColorOpaque_F(0.5F * brightness, 0.5F * brightness, 0.5F * brightness);
		renderBlocks.renderFace(block, 0, x, y, z, block.getBlockTexture(blockAccess, x, y, z, 0));
		anyFace = true;
		brightness = block.getBlockBrightness(blockAccess, x, y + 1, z);
		if(block.maxY < 1.0D) {
			brightness = selfBrightness;
		}

		if(block.getLightValue(metadata) > 0) {
			brightness = 1.0F;
		}

		tessellator.setColorOpaque_F(brightness, brightness, brightness);
		renderBlocks.renderFace(block, 1, x, y, z, block.getBlockTexture(blockAccess, x, y, z, 1));
		anyFace = true;
		anyFace |= this.renderDoorSide(renderBlocks, block, x, y, z, 2, 0.8F, 0.0D, 0.0D, -1.0D, selfBrightness);
		anyFace |= this.renderDoorSide(renderBlocks, block, x, y, z, 3, 0.8F, 0.0D, 0.0D, 1.0D, selfBrightness);
		anyFace |= this.renderDoorSide(renderBlocks, block, x, y, z, 4, 0.6F, -1.0D, 0.0D, 0.0D, selfBrightness);
		anyFace |= this.renderDoorSide(renderBlocks, block, x, y, z, 5, 0.6F, 1.0D, 0.0D, 0.0D, selfBrightness);
		return anyFace;
	}

	/** One of the four door sides: neighbour brightness, mirroring for negative tile indices. */
	private boolean renderDoorSide(RenderBlocks renderBlocks, Block block, int x, int y, int z, int side, float shade, double offsetX, double offsetY, double offsetZ, float selfBrightness) {
		IBlockAccess blockAccess = renderBlocks.blockAccess;
		Tessellator tessellator = Tessellator.instance;
		int[] neighbor = RenderBlocks.NEIGHBOR_OFFSETS[side];
		float brightness = block.getBlockBrightness(blockAccess, x + neighbor[0], y + neighbor[1], z + neighbor[2]);
		if((side == 2 && block.minZ > 0.0D) || (side == 3 && block.maxZ < 1.0D) || (side == 4 && block.minX > 0.0D) || (side == 5 && block.maxX < 1.0D)) {
			brightness = selfBrightness;
		}

		if(block.getLightValue(blockAccess.getBlockMetadata(x, y, z)) > 0) {
			brightness = 1.0F;
		}

		tessellator.setColorOpaque_F(shade * brightness, shade * brightness, shade * brightness);
		int textureId = block.getBlockTexture(blockAccess, x, y, z, side);
		if(textureId < 0) {
			renderBlocks.mirrorTexture = true;
			textureId = -textureId;
		}

		renderBlocks.renderFace(block, side, x, y, z, textureId);
		renderBlocks.mirrorTexture = false;
		return true;
	}
}