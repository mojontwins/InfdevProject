package net.minecraft.client.render.block;

import net.minecraft.client.render.RenderBlocks;
import net.minecraft.client.render.Tessellator;
import net.minecraft.game.world.IBlockAccess;
import net.minecraft.game.world.block.Block;
import util.AtlasUV;
import util.TextureAtlas;

/**
 * Render type 15 — redstone repeater, ported from the Beta 1.7.3 renderer. The
 * slab body is the plain standard cube; the raised central plate is a top quad
 * one eighth of a block up, and the two torches are drawn with the shared
 * {@link RenderBlockTorch} body into the corners of that quad. The metadata
 * provides the direction (bits 0-1) the repeater faces and the delay setting
 * (bits 2-3) that nudges both torches sideways.
 */
public final class RenderBlockRepeater implements BlockRenderHandler {
	/** The two torch slots shift a little deeper into the plate per delay step. */
	private static final double[] TORCH_OFFSET = {0.375D, 0.375D, 0.375D, 0.375D};

	@Override
	public final boolean renderBlock(RenderBlocks renderBlocks, Block block, int x, int y, int z) {
		IBlockAccess blockAccess = renderBlocks.blockAccess;
		int metadata = blockAccess.getBlockMetadata(x, y, z);
		int facing = metadata & 3;
		int delay = (metadata & 12) >> 2;
		RenderBlockUtil.renderStandardBlock(renderBlocks, block, x, y, z);
		Tessellator tessellator = Tessellator.instance;
		float brightness = block.getBlockBrightness(blockAccess, x, y, z);
		if(block.getLightValue(metadata) > 0) {
			brightness = 1.0F;
		}

		tessellator.setColorOpaque_F(brightness, brightness, brightness);
		double stickDrop = -0.1875D;
		double slotX = 0.0D;
		double slotZ = 0.0D;
		double torchX = 0.0D;
		double torchZ = 0.0D;
		switch(facing) {
		case 0:
			torchZ = -0.3125D;
			slotZ = TORCH_OFFSET[delay];
			break;
		case 1:
			torchX = 0.3125D;
			slotX = -TORCH_OFFSET[delay];
			break;
		case 2:
			torchZ = 0.3125D;
			slotZ = -TORCH_OFFSET[delay];
			break;
		case 3:
			torchX = -0.3125D;
			slotX = TORCH_OFFSET[delay];
		}

		RenderBlockTorch.renderTorchAtAngle(renderBlocks, block, (double)x + slotX, (double)y + stickDrop, (double)z + slotZ, 0.0D, 0.0D);
		RenderBlockTorch.renderTorchAtAngle(renderBlocks, block, (double)x + torchX, (double)y + stickDrop, (double)z + torchZ, 0.0D, 0.0D);
		int textureId = block.getBlockTextureFromSide(1);
		AtlasUV.calc(textureId, TextureAtlas.TERRAIN);
		double uLo = AtlasUV.u1;
		double uHi = AtlasUV.u2;
		double vLo = AtlasUV.v1;
		double vHi = AtlasUV.v2;
		float step = 0.125F;
		float topY = (float)y + step;
		// The four corners of the top quad, laid out in emission order.
		float cx0 = (float)(x + 1);
		float cx1 = (float)(x + 1);
		float cx2 = (float)x;
		float cx3 = (float)x;
		float cz0 = (float)z;
		float cz1 = (float)(z + 1);
		float cz2 = (float)(z + 1);
		float cz3 = (float)z;
		if(facing == 2) {
			cx0 = cx1 = (float)x;
			cx2 = cx3 = (float)(x + 1);
			cz0 = cz3 = (float)(z + 1);
			cz1 = cz2 = (float)z;
		} else if(facing == 3) {
			cx0 = cx3 = (float)x;
			cx1 = cx2 = (float)(x + 1);
			cz0 = cz1 = (float)z;
			cz2 = cz3 = (float)(z + 1);
		} else if(facing == 1) {
			cx0 = cx3 = (float)(x + 1);
			cx1 = cx2 = (float)x;
			cz0 = cz1 = (float)(z + 1);
			cz2 = cz3 = (float)z;
		}

		tessellator.addVertexWithUV((double)cx3, (double)topY, (double)cz3, uLo, vLo);
		tessellator.addVertexWithUV((double)cx2, (double)topY, (double)cz2, uLo, vHi);
		tessellator.addVertexWithUV((double)cx1, (double)topY, (double)cz1, uHi, vHi);
		tessellator.addVertexWithUV((double)cx0, (double)topY, (double)cz0, uHi, vLo);
		return true;
	}
}