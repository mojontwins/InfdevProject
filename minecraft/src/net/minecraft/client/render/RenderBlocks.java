package net.minecraft.client.render;

import net.minecraft.client.render.block.BlockRenderType;
import net.minecraft.game.world.IBlockAccess;
import net.minecraft.game.world.block.Block;
import util.AtlasUV;
import util.AtlasUVBounds;
import util.TextureAtlas;

/**
 * Shared block-face tessellation engine. Holds the per-render state, the six
 * primitive face emitters and the render-type tables; the geometry of each
 * render type lives in a dedicated {@link net.minecraft.client.render.block.BlockRenderHandler}
 * implementation registered in {@link net.minecraft.client.render.block.BlockRenderType},
 * so {@code renderBlockByRenderType} is a single O(1) dispatch instead of a type switch.
 */
public final class RenderBlocks {
	public IBlockAccess blockAccess;
	public int overrideBlockTexture = -1;
	public boolean flipTexture = false;
	/** Temporarily mirrors the horizontal tile on the four side emitters (used by door/bed reverse halves). */
	public boolean mirrorTexture = false;

	/** Offset of each side's neighbour cell, indexed by side 0..5. */
	public static final int[][] NEIGHBOR_OFFSETS = new int[][] {
		{0, -1, 0}, {0, 1, 0}, {0, 0, -1}, {0, 0, 1}, {-1, 0, 0}, {1, 0, 0}
	};
	/** Classic per-side shading: bottom 0.5, top 1.0, north/south 0.8, east/west 0.6. */
	public static final float[] SIDE_LIGHT = new float[] {0.5F, 1.0F, 0.8F, 0.8F, 0.6F, 0.6F};
	/** Outward normal of each side, indexed by side 0..5 (used by inventory previews). */
	public static final float[][] SIDE_NORMALS = new float[][] {
		{0.0F, -1.0F, 0.0F}, {0.0F, 1.0F, 0.0F}, {0.0F, 0.0F, -1.0F}, {0.0F, 0.0F, 1.0F}, {-1.0F, 0.0F, 0.0F}, {1.0F, 0.0F, 0.0F}
	};
	/*
	 * Atlas UV layout is centralized in util.AtlasUV/AtlasUVBounds, which scale
	 * every quad from the atlas dimensions declared in util.TextureAtlas — the
	 * per-site texture math below no longer knows about absolute sizes.
	 */

	public RenderBlocks(IBlockAccess blockAccess) {
		this.blockAccess = blockAccess;
	}

	public RenderBlocks() {
	}

	/** Temporarily forces a specific terrain tile for the next face pass (used for block breaking cracks). */
	public final void renderBlockUsingTexture(Block block, int x, int y, int z, int textureId) {
		this.overrideBlockTexture = textureId;
		this.renderBlockByRenderType(block, x, y, z);
		this.overrideBlockTexture = -1;
	}

	/** Renders every face regardless of occlusion check (used for item previews). */
	public final void renderBlockAllFaces(Block block, int x, int y, int z) {
		this.flipTexture = true;
		this.renderBlockByRenderType(block, x, y, z);
		this.flipTexture = false;
	}

	public final boolean renderBlockByRenderType(Block block, int x, int y, int z) {
		return BlockRenderType.get(block.getRenderType()).handler().renderBlock(this, block, x, y, z);
	}

	/** Draws a single block preview inside an inventory slot, scaled by {@code brightness}. */
	public final void renderBlockOnInventory(Block block, int metadata, float brightness) {
		BlockRenderType.get(block.getRenderType()).handler().renderBlockOnInventory(this, block, metadata, brightness);
	}

	/** Emits one face of the block's current bounds on the given side. */
	public void renderFace(Block block, int side, double x, double y, double z, int textureId) {
		switch(side) {
		case 0:
			this.renderBlockBottom(block, x, y, z, textureId);
			break;
		case 1:
			this.renderBlockTop(block, x, y, z, textureId);
			break;
		case 2:
			this.renderBlockNorth(block, x, y, z, textureId);
			break;
		case 3:
			this.renderBlockSouth(block, x, y, z, textureId);
			break;
		case 4:
			this.renderBlockWest(block, x, y, z, textureId);
			break;
		case 5:
			this.renderBlockEast(block, x, y, z, textureId);
		}

	}

	public void renderBlockBottom(Block block, double x, double y, double z, int side) {
		Tessellator tessellator = Tessellator.instance;
		if(this.overrideBlockTexture >= 0) {
			side = this.overrideBlockTexture;
		}

		AtlasUV.calc(side, TextureAtlas.TERRAIN);
		double uLo = AtlasUV.u1;
		double uHi = AtlasUV.u2;
		double vLo = AtlasUV.v1;
		double vHi = AtlasUV.v2;
		double xMin = x + block.minX;
		double xMax = x + block.maxX;
		double yFloor = y + block.minY;
		double zMin = z + block.minZ;
		double zMax = z + block.maxZ;
		tessellator.addVertexWithUV(xMin, yFloor, zMax, uLo, vHi);
		tessellator.addVertexWithUV(xMin, yFloor, zMin, uLo, vLo);
		tessellator.addVertexWithUV(xMax, yFloor, zMin, uHi, vLo);
		tessellator.addVertexWithUV(xMax, yFloor, zMax, uHi, vHi);
	}

