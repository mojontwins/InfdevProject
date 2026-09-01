package net.minecraft.client.render.block;

import net.minecraft.client.render.RenderBlocks;
import net.minecraft.client.render.Tessellator;
import net.minecraft.game.world.IBlockAccess;
import net.minecraft.game.world.block.Block;
import util.AtlasTexel;
import util.AtlasUV;
import util.TextureAtlas;

/**
 * Render type 5 — redstone wire, ported from the Alpha 1.1.2 renderer.
 *
 * The wire lays a flat 0.5-pixel-thick strip on the floor of its cell, trimmed
 * to the sides that are actually connected, and climbs one block up the sides
 * of the blocks neighbouring a vertical connection.
 *
 * The canonical wire id is {@link #REDSTONE_WIRE_ID} and
 * {@code canProvidePower()} does not exist in this version, so the
 * power-connection test below only links a wire to other wires. The
 * block-access test {@code isBlockNormalCube} is not part of the
 * {@code IBlockAccess} surface here either, so the solid-block test is used
 * instead.
 */
public final class RenderBlockRedstoneWire implements BlockRenderHandler {
	/** The block id of the redstone wire (also the slot shared with the cog/gears block). */
	private static final int REDSTONE_WIRE_ID = 55;

	@Override
	public final boolean renderBlock(RenderBlocks renderBlocks, Block block, int x, int y, int z) {
		IBlockAccess blockAccess = renderBlocks.blockAccess;
		Tessellator tessellator = Tessellator.instance;
		int textureId = block.getBlockTextureFromSideAndMetadata(1, blockAccess.getBlockMetadata(x, y, z));
		if(renderBlocks.overrideBlockTexture >= 0) {
			textureId = renderBlocks.overrideBlockTexture;
		}

		float brightness = block.getBlockBrightness(blockAccess, x, y, z);
		tessellator.setColorOpaque_F(brightness, brightness, brightness);
		AtlasUV.calc(textureId, TextureAtlas.TERRAIN);
		double uLo = AtlasUV.u1;
		double uHi = AtlasUV.u2;
		double vLo = AtlasUV.v1;
		double vHi = AtlasUV.v2;
		float trim = 0.0F;
		float thickness = 0.03125F;
		boolean west = isPowerProviderOrWire(blockAccess, x - 1, y, z) || !blockAccess.isSolid(x - 1, y, z) && isPowerProviderOrWire(blockAccess, x - 1, y - 1, z);
		boolean east = isPowerProviderOrWire(blockAccess, x + 1, y, z) || !blockAccess.isSolid(x + 1, y, z) && isPowerProviderOrWire(blockAccess, x + 1, y - 1, z);
		boolean north = isPowerProviderOrWire(blockAccess, x, y, z - 1) || !blockAccess.isSolid(x, y, z - 1) && isPowerProviderOrWire(blockAccess, x, y - 1, z - 1);
		boolean south = isPowerProviderOrWire(blockAccess, x, y, z + 1) || !blockAccess.isSolid(x, y, z + 1) && isPowerProviderOrWire(blockAccess, x, y - 1, z + 1);
		if(!blockAccess.isSolid(x, y + 1, z)) {
			if(blockAccess.isSolid(x - 1, y, z) && isPowerProviderOrWire(blockAccess, x - 1, y + 1, z)) {
				west = true;
			}

			if(blockAccess.isSolid(x + 1, y, z) && isPowerProviderOrWire(blockAccess, x + 1, y + 1, z)) {
				east = true;
			}

			if(blockAccess.isSolid(x, y, z - 1) && isPowerProviderOrWire(blockAccess, x, y + 1, z - 1)) {
				north = true;
			}

			if(blockAccess.isSolid(x, y, z + 1) && isPowerProviderOrWire(blockAccess, x, y + 1, z + 1)) {
				south = true;
			}
		}

		float inset = 5.0F / 16.0F;
		float xMin = (float)x;
		float xMax = (float)(x + 1);
		float zMin = (float)z;
		float zMax = (float)(z + 1);
		int shape = 0;
		if((west || east) && !north && !south) {
			shape = 1;
		}

		if((north || south) && !east && !west) {
			shape = 2;
		}

		if(shape != 0) {
			AtlasUV.calcPixels(AtlasTexel.u + 16, AtlasTexel.v, TextureAtlas.TERRAIN.tileSpan, TextureAtlas.TERRAIN.tileSpan, TextureAtlas.TERRAIN);
			uLo = AtlasUV.u1;
			uHi = AtlasUV.u2;
		}

		if(shape == 0) {
			if(east || south || north || west) {
				if(!west) {
					xMin += inset;
				}

				if(!west) {
					uLo += (double)(inset / 16.0F);
				}

				if(!east) {
					xMax -= inset;
				}

				if(!east) {
					uHi -= (double)(inset / 16.0F);
				}

				if(!north) {
					zMin += inset;
				}

				if(!north) {
					vLo += (double)(inset / 16.0F);
				}

				if(!south) {
					zMax -= inset;
				}

				if(!south) {
					vHi -= (double)(inset / 16.0F);
				}
			}

			tessellator.addVertexWithUV((double)(xMax + trim), (double)((float)y + thickness), (double)(zMax + trim), uHi, vHi);
			tessellator.addVertexWithUV((double)(xMax + trim), (double)((float)y + thickness), (double)(zMin - trim), uHi, vLo);
			tessellator.addVertexWithUV((double)(xMin - trim), (double)((float)y + thickness), (double)(zMin - trim), uLo, vLo);
			tessellator.addVertexWithUV((double)(xMin - trim), (double)((float)y + thickness), (double)(zMax + trim), uLo, vHi);
		}

		if(shape == 1) {
			tessellator.addVertexWithUV((double)(xMax + trim), (double)((float)y + thickness), (double)(zMax + trim), uHi, vHi);
			tessellator.addVertexWithUV((double)(xMax + trim), (double)((float)y + thickness), (double)(zMin - trim), uHi, vLo);
			tessellator.addVertexWithUV((double)(xMin - trim), (double)((float)y + thickness), (double)(zMin - trim), uLo, vLo);
			tessellator.addVertexWithUV((double)(xMin - trim), (double)((float)y + thickness), (double)(zMax + trim), uLo, vHi);
		}

		if(shape == 2) {
			tessellator.addVertexWithUV((double)(xMax + trim), (double)((float)y + thickness), (double)(zMax + trim), uHi, vHi);
			tessellator.addVertexWithUV((double)(xMax + trim), (double)((float)y + thickness), (double)(zMin - trim), uLo, vHi);
			tessellator.addVertexWithUV((double)(xMin - trim), (double)((float)y + thickness), (double)(zMin - trim), uLo, vLo);
			tessellator.addVertexWithUV((double)(xMin - trim), (double)((float)y + thickness), (double)(zMax + trim), uHi, vLo);
		}

		AtlasUV.calcPixels(AtlasTexel.u + 16, AtlasTexel.v, TextureAtlas.TERRAIN.tileSpan, TextureAtlas.TERRAIN.tileSpan, TextureAtlas.TERRAIN);
		uLo = AtlasUV.u1;
		uHi = AtlasUV.u2;
		vLo = AtlasUV.v1;
		vHi = AtlasUV.v2;
		if(!blockAccess.isSolid(x, y + 1, z)) {
			if(blockAccess.isSolid(x - 1, y, z) && blockAccess.getBlockId(x - 1, y + 1, z) == REDSTONE_WIRE_ID) {
				tessellator.addVertexWithUV((double)((float)x + thickness), (double)((float)(y + 1) + trim), (double)((float)(z + 1) + trim), uHi, vLo);
				tessellator.addVertexWithUV((double)((float)x + thickness), (double)((float)y - trim), (double)((float)(z + 1) + trim), uLo, vLo);
				tessellator.addVertexWithUV((double)((float)x + thickness), (double)((float)y - trim), (double)((float)z - trim), uLo, vHi);
				tessellator.addVertexWithUV((double)((float)x + thickness), (double)((float)(y + 1) + trim), (double)((float)z - trim), uHi, vHi);
			}

			if(blockAccess.isSolid(x + 1, y, z) && blockAccess.getBlockId(x + 1, y + 1, z) == REDSTONE_WIRE_ID) {
				tessellator.addVertexWithUV((double)((float)(x + 1) - thickness), (double)((float)y - trim), (double)((float)(z + 1) + trim), uLo, vHi);
				tessellator.addVertexWithUV((double)((float)(x + 1) - thickness), (double)((float)(y + 1) + trim), (double)((float)(z + 1) + trim), uHi, vHi);
				tessellator.addVertexWithUV((double)((float)(x + 1) - thickness), (double)((float)(y + 1) + trim), (double)((float)z - trim), uHi, vLo);
				tessellator.addVertexWithUV((double)((float)(x + 1) - thickness), (double)((float)y - trim), (double)((float)z - trim), uLo, vLo);
			}

			if(blockAccess.isSolid(x, y, z - 1) && blockAccess.getBlockId(x, y + 1, z - 1) == REDSTONE_WIRE_ID) {
				tessellator.addVertexWithUV((double)((float)(x + 1) + trim), (double)((float)y - trim), (double)((float)z + thickness), uLo, vHi);
				tessellator.addVertexWithUV((double)((float)(x + 1) + trim), (double)((float)(y + 1) + trim), (double)((float)z + thickness), uHi, vHi);
				tessellator.addVertexWithUV((double)((float)x - trim), (double)((float)(y + 1) + trim), (double)((float)z + thickness), uHi, vLo);
				tessellator.addVertexWithUV((double)((float)x - trim), (double)((float)y - trim), (double)((float)z + thickness), uLo, vLo);
			}

			if(blockAccess.isSolid(x, y, z + 1) && blockAccess.getBlockId(x, y + 1, z + 1) == REDSTONE_WIRE_ID) {
				tessellator.addVertexWithUV((double)((float)(x + 1) + trim), (double)((float)(y + 1) + trim), (double)((float)(z + 1) - thickness), uHi, vLo);
				tessellator.addVertexWithUV((double)((float)(x + 1) + trim), (double)((float)y - trim), (double)((float)(z + 1) - thickness), uLo, vLo);
				tessellator.addVertexWithUV((double)((float)x - trim), (double)((float)y - trim), (double)((float)(z + 1) - thickness), uLo, vHi);
				tessellator.addVertexWithUV((double)((float)x - trim), (double)((float)(y + 1) + trim), (double)((float)(z + 1) - thickness), uHi, vHi);
			}
		}

		return true;
	}

	/**
	 * The canonical wire only links to itself here: id {@value #REDSTONE_WIRE_ID}
	 * is the redstone wire, and no block in this version reports
	 * {@code canProvidePower()}.
	 */
	private static boolean isPowerProviderOrWire(IBlockAccess blockAccess, int x, int y, int z) {
		return blockAccess.getBlockId(x, y, z) == REDSTONE_WIRE_ID;
	}
}