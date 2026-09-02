package net.minecraft.client.render.block;

import net.minecraft.client.render.RenderBlocks;
import net.minecraft.client.render.Tessellator;
import net.minecraft.game.world.IBlockAccess;
import net.minecraft.game.world.block.Block;
import util.AtlasTexel;
import util.AtlasUV;
import util.TextureAtlas;

/**
 * Render type 2 — torches. The metadata picks which neighbour the torch leans
 * against (1 west, 2 east, 3 south, 4 north); any other value renders it
 * upright. Geometry is a floating head slab plus four stick faces that taper
 * toward the base. Also used for the inventory preview.
 */
public final class RenderBlockTorch implements BlockRenderHandler {
	@Override
	public final boolean renderBlock(RenderBlocks renderBlocks, Block block, int x, int y, int z) {
		IBlockAccess blockAccess = renderBlocks.blockAccess;
		int metadata = blockAccess.getBlockMetadata(x, y, z);
		Tessellator tessellator = Tessellator.instance;
		float torchBrightness = block.getBlockBrightness(blockAccess, x, y, z);
		if(block.getLightValue(metadata) > 0) {
			torchBrightness = 1.0F;
		}

		tessellator.setColorOpaque_F(torchBrightness, torchBrightness, torchBrightness);
		if(metadata == 1) {
			// Leaning against the west neighbour.
			renderTorchAtAngle(renderBlocks, block, (double)x - (double)0.099999994F, (double)y + (double)0.2F, (double)z, (double)-0.4F, 0.0D);
		} else if(metadata == 2) {
			// Leaning against the east neighbour.
			renderTorchAtAngle(renderBlocks, block, (double)x + (double)0.099999994F, (double)y + (double)0.2F, (double)z, (double)0.4F, 0.0D);
		} else if(metadata == 3) {
			// Leaning against the south neighbour.
			renderTorchAtAngle(renderBlocks, block, (double)x, (double)y + (double)0.2F, (double)z - (double)0.099999994F, 0.0D, (double)-0.4F);
		} else if(metadata == 4) {
			// Leaning against the north neighbour.
			renderTorchAtAngle(renderBlocks, block, (double)x, (double)y + (double)0.2F, (double)z + (double)0.099999994F, 0.0D, (double)0.4F);
		} else {
			// Upright torch.
			renderTorchAtAngle(renderBlocks, block, (double)x, (double)y, (double)z, 0.0D, 0.0D);
		}

		return true;
	}

	@Override
	public final void renderBlockOnInventory(RenderBlocks renderBlocks, Block block, int metadata, float brightness) {
		Tessellator tessellator = Tessellator.instance;
		tessellator.setColorOpaque_F(brightness, brightness, brightness);
		tessellator.startDrawingQuads();
		Tessellator.setNormal(0.0F, -1.0F, 0.0F);
		renderTorchAtAngle(renderBlocks, block, -0.5D, -0.5D, -0.5D, 0.0D, 0.0D);
		tessellator.draw();
	}