	public void renderBlockTop(Block block, double x, double y, double z, int side) {
		Tessellator tessellator = Tessellator.instance;
		if(this.overrideBlockTexture >= 0) {
			side = this.overrideBlockTexture;
		}

		AtlasUV.calc(side, TextureAtlas.TERRAIN);
		double uLo = AtlasUV.u1;
		double uHi = AtlasUV.u2;
		double vLo = AtlasUV.v1;
		double vHi = AtlasUV.v2;
		double xMin = x + block.minX;
		double xMax = x + block.maxX;
		double yCeiling = y + block.maxY;
		double zMin = z + block.minZ;
		double zMax = z + block.maxZ;
		tessellator.addVertexWithUV(xMax, yCeiling, zMax, uHi, vHi);
		tessellator.addVertexWithUV(xMax, yCeiling, zMin, uHi, vLo);
		tessellator.addVertexWithUV(xMin, yCeiling, zMin, uLo, vLo);
		tessellator.addVertexWithUV(xMin, yCeiling, zMax, uLo, vHi);
	}

	public void renderBlockNorth(Block block, double x, double y, double z, int side) {
		Tessellator tessellator = Tessellator.instance;
		if(this.overrideBlockTexture >= 0) {
			side = this.overrideBlockTexture;
		}

		AtlasUVBounds.calc(block, side);
		double uLo = AtlasUVBounds.u1;
		double uHi = AtlasUVBounds.u2;
		double vTop = AtlasUVBounds.v1;
		double vBottom = AtlasUVBounds.v2;
		if(this.mirrorTexture) {
			double mirrorSwap = uLo;
			uLo = uHi;
			uHi = mirrorSwap;
		}

		double xMin = x + block.minX;
		double xMax = x + block.maxX;
		double yMin = y + block.minY;
		double yMax = y + block.maxY;
		double zNorth = z + block.minZ;
		tessellator.addVertexWithUV(xMin, yMax, zNorth, uHi, vTop);
		tessellator.addVertexWithUV(xMax, yMax, zNorth, uLo, vTop);
		tessellator.addVertexWithUV(xMax, yMin, zNorth, uLo, vBottom);
		tessellator.addVertexWithUV(xMin, yMin, zNorth, uHi, vBottom);
	}

	public void renderBlockSouth(Block block, double x, double y, double z, int side) {
		Tessellator tessellator = Tessellator.instance;
		if(this.overrideBlockTexture >= 0) {
			side = this.overrideBlockTexture;
		}

		AtlasUVBounds.calc(block, side);
		double uLo = AtlasUVBounds.u1;
		double uHi = AtlasUVBounds.u2;
		double vTop = AtlasUVBounds.v1;
		double vBottom = AtlasUVBounds.v2;
		if(this.mirrorTexture) {
			double mirrorSwap = uLo;
			uLo = uHi;
			uHi = mirrorSwap;
		}

		double xMin = x + block.minX;
		double xMax = x + block.maxX;
		double yMin = y + block.minY;
		double yMax = y + block.maxY;
		double zSouth = z + block.maxZ;
		tessellator.addVertexWithUV(xMin, yMax, zSouth, uLo, vTop);
		tessellator.addVertexWithUV(xMin, yMin, zSouth, uLo, vBottom);
		tessellator.addVertexWithUV(xMax, yMin, zSouth, uHi, vBottom);
		tessellator.addVertexWithUV(xMax, yMax, zSouth, uHi, vTop);
	}

	public void renderBlockWest(Block block, double x, double y, double z, int side) {
		Tessellator tessellator = Tessellator.instance;
		if(this.overrideBlockTexture >= 0) {
			side = this.overrideBlockTexture;
		}

		AtlasUVBounds.calc(block, side);
		double uLo = AtlasUVBounds.u1;
		double uHi = AtlasUVBounds.u2;
		double vTop = AtlasUVBounds.v1;
		double vBottom = AtlasUVBounds.v2;
		if(this.mirrorTexture) {
			double mirrorSwap = uLo;
			uLo = uHi;
			uHi = mirrorSwap;
		}

		double xWest = x + block.minX;
		double yMin = y + block.minY;
		double yMax = y + block.maxY;
		double zMin = z + block.minZ;
		double zMax = z + block.maxZ;
		tessellator.addVertexWithUV(xWest, yMax, zMax, uHi, vTop);
		tessellator.addVertexWithUV(xWest, yMax, zMin, uLo, vTop);
		tessellator.addVertexWithUV(xWest, yMin, zMin, uLo, vBottom);
		tessellator.addVertexWithUV(xWest, yMin, zMax, uHi, vBottom);
	}

	public void renderBlockEast(Block block, double x, double y, double z, int side) {
		Tessellator tessellator = Tessellator.instance;
		if(this.overrideBlockTexture >= 0) {
			side = this.overrideBlockTexture;
		}

		AtlasUVBounds.calc(block, side);
		double uLo = AtlasUVBounds.u1;
		double uHi = AtlasUVBounds.u2;
		double vTop = AtlasUVBounds.v1;
		double vBottom = AtlasUVBounds.v2;
		if(this.mirrorTexture) {
			double mirrorSwap = uLo;
			uLo = uHi;
			uHi = mirrorSwap;
		}

		double xEast = x + block.maxX;
		double yMin = y + block.minY;
		double yMax = y + block.maxY;
		double zMin = z + block.minZ;
		double zMax = z + block.maxZ;
		tessellator.addVertexWithUV(xEast, yMin, zMax, uLo, vBottom);
		tessellator.addVertexWithUV(xEast, yMin, zMin, uHi, vBottom);
		tessellator.addVertexWithUV(xEast, yMax, zMin, uHi, vTop);
		tessellator.addVertexWithUV(xEast, yMax, zMax, uLo, vTop);
	}
}