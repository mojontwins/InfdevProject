package net.minecraft.client.render.block;

import net.minecraft.client.render.RenderBlocks;
import net.minecraft.client.render.Tessellator;
import net.minecraft.game.world.IBlockAccess;
import net.minecraft.game.world.block.Block;
import util.AtlasUV;
import util.TextureAtlas;

/**
 * Geometry shared by several render handlers: the standard shaded cube, the
 * crossed-squares planes of plants, and direct writes to the shared block
 * bounds.
 *
 * Many handlers must draw a block with non-unit bounds (doors, fences, rails,
 * …). In this 2010 codebase the bounds are mutable fields on the single shared
 * {@link Block} instance, and {@link Block#setBlockBounds} is protected, so the
 * helpers below write them directly; every handler restores the full unit cube
 * ({@link #resetBounds}) when it is done. The colour multiplier of later
 * versions is absent here, so the standard cube is drawn plain white.
 */
final class RenderBlockUtil {
	private RenderBlockUtil() {
	}

	/**
	 * Writes the block's bounds without touching the protected
	 * {@code setBlockBounds}, and remembers nothing — the caller is responsible
	 * for restoring {@link #resetBounds}.
	 */
	static void setBounds(Block block, float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
		block.minX = minX;
		block.minY = minY;
		block.minZ = minZ;
		block.maxX = maxX;
		block.maxY = maxY;
		block.maxZ = maxZ;
	}

	/** Restores the full unit cube, undoing any {@link #setBounds} call. */
	static void resetBounds(Block block) {
		setBounds(block, 0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
	}

	/** Standard six-faced cube with a plain white tint. */
	static boolean renderStandardBlock(RenderBlocks renderBlocks, Block block, int x, int y, int z) {
		return renderStandardBlockWithColorMultiplier(renderBlocks, block, x, y, z, 1.0F, 1.0F, 1.0F);
	}

	/**
	 * The classic cube: one face per exposed side, neighbour-brightness shaded
	 * with the per-side light table. When the block is thinner than a full cube
	 * the sunken sides fall back to the block's own brightness.
	 */
	static boolean renderStandardBlockWithColorMultiplier(RenderBlocks renderBlocks, Block block, int x, int y, int z, float red, float green, float blue) {
		IBlockAccess blockAccess = renderBlocks.blockAccess;
		Tessellator tessellator = Tessellator.instance;
		boolean anyFace = false;
		float selfBrightness = block.getBlockBrightness(blockAccess, x, y, z);

		for(int side = 0; side < 6; ++side) {
			int[] offset = RenderBlocks.NEIGHBOR_OFFSETS[side];
			if(!renderBlocks.flipTexture && !block.shouldSideBeRendered(blockAccess, x + offset[0], y + offset[1], z + offset[2], side)) {
				continue;
			}

			float brightness = block.getBlockBrightness(blockAccess, x + offset[0], y + offset[1], z + offset[2]);
			switch(side) {
			case 1:
				if(block.maxY != 1.0D && !block.blockMaterial.getIsLiquid()) {
					brightness = selfBrightness;
				}
				break;
			case 2:
				if(block.minZ > 0.0D) {
					brightness = selfBrightness;
				}
				break;
			case 3:
				if(block.maxZ < 1.0D) {
					brightness = selfBrightness;
				}
				break;
			case 4:
				if(block.minX > 0.0D) {
					brightness = selfBrightness;
				}
				break;
			case 5:
				if(block.maxX < 1.0D) {
					brightness = selfBrightness;
				}
				break;
			}

			float light = RenderBlocks.SIDE_LIGHT[side];
			tessellator.setColorOpaque_F(light * red * brightness, light * green * brightness, light * blue * brightness);
			renderBlocks.renderFace(block, side, x, y, z, block.getBlockTexture(blockAccess, x, y, z, side));
			anyFace = true;
		}

		return anyFace;
	}

	/**
	 * The two crossed, two-sided planes that make every plant-like block, with
	 * 0.45 arms so the leaves stay visually separated.
	 */
	static void renderCrossedSquares(RenderBlocks renderBlocks, Block block, int metadata, double x, double y, double z) {
		Tessellator tessellator = Tessellator.instance;
		int textureId = block.getBlockTextureFromSideAndMetadata(0, metadata);
		if(renderBlocks.overrideBlockTexture >= 0) {
			textureId = renderBlocks.overrideBlockTexture;
		}

		AtlasUV.calc(textureId, TextureAtlas.TERRAIN);
		double uLo = AtlasUV.u1;
		double uHi = AtlasUV.u2;
		double vLo = AtlasUV.v1;
		double vHi = AtlasUV.v2;
		double xLow = x + 0.5D - (double)0.45F;
		double xHigh = x + 0.5D + (double)0.45F;
		double zLow = z + 0.5D - (double)0.45F;
		double zHigh = z + 0.5D + (double)0.45F;
		tessellator.addVertexWithUV(xLow, y + 1.0D, zLow, uLo, vLo);
		tessellator.addVertexWithUV(xLow, y, zLow, uLo, vHi);
		tessellator.addVertexWithUV(xHigh, y, zHigh, uHi, vHi);
		tessellator.addVertexWithUV(xHigh, y + 1.0D, zHigh, uHi, vLo);
		tessellator.addVertexWithUV(xHigh, y + 1.0D, zHigh, uLo, vLo);
		tessellator.addVertexWithUV(xHigh, y, zHigh, uLo, vHi);
		tessellator.addVertexWithUV(xLow, y, zLow, uHi, vHi);
		tessellator.addVertexWithUV(xLow, y + 1.0D, zLow, uHi, vLo);
		tessellator.addVertexWithUV(xLow, y + 1.0D, zHigh, uLo, vLo);
		tessellator.addVertexWithUV(xLow, y, zHigh, uLo, vHi);
		tessellator.addVertexWithUV(xHigh, y, zLow, uHi, vHi);
		tessellator.addVertexWithUV(xHigh, y + 1.0D, zLow, uHi, vLo);
		tessellator.addVertexWithUV(xHigh, y + 1.0D, zLow, uLo, vLo);
		tessellator.addVertexWithUV(xHigh, y, zLow, uLo, vHi);
		tessellator.addVertexWithUV(xLow, y, zHigh, uHi, vHi);
		tessellator.addVertexWithUV(xLow, y + 1.0D, zHigh, uHi, vLo);
	}
}