	/**
	 * The torch body (head slab plus four tapering stick faces), positioned at
	 * {@code (x, y, z)} with an optional lean. Shared with the redstone repeater,
	 * which draws its two torches into the same corner slate.
	 */
	static void renderTorchAtAngle(RenderBlocks renderBlocks, Block block, double x, double y, double z, double leanX, double leanZ) {
		Tessellator tessellator = Tessellator.instance;
		int textureId = block.getBlockTextureFromSide(0);
		if(renderBlocks.overrideBlockTexture >= 0) {
			textureId = renderBlocks.overrideBlockTexture;
		}

		AtlasUV.calc(textureId, TextureAtlas.TERRAIN);
		float uLo = (float)AtlasUV.u1;
		float uHi = (float)AtlasUV.u2;
		float vLo = (float)AtlasUV.v1;
		float vHi = (float)AtlasUV.v2;
		// The lighter-coloured cross on the torch head is a 2x2 pixel patch in
		// the centre of the tile.
		AtlasUV.calcPixels(AtlasTexel.u + 7, AtlasTexel.v + 6, 2.0F, 2.0F, TextureAtlas.TERRAIN);
		double headEdgeU = AtlasUV.u1;
		double headTopV = AtlasUV.v1;
		double headEdgeU2 = AtlasUV.u2;
		double headBottomV = AtlasUV.v2;
		x += 0.5D;
		z += 0.5D;
		double xMin = x - 0.5D;
		double xMax = x + 0.5D;
		double zMin = z - 0.5D;
		double zMax = z + 0.5D;

		// Torch head (a small slab floating at the tip, offset by any lean).
		tessellator.addVertexWithUV(x + leanX * 0.375D - 1.0D / 16.0D, y + 0.625D, z + leanZ * 0.375D - 1.0D / 16.0D, headEdgeU, headTopV);
		tessellator.addVertexWithUV(x + leanX * 0.375D - 1.0D / 16.0D, y + 0.625D, z + leanZ * 0.375D + 1.0D / 16.0D, headEdgeU, headBottomV);
		tessellator.addVertexWithUV(x + leanX * 0.375D + 1.0D / 16.0D, y + 0.625D, z + leanZ * 0.375D + 1.0D / 16.0D, headEdgeU2, headBottomV);
		tessellator.addVertexWithUV(x + leanX * 0.375D + 1.0D / 16.0D, y + 0.625D, z + leanZ * 0.375D - 1.0D / 16.0D, headEdgeU2, headTopV);

		// The four stick faces, tapering down to the base (the lean moves the
		// bottom edge sideways towards whatever the torch is attached to).
		tessellator.addVertexWithUV(x - 1.0D / 16.0D, y + 1.0D, zMin, (double)uLo, (double)vLo);
		tessellator.addVertexWithUV(x - 1.0D / 16.0D + leanX, y, zMin + leanZ, (double)uLo, (double)vHi);
		tessellator.addVertexWithUV(x - 1.0D / 16.0D + leanX, y, zMax + leanZ, (double)uHi, (double)vHi);
		tessellator.addVertexWithUV(x - 1.0D / 16.0D, y + 1.0D, zMax, (double)uHi, (double)vLo);
		tessellator.addVertexWithUV(x + 1.0D / 16.0D, y + 1.0D, zMax, (double)uLo, (double)vLo);
		tessellator.addVertexWithUV(x + leanX + 1.0D / 16.0D, y, zMax + leanZ, (double)uLo, (double)vHi);
		tessellator.addVertexWithUV(x + leanX + 1.0D / 16.0D, y, zMin + leanZ, (double)uHi, (double)vHi);
		tessellator.addVertexWithUV(x + 1.0D / 16.0D, y + 1.0D, zMin, (double)uHi, (double)vLo);
		tessellator.addVertexWithUV(xMin, y + 1.0D, z + 1.0D / 16.0D, (double)uLo, (double)vLo);
		tessellator.addVertexWithUV(xMin + leanX, y, z + 1.0D / 16.0D + leanZ, (double)uLo, (double)vHi);
		tessellator.addVertexWithUV(xMax + leanX, y, z + 1.0D / 16.0D + leanZ, (double)uHi, (double)vHi);
		tessellator.addVertexWithUV(xMax, y + 1.0D, z + 1.0D / 16.0D, (double)uHi, (double)vLo);
		tessellator.addVertexWithUV(xMax, y + 1.0D, z - 1.0D / 16.0D, (double)uLo, (double)vLo);
		tessellator.addVertexWithUV(xMax + leanX, y, z - 1.0D / 16.0D + leanZ, (double)uLo, (double)vHi);
		tessellator.addVertexWithUV(xMin + leanX, y, z - 1.0D / 16.0D + leanZ, (double)uHi, (double)vHi);
		tessellator.addVertexWithUV(xMin, y + 1.0D, z - 1.0D / 16.0D, (double)uHi, (double)vLo);
	}
}