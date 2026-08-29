package net.minecraft.client.render.block;

import net.minecraft.client.render.RenderBlocks;
import net.minecraft.client.render.Tessellator;
import net.minecraft.game.world.IBlockAccess;
import net.minecraft.game.world.block.Block;

/**
 * Render type 13 — cactus, ported from the Alpha 1.1.2 renderer. It is the
 * standard shaded cube, but each of the four side faces is nudged one sixteenth
 * of a block outward (via a tessellator translation) so the thorns stay
 * visually separated from the neighbouring face, and the top divides its
 * shading like any slab. Colour multipliers do not exist in this version, so
 * the tint is plain white.
 */
public final class RenderBlockCactus implements BlockRenderHandler {
	@Override
	public final boolean renderBlock(RenderBlocks renderBlocks, Block block, int x, int y, int z) {
		IBlockAccess blockAccess = renderBlocks.blockAccess;
		Tessellator tessellator = Tessellator.instance;
		boolean anyFace = false;
		float selfBrightness = block.getBlockBrightness(blockAccess, x, y, z);
		float sideShade = 1.0F / 16.0F;
		float brightness;
		if(renderBlocks.flipTexture || block.shouldSideBeRendered(blockAccess, x, y - 1, z, 0)) {
			brightness = block.getBlockBrightness(blockAccess, x, y - 1, z);
			tessellator.setColorOpaque_F(0.5F * brightness, 0.5F * brightness, 0.5F * brightness);
			renderBlocks.renderFace(block, 0, x, y, z, block.getBlockTexture(blockAccess, x, y, z, 0));
			anyFace = true;
		}

		if(renderBlocks.flipTexture || block.shouldSideBeRendered(blockAccess, x, y + 1, z, 1)) {
			brightness = block.getBlockBrightness(blockAccess, x, y + 1, z);
			if(block.maxY != 1.0D && !block.blockMaterial.getIsLiquid()) {
				brightness = selfBrightness;
			}

			tessellator.setColorOpaque_F(brightness, brightness, brightness);
			renderBlocks.renderFace(block, 1, x, y, z, block.getBlockTexture(blockAccess, x, y, z, 1));
			anyFace = true;
		}

		if(renderBlocks.flipTexture || block.shouldSideBeRendered(blockAccess, x, y, z - 1, 2)) {
			brightness = block.getBlockBrightness(blockAccess, x, y, z - 1);
			if(block.minZ > 0.0D) {
				brightness = selfBrightness;
			}

			tessellator.setColorOpaque_F(0.8F * brightness, 0.8F * brightness, 0.8F * brightness);
			tessellator.setTranslationD(0.0D, 0.0D, sideShade);
			renderBlocks.renderFace(block, 2, x, y, z, block.getBlockTexture(blockAccess, x, y, z, 2));
			tessellator.setTranslationD(0.0D, 0.0D, -sideShade);
			anyFace = true;
		}

		if(renderBlocks.flipTexture || block.shouldSideBeRendered(blockAccess, x, y, z + 1, 3)) {
			brightness = block.getBlockBrightness(blockAccess, x, y, z + 1);
			if(block.maxZ < 1.0D) {
				brightness = selfBrightness;
			}

			tessellator.setColorOpaque_F(0.8F * brightness, 0.8F * brightness, 0.8F * brightness);
			tessellator.setTranslationD(0.0D, 0.0D, -sideShade);
			renderBlocks.renderFace(block, 3, x, y, z, block.getBlockTexture(blockAccess, x, y, z, 3));
			tessellator.setTranslationD(0.0D, 0.0D, sideShade);
			anyFace = true;
		}

		if(renderBlocks.flipTexture || block.shouldSideBeRendered(blockAccess, x - 1, y, z, 4)) {
			brightness = block.getBlockBrightness(blockAccess, x - 1, y, z);
			if(block.minX > 0.0D) {
				brightness = selfBrightness;
			}

			tessellator.setColorOpaque_F(0.6F * brightness, 0.6F * brightness, 0.6F * brightness);
			tessellator.setTranslationD(sideShade, 0.0D, 0.0D);
			renderBlocks.renderFace(block, 4, x, y, z, block.getBlockTexture(blockAccess, x, y, z, 4));
			tessellator.setTranslationD(-sideShade, 0.0D, 0.0D);
			anyFace = true;
		}

		if(renderBlocks.flipTexture || block.shouldSideBeRendered(blockAccess, x + 1, y, z, 5)) {
			brightness = block.getBlockBrightness(blockAccess, x + 1, y, z);
			if(block.maxX < 1.0D) {
				brightness = selfBrightness;
			}

			tessellator.setColorOpaque_F(0.6F * brightness, 0.6F * brightness, 0.6F * brightness);
			tessellator.setTranslationD(-sideShade, 0.0D, 0.0D);
			renderBlocks.renderFace(block, 5, x, y, z, block.getBlockTexture(blockAccess, x, y, z, 5));
			tessellator.setTranslationD(sideShade, 0.0D, 0.0D);
			anyFace = true;
		}

		return anyFace;
	}
}