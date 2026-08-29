package net.minecraft.client.render.block;

import net.minecraft.client.render.RenderBlocks;
import net.minecraft.client.render.Tessellator;
import net.minecraft.game.world.IBlockAccess;
import net.minecraft.game.world.block.Block;
import util.AtlasUV;
import util.TextureAtlas;

/**
 * Render type 14 — the bed, ported from the Beta 1.7.3 renderer.
 *
 * <p>Beds do not exist yet in this 2010 build (they arrive in Alpha 1.0.1), so
 * this handler is written against the upstream footsteps and registered in
 * {@link BlockRenderType} ready for the expansion stage. The two metadata
 * helpers that later versions place on {@code BlockBed} are inlined here as
 * plain block math: the facing is {@code metadata & 3} and the foot half is
 * flagged by {@code metadata & 8}.
 *
 * <p>The frame is drawn as a mattress with a lowered underside and four walls;
 * the wall coinciding with the headboard is skipped, and the reversed half of
 * the bed flips its tile so the headboard design faces the right wall. Unlike
 * bounds-driven handlers such as the door, the bed always occupies a full
 * footprint (one block wide, nine sixteenths tall).
 */
public final class RenderBlockBed implements BlockRenderHandler {
	/** Side that hides the headboard, indexed by the facing direction 0-3. */
	private static final int[] HEAD_INVISIBLE_FACE = {3, 4, 2, 5};
	/** Re-maps a foot half's facing onto the head-half tables. */
	private static final int[] FOOT_FACE_REMAP = {2, 3, 0, 1};
	/** Side whose tile is mirrored for the reversed half of the bed, indexed by facing. */
	private static final int[] MIRROR_FACE = {5, 3, 4, 2};

	@Override
	public final boolean renderBlock(RenderBlocks renderBlocks, Block block, int x, int y, int z) {
		IBlockAccess blockAccess = renderBlocks.blockAccess;
		Tessellator tessellator = Tessellator.instance;
		int metadata = blockAccess.getBlockMetadata(x, y, z);
		int direction = metadata & 3;
		boolean foot = (metadata & 8) != 0;
		RenderBlockUtil.setBounds(block, 0.0F, 0.0F, 0.0F, 1.0F, 9.0F / 16.0F, 1.0F);
		float selfBrightness = block.getBlockBrightness(blockAccess, x, y, z);

		// Underside of the frame, almost a quarter block lower than the mattress.
		tessellator.setColorOpaque_F(0.5F * selfBrightness, 0.5F * selfBrightness, 0.5F * selfBrightness);
		AtlasUV.calc(block.getBlockTexture(blockAccess, x, y, z, 0), TextureAtlas.TERRAIN);
		double uLo = AtlasUV.u1;
		double uHi = AtlasUV.u2;
		double vLo = AtlasUV.v1;
		double vHi = AtlasUV.v2;
		double xMin = x + block.minX;
		double xMax = x + block.maxX;
		double zMin = z + block.minZ;
		double zMax = z + block.maxZ;
		double bedBottom = y + block.minY + 0.1875D;
		tessellator.addVertexWithUV(xMin, bedBottom, zMax, uLo, vHi);
		tessellator.addVertexWithUV(xMin, bedBottom, zMin, uLo, vLo);
		tessellator.addVertexWithUV(xMax, bedBottom, zMin, uHi, vLo);
		tessellator.addVertexWithUV(xMax, bedBottom, zMax, uHi, vHi);

		// Mattress top; each facing direction rotates the tile so the headboard
		// design lands against the pillow side.
		float topBrightness = block.getBlockBrightness(blockAccess, x, y + 1, z);
		tessellator.setColorOpaque_F(topBrightness, topBrightness, topBrightness);
		AtlasUV.calc(block.getBlockTexture(blockAccess, x, y, z, 1), TextureAtlas.TERRAIN);
		double uLeft = AtlasUV.u1;
		double uRight = AtlasUV.u2;
		double vDown = AtlasUV.v1;
		double vUp = AtlasUV.v2;
		double uXMaxZMax = uLeft;
		double vXMaxZMax = vUp;
		double uXMaxZMin = uLeft;
		double vXMaxZMin = vDown;
		double uXMinZMin = uRight;
		double vXMinZMin = vDown;
		double uXMinZMax = uRight;
		double vXMinZMax = vUp;
		if(direction == 0) {
			uXMinZMin = uLeft;
			vXMaxZMin = vUp;
			uXMaxZMax = uRight;
			vXMinZMax = vDown;
		} else if(direction == 2) {
			uXMaxZMin = uRight;
			vXMinZMin = vUp;
			uXMinZMax = uLeft;
			vXMaxZMax = vDown;
		} else if(direction == 3) {
			uXMinZMin = uLeft;
			vXMaxZMin = vUp;
			uXMaxZMax = uRight;
			vXMinZMax = vDown;
			uXMaxZMin = uRight;
			vXMinZMin = vUp;
			uXMinZMax = uLeft;
			vXMaxZMax = vDown;
		}

		double yTop = y + block.maxY;
		tessellator.addVertexWithUV(xMax, yTop, zMax, uXMaxZMax, vXMaxZMax);
		tessellator.addVertexWithUV(xMax, yTop, zMin, uXMaxZMin, vXMaxZMin);
		tessellator.addVertexWithUV(xMin, yTop, zMin, uXMinZMin, vXMinZMin);
		tessellator.addVertexWithUV(xMin, yTop, zMax, uXMinZMax, vXMinZMax);

		// The four walls. The wall folding under the headboard is skipped; the
		// reversed half mirrors its tile around the frame's long axis.
		int hiddenFace = HEAD_INVISIBLE_FACE[direction];
		if(foot) {
			hiddenFace = HEAD_INVISIBLE_FACE[FOOT_FACE_REMAP[direction]];
		}

		int mirrorFace = MIRROR_FACE[direction];
		if(hiddenFace != 2 && (renderBlocks.flipTexture || block.shouldSideBeRendered(blockAccess, x, y, z - 1, 2))) {
			float brightness = block.getBlockBrightness(blockAccess, x, y, z - 1);
			if(block.minZ > 0.0D) {
				brightness = selfBrightness;
			}

			tessellator.setColorOpaque_F(0.8F * brightness, 0.8F * brightness, 0.8F * brightness);
			renderBlocks.mirrorTexture = mirrorFace == 2;
			renderBlocks.renderFace(block, 2, x, y, z, block.getBlockTexture(blockAccess, x, y, z, 2));
		}

		if(hiddenFace != 3 && (renderBlocks.flipTexture || block.shouldSideBeRendered(blockAccess, x, y, z + 1, 3))) {
			float brightness = block.getBlockBrightness(blockAccess, x, y, z + 1);
			if(block.maxZ < 1.0D) {
				brightness = selfBrightness;
			}

			tessellator.setColorOpaque_F(0.8F * brightness, 0.8F * brightness, 0.8F * brightness);
			renderBlocks.mirrorTexture = mirrorFace == 3;
			renderBlocks.renderFace(block, 3, x, y, z, block.getBlockTexture(blockAccess, x, y, z, 3));
		}

		if(hiddenFace != 4 && (renderBlocks.flipTexture || block.shouldSideBeRendered(blockAccess, x - 1, y, z, 4))) {
			float brightness = block.getBlockBrightness(blockAccess, x - 1, y, z);
			if(block.minX > 0.0D) {
				brightness = selfBrightness;
			}

			tessellator.setColorOpaque_F(0.6F * brightness, 0.6F * brightness, 0.6F * brightness);
			renderBlocks.mirrorTexture = mirrorFace == 4;
			renderBlocks.renderFace(block, 4, x, y, z, block.getBlockTexture(blockAccess, x, y, z, 4));
		}

		if(hiddenFace != 5 && (renderBlocks.flipTexture || block.shouldSideBeRendered(blockAccess, x + 1, y, z, 5))) {
			float brightness = block.getBlockBrightness(blockAccess, x + 1, y, z);
			if(block.maxX < 1.0D) {
				brightness = selfBrightness;
			}

			tessellator.setColorOpaque_F(0.6F * brightness, 0.6F * brightness, 0.6F * brightness);
			renderBlocks.mirrorTexture = mirrorFace == 5;
			renderBlocks.renderFace(block, 5, x, y, z, block.getBlockTexture(blockAccess, x, y, z, 5));
		}

		renderBlocks.mirrorTexture = false;
		RenderBlockUtil.resetBounds(block);
		return true;
	}
}