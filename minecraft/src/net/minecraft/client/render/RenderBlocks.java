package net.minecraft.client.render;

import net.minecraft.game.world.IBlockAccess;
import net.minecraft.game.world.block.Block;
import net.minecraft.game.world.material.Material;
import org.lwjgl.opengl.GL11;

/**
 * Emits tessellator vertices and UV coordinates for every block render type:
 * standard cubes, water slabs, plants/crops (crosses), torches, fire and
 * ladders. Also used for the inventory item previews (see
 * {@link #renderBlockOnInventory}).
 */
public final class RenderBlocks {
	private IBlockAccess blockAccess;
	private int overrideBlockTexture = -1;
	private boolean flipTexture = false;

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
		int renderType = block.getRenderType();
		Tessellator tessellator;
		boolean anyFaceRendered;
		if(renderType == 0) {
			tessellator = Tessellator.instance;
			anyFaceRendered = false;
			float selfBrightness = block.getBlockBrightness(this.blockAccess, x, y, z);
			float neighborBrightness;

			// Bottom face — darker, only drawn when nothing below.
			if(this.flipTexture || block.shouldSideBeRendered(this.blockAccess, x, y - 1, z, 0)) {
				neighborBrightness = block.getBlockBrightness(this.blockAccess, x, y - 1, z);
				if(Block.lightValue[block.blockID] > 0) {
					neighborBrightness = 1.0F;
				}

				tessellator.setColorOpaque_F(0.5F * neighborBrightness, 0.5F * neighborBrightness, 0.5F * neighborBrightness);
				this.renderBlockBottom(block, (double)x, (double)y, (double)z, block.getBlockTexture(this.blockAccess, x, y, z, 0));
				anyFaceRendered = true;
			}

			// Top face — full brightness.
			if(this.flipTexture || block.shouldSideBeRendered(this.blockAccess, x, y + 1, z, 1)) {
				neighborBrightness = block.getBlockBrightness(this.blockAccess, x, y + 1, z);
				if(block.maxY != 1.0D && !block.blockMaterial.getIsLiquid()) {
					neighborBrightness = selfBrightness;
				}

				if(Block.lightValue[block.blockID] > 0) {
					neighborBrightness = 1.0F;
				}

				tessellator.setColorOpaque_F(neighborBrightness * 1.0F, neighborBrightness * 1.0F, neighborBrightness * 1.0F);
				this.renderBlockTop(block, (double)x, (double)y, (double)z, block.getBlockTexture(this.blockAccess, x, y, z, 1));
				anyFaceRendered = true;
			}

			// North/south faces — 0.8 brightness.
			if(this.flipTexture || block.shouldSideBeRendered(this.blockAccess, x, y, z - 1, 2)) {
				neighborBrightness = block.getBlockBrightness(this.blockAccess, x, y, z - 1);
				if(Block.lightValue[block.blockID] > 0) {
					neighborBrightness = 1.0F;
				}

				tessellator.setColorOpaque_F(0.8F * neighborBrightness, 0.8F * neighborBrightness, 0.8F * neighborBrightness);
				this.renderBlockNorth(block, (double)x, (double)y, (double)z, block.getBlockTexture(this.blockAccess, x, y, z, 2));
				anyFaceRendered = true;
			}

			if(this.flipTexture || block.shouldSideBeRendered(this.blockAccess, x, y, z + 1, 3)) {
				neighborBrightness = block.getBlockBrightness(this.blockAccess, x, y, z + 1);
				if(Block.lightValue[block.blockID] > 0) {
					neighborBrightness = 1.0F;
				}

				tessellator.setColorOpaque_F(0.8F * neighborBrightness, 0.8F * neighborBrightness, 0.8F * neighborBrightness);
				this.renderBlockSouth(block, (double)x, (double)y, (double)z, block.getBlockTexture(this.blockAccess, x, y, z, 3));
				anyFaceRendered = true;
			}

			// East/west faces — 0.6 brightness.
			if(this.flipTexture || block.shouldSideBeRendered(this.blockAccess, x - 1, y, z, 4)) {
				neighborBrightness = block.getBlockBrightness(this.blockAccess, x - 1, y, z);
				if(Block.lightValue[block.blockID] > 0) {
					neighborBrightness = 1.0F;
				}

				tessellator.setColorOpaque_F(0.6F * neighborBrightness, 0.6F * neighborBrightness, 0.6F * neighborBrightness);
				this.renderBlockWest(block, (double)x, (double)y, (double)z, block.getBlockTexture(this.blockAccess, x, y, z, 4));
				anyFaceRendered = true;
			}

			if(this.flipTexture || block.shouldSideBeRendered(this.blockAccess, x + 1, y, z, 5)) {
				neighborBrightness = block.getBlockBrightness(this.blockAccess, x + 1, y, z);
				if(Block.lightValue[block.blockID] > 0) {
					neighborBrightness = 1.0F;
				}

				tessellator.setColorOpaque_F(0.6F * neighborBrightness, 0.6F * neighborBrightness, 0.6F * neighborBrightness);
				this.renderBlockEast(block, (double)x, (double)y, (double)z, block.getBlockTexture(this.blockAccess, x, y, z, 5));
				anyFaceRendered = true;
			}

			return anyFaceRendered;
		} else {
			double savedMinY;
			double savedMaxY;
			if(renderType == 4) {
				// Water/liquid: the top surface sits at the liquid level, and each
				// side wall runs down from this cell's level to its neighbor's.
				tessellator = Tessellator.instance;
				anyFaceRendered = false;
				savedMinY = block.minY;
				savedMaxY = block.maxY;
				block.maxY = savedMaxY - (double)this.materialNotWater(x, y, z);
				float sideBrightness;
				if(this.flipTexture || block.shouldSideBeRendered(this.blockAccess, x, y - 1, z, 0)) {
					sideBrightness = block.getBlockBrightness(this.blockAccess, x, y - 1, z);
					tessellator.setColorOpaque_F(0.5F * sideBrightness, 0.5F * sideBrightness, 0.5F * sideBrightness);
					this.renderBlockBottom(block, (double)x, (double)y, (double)z, block.getBlockTextureFromSide(0));
					anyFaceRendered = true;
				}

				if(this.flipTexture || block.shouldSideBeRendered(this.blockAccess, x, y + 1, z, 1)) {
					sideBrightness = block.getBlockBrightness(this.blockAccess, x, y + 1, z);
					tessellator.setColorOpaque_F(sideBrightness * 1.0F, sideBrightness * 1.0F, sideBrightness * 1.0F);
					this.renderBlockTop(block, (double)x, (double)y, (double)z, block.getBlockTextureFromSide(1));
					anyFaceRendered = true;
				}

				block.minY = savedMaxY - (double)this.materialNotWater(x, y, z - 1);
				if(this.flipTexture || block.maxY > block.minY || block.shouldSideBeRendered(this.blockAccess, x, y, z - 1, 2)) {
					sideBrightness = block.getBlockBrightness(this.blockAccess, x, y, z - 1);
					tessellator.setColorOpaque_F(0.8F * sideBrightness, 0.8F * sideBrightness, 0.8F * sideBrightness);
					this.renderBlockNorth(block, (double)x, (double)y, (double)z, block.getBlockTextureFromSide(2));
					anyFaceRendered = true;
				}

				block.minY = savedMaxY - (double)this.materialNotWater(x, y, z + 1);
				if(this.flipTexture || block.maxY > block.minY || block.shouldSideBeRendered(this.blockAccess, x, y, z + 1, 3)) {
					sideBrightness = block.getBlockBrightness(this.blockAccess, x, y, z + 1);
					tessellator.setColorOpaque_F(0.8F * sideBrightness, 0.8F * sideBrightness, 0.8F * sideBrightness);
					this.renderBlockSouth(block, (double)x, (double)y, (double)z, block.getBlockTextureFromSide(3));
					anyFaceRendered = true;
				}

				block.minY = savedMaxY - (double)this.materialNotWater(x - 1, y, z);
				if(this.flipTexture || block.maxY > block.minY || block.shouldSideBeRendered(this.blockAccess, x - 1, y, z, 4)) {
					sideBrightness = block.getBlockBrightness(this.blockAccess, x - 1, y, z);
					tessellator.setColorOpaque_F(0.6F * sideBrightness, 0.6F * sideBrightness, 0.6F * sideBrightness);
					this.renderBlockWest(block, (double)x, (double)y, (double)z, block.getBlockTextureFromSide(4));
					anyFaceRendered = true;
				}

				block.minY = savedMaxY - (double)this.materialNotWater(x + 1, y, z);
				if(this.flipTexture || block.maxY > block.minY || block.shouldSideBeRendered(this.blockAccess, x + 1, y, z, 5)) {
					sideBrightness = block.getBlockBrightness(this.blockAccess, x + 1, y, z);
					tessellator.setColorOpaque_F(0.6F * sideBrightness, 0.6F * sideBrightness, 0.6F * sideBrightness);
					this.renderBlockEast(block, (double)x, (double)y, (double)z, block.getBlockTextureFromSide(5));
					anyFaceRendered = true;
				}

				block.minY = savedMinY;
				block.maxY = savedMaxY;
				return anyFaceRendered;
			} else {
				float fullBrightness;
				if(renderType == 1) {
					tessellator = Tessellator.instance;
					fullBrightness = block.getBlockBrightness(this.blockAccess, x, y, z);
					tessellator.setColorOpaque_F(fullBrightness, fullBrightness, fullBrightness);
					this.renderBlockPlant(block, this.blockAccess.getBlockMetadata(x, y, z), (double)x, (double)y, (double)z);
					return true;
				} else if(renderType == 6) {
					tessellator = Tessellator.instance;
					fullBrightness = block.getBlockBrightness(this.blockAccess, x, y, z);
					tessellator.setColorOpaque_F(fullBrightness, fullBrightness, fullBrightness);
					this.renderBlockCrops(block, this.blockAccess.getBlockMetadata(x, y, z), (double)x, (double)((float)y - 1.0F / 16.0F), (double)z);
					return true;
				} else {
					float torchBrightness;
					if(renderType == 2) {
						int metadata = this.blockAccess.getBlockMetadata(x, y, z);
						tessellator = Tessellator.instance;
						torchBrightness = block.getBlockBrightness(this.blockAccess, x, y, z);
						if(Block.lightValue[block.blockID] > 0) {
							torchBrightness = 1.0F;
						}

						tessellator.setColorOpaque_F(torchBrightness, torchBrightness, torchBrightness);
						if(metadata == 1) {
							// Leaning against the west neighbour.
							this.renderBlockTorch(block, (double)x - (double)0.099999994F, (double)y + (double)0.2F, (double)z, (double)-0.4F, 0.0D);
						} else if(metadata == 2) {
							// Leaning against the east neighbour.
							this.renderBlockTorch(block, (double)x + (double)0.099999994F, (double)y + (double)0.2F, (double)z, (double)0.4F, 0.0D);
						} else if(metadata == 3) {
							// Leaning against the south neighbour.
							this.renderBlockTorch(block, (double)x, (double)y + (double)0.2F, (double)z - (double)0.099999994F, 0.0D, (double)-0.4F);
						} else if(metadata == 4) {
							// Leaning against the north neighbour.
							this.renderBlockTorch(block, (double)x, (double)y + (double)0.2F, (double)z + (double)0.099999994F, 0.0D, (double)0.4F);
						} else {
							// Upright torch.
							this.renderBlockTorch(block, (double)x, (double)y, (double)z, 0.0D, 0.0D);
						}

						return true;
					} else {
						int textureId;
						double xHigh;
						double xLow;
						double zHigh;
						double zLow;
						int vTile;
						if(renderType == 3) {
							// Fire. Contrary to the usual coordinate ordering, in
							// this branch x/y/z keep their original meaning below.
							tessellator = Tessellator.instance;
							textureId = block.getBlockTextureFromSide(0);
							if(this.overrideBlockTexture >= 0) {
								textureId = this.overrideBlockTexture;
							}

							torchBrightness = block.getBlockBrightness(this.blockAccess, x, y, z);
							tessellator.setColorOpaque_F(torchBrightness, torchBrightness, torchBrightness);
							int uTile = (textureId & 15) << 4;
							vTile = textureId & 240;
							double uLo = (double)((float)uTile / 256.0F);
							double uHi = (double)(((float)uTile + 15.99F) / 256.0F);
							double vLo = (double)((float)vTile / 256.0F);
							double vHi = (double)(((float)vTile + 15.99F) / 256.0F);
							double uTmp;
							if(!this.blockAccess.isSolid(x, y - 1, z) && !Block.fire.canBlockCatchFire(this.blockAccess, x, y - 1, z)) {
								// Small fire: the flame is a couple of crossed two-sided
								// quads hovered slightly above the floor, using the
								// second texture row for the alternating flicker tile.
								if((x + y + z & 1) == 1) {
									uLo = (double)((float)uTile / 256.0F);
									uHi = (double)(((float)uTile + 15.99F) / 256.0F);
									vLo = (double)((float)(vTile + 16) / 256.0F);
									vHi = (double)(((float)vTile + 15.99F + 16.0F) / 256.0F);
								}

								if((x / 2 + y / 2 + z / 2 & 1) == 1) {
									uTmp = uHi;
									uHi = uLo;
									uLo = uTmp;
								}

								// Flames spill out through each burnable neighbour.
								if(Block.fire.canBlockCatchFire(this.blockAccess, x - 1, y, z)) {
									tessellator.addVertexWithUV((double)((float)x + 0.2F), (double)((float)y + 1.4F + 1.0F / 16.0F), (double)(z + 1), uHi, vLo);
									tessellator.addVertexWithUV((double)x, (double)((float)y + 1.0F / 16.0F), (double)(z + 1), uHi, vHi);
									tessellator.addVertexWithUV((double)x, (double)((float)y + 1.0F / 16.0F), (double)z, uLo, vHi);
									tessellator.addVertexWithUV((double)((float)x + 0.2F), (double)((float)y + 1.4F + 1.0F / 16.0F), (double)z, uLo, vLo);
									tessellator.addVertexWithUV((double)((float)x + 0.2F), (double)((float)y + 1.4F + 1.0F / 16.0F), (double)z, uLo, vLo);
									tessellator.addVertexWithUV((double)x, (double)((float)y + 1.0F / 16.0F), (double)z, uLo, vHi);
									tessellator.addVertexWithUV((double)x, (double)((float)y + 1.0F / 16.0F), (double)(z + 1), uHi, vHi);
									tessellator.addVertexWithUV((double)((float)x + 0.2F), (double)((float)y + 1.4F + 1.0F / 16.0F), (double)(z + 1), uHi, vLo);
								}

								if(Block.fire.canBlockCatchFire(this.blockAccess, x + 1, y, z)) {
									tessellator.addVertexWithUV((double)((float)(x + 1) - 0.2F), (double)((float)y + 1.4F + 1.0F / 16.0F), (double)z, uLo, vLo);
									tessellator.addVertexWithUV((double)(x + 1), (double)((float)y + 1.0F / 16.0F), (double)z, uLo, vHi);
									tessellator.addVertexWithUV((double)(x + 1), (double)((float)y + 1.0F / 16.0F), (double)(z + 1), uHi, vHi);
									tessellator.addVertexWithUV((double)((float)(x + 1) - 0.2F), (double)((float)y + 1.4F + 1.0F / 16.0F), (double)(z + 1), uHi, vLo);
									tessellator.addVertexWithUV((double)((float)(x + 1) - 0.2F), (double)((float)y + 1.4F + 1.0F / 16.0F), (double)(z + 1), uHi, vLo);
									tessellator.addVertexWithUV((double)(x + 1), (double)((float)y + 1.0F / 16.0F), (double)(z + 1), uHi, vHi);
									tessellator.addVertexWithUV((double)(x + 1), (double)((float)y + 1.0F / 16.0F), (double)z, uLo, vHi);
									tessellator.addVertexWithUV((double)((float)(x + 1) - 0.2F), (double)((float)y + 1.4F + 1.0F / 16.0F), (double)z, uLo, vLo);
								}

								if(Block.fire.canBlockCatchFire(this.blockAccess, x, y, z - 1)) {
									tessellator.addVertexWithUV((double)x, (double)((float)y + 1.4F + 1.0F / 16.0F), (double)((float)z + 0.2F), uHi, vLo);
									tessellator.addVertexWithUV((double)x, (double)((float)y + 1.0F / 16.0F), (double)z, uHi, vHi);
									tessellator.addVertexWithUV((double)(x + 1), (double)((float)y + 1.0F / 16.0F), (double)z, uLo, vHi);
									tessellator.addVertexWithUV((double)(x + 1), (double)((float)y + 1.4F + 1.0F / 16.0F), (double)((float)z + 0.2F), uLo, vLo);
									tessellator.addVertexWithUV((double)(x + 1), (double)((float)y + 1.4F + 1.0F / 16.0F), (double)((float)z + 0.2F), uLo, vLo);
									tessellator.addVertexWithUV((double)(x + 1), (double)((float)y + 1.0F / 16.0F), (double)z, uLo, vHi);
									tessellator.addVertexWithUV((double)x, (double)((float)y + 1.0F / 16.0F), (double)z, uHi, vHi);
									tessellator.addVertexWithUV((double)x, (double)((float)y + 1.4F + 1.0F / 16.0F), (double)((float)z + 0.2F), uHi, vLo);
								}

								if(Block.fire.canBlockCatchFire(this.blockAccess, x, y, z + 1)) {
									tessellator.addVertexWithUV((double)(x + 1), (double)((float)y + 1.4F + 1.0F / 16.0F), (double)((float)(z + 1) - 0.2F), uLo, vLo);
									tessellator.addVertexWithUV((double)(x + 1), (double)((float)y + 1.0F / 16.0F), (double)(z + 1), uLo, vHi);
									tessellator.addVertexWithUV((double)x, (double)((float)y + 1.0F / 16.0F), (double)(z + 1), uHi, vHi);
									tessellator.addVertexWithUV((double)x, (double)((float)y + 1.4F + 1.0F / 16.0F), (double)((float)(z + 1) - 0.2F), uHi, vLo);
									tessellator.addVertexWithUV((double)x, (double)((float)y + 1.4F + 1.0F / 16.0F), (double)((float)(z + 1) - 0.2F), uHi, vLo);
									tessellator.addVertexWithUV((double)x, (double)((float)y + 1.0F / 16.0F), (double)(z + 1), uHi, vHi);
									tessellator.addVertexWithUV((double)(x + 1), (double)((float)y + 1.0F / 16.0F), (double)(z + 1), uLo, vHi);
									tessellator.addVertexWithUV((double)(x + 1), (double)((float)y + 1.4F + 1.0F / 16.0F), (double)((float)(z + 1) - 0.2F), uLo, vLo);
								}

								// Tiny licking flame on top of the block.
								if(Block.fire.canBlockCatchFire(this.blockAccess, x, y + 1, z)) {
									xHigh = (double)x + 0.5D + 0.5D;
									xLow = (double)x + 0.5D - 0.5D;
									zHigh = (double)z + 0.5D + 0.5D;
									zLow = (double)z + 0.5D - 0.5D;
									uLo = (double)((float)uTile / 256.0F);
									uHi = (double)(((float)uTile + 15.99F) / 256.0F);
									vLo = (double)((float)vTile / 256.0F);
									vHi = (double)(((float)vTile + 15.99F) / 256.0F);
									int flameY = y + 1;
									if((x + flameY + z & 1) == 0) {
										tessellator.addVertexWithUV(xLow, (double)((float)flameY + -0.2F), (double)z, uHi, vLo);
										tessellator.addVertexWithUV(xHigh, (double)flameY, (double)z, uHi, vHi);
										tessellator.addVertexWithUV(xHigh, (double)flameY, (double)(z + 1), uLo, vHi);
										tessellator.addVertexWithUV(xLow, (double)((float)flameY + -0.2F), (double)(z + 1), uLo, vLo);
										uLo = (double)((float)uTile / 256.0F);
										uHi = (double)(((float)uTile + 15.99F) / 256.0F);
										vLo = (double)((float)(vTile + 16) / 256.0F);
										vHi = (double)(((float)vTile + 15.99F + 16.0F) / 256.0F);
										tessellator.addVertexWithUV(xHigh, (double)((float)flameY + -0.2F), (double)(z + 1), uHi, vLo);
										tessellator.addVertexWithUV(xLow, (double)flameY, (double)(z + 1), uHi, vHi);
										tessellator.addVertexWithUV(xLow, (double)flameY, (double)z, uLo, vHi);
										tessellator.addVertexWithUV(xHigh, (double)((float)flameY + -0.2F), (double)z, uLo, vLo);
									} else {
										tessellator.addVertexWithUV((double)x, (double)((float)flameY + -0.2F), zHigh, uHi, vLo);
										tessellator.addVertexWithUV((double)x, (double)flameY, zLow, uHi, vHi);
										tessellator.addVertexWithUV((double)(x + 1), (double)flameY, zLow, uLo, vHi);
										tessellator.addVertexWithUV((double)(x + 1), (double)((float)flameY + -0.2F), zHigh, uLo, vLo);
										uLo = (double)((float)uTile / 256.0F);
										uHi = (double)(((float)uTile + 15.99F) / 256.0F);
										vLo = (double)((float)(vTile + 16) / 256.0F);
										vHi = (double)(((float)vTile + 15.99F + 16.0F) / 256.0F);
										tessellator.addVertexWithUV((double)(x + 1), (double)((float)flameY + -0.2F), zLow, uHi, vLo);
										tessellator.addVertexWithUV((double)(x + 1), (double)flameY, zHigh, uHi, vHi);
										tessellator.addVertexWithUV((double)x, (double)flameY, zHigh, uLo, vHi);
										tessellator.addVertexWithUV((double)x, (double)((float)flameY + -0.2F), zLow, uLo, vLo);
									}
								}
							} else {
								// Full fire: four crossed oversized quads around a lean,
								// notched top, using both texture rows for flicker.
								xHigh = (double)x + 0.5D + 0.2D;
								xLow = (double)x + 0.5D - 0.2D;
								zHigh = (double)z + 0.5D + 0.2D;
								zLow = (double)z + 0.5D - 0.2D;
								uTmp = (double)x + 0.5D - 0.3D;
								double xWideHigh = (double)x + 0.5D + 0.3D;
								double zWideLow = (double)z + 0.5D - 0.3D;
								double zWideHigh = (double)z + 0.5D + 0.3D;
								tessellator.addVertexWithUV(uTmp, (double)((float)y + 1.4F), (double)(z + 1), uHi, vLo);
								tessellator.addVertexWithUV(xHigh, (double)y, (double)(z + 1), uHi, vHi);
								tessellator.addVertexWithUV(xHigh, (double)y, (double)z, uLo, vHi);
								tessellator.addVertexWithUV(uTmp, (double)((float)y + 1.4F), (double)z, uLo, vLo);
								tessellator.addVertexWithUV(xWideHigh, (double)((float)y + 1.4F), (double)z, uHi, vLo);
								tessellator.addVertexWithUV(xLow, (double)y, (double)z, uHi, vHi);
								tessellator.addVertexWithUV(xLow, (double)y, (double)(z + 1), uLo, vHi);
								tessellator.addVertexWithUV(xWideHigh, (double)((float)y + 1.4F), (double)(z + 1), uLo, vLo);
								uLo = (double)((float)uTile / 256.0F);
								uHi = (double)(((float)uTile + 15.99F) / 256.0F);
								vLo = (double)((float)(vTile + 16) / 256.0F);
								vHi = (double)(((float)vTile + 15.99F + 16.0F) / 256.0F);
								tessellator.addVertexWithUV((double)(x + 1), (double)((float)y + 1.4F), zWideHigh, uHi, vLo);
								tessellator.addVertexWithUV((double)(x + 1), (double)y, zLow, uHi, vHi);
								tessellator.addVertexWithUV((double)x, (double)y, zLow, uLo, vHi);
								tessellator.addVertexWithUV((double)x, (double)((float)y + 1.4F), zWideHigh, uLo, vLo);
								tessellator.addVertexWithUV((double)x, (double)((float)y + 1.4F), zWideLow, uHi, vLo);
								tessellator.addVertexWithUV((double)x, (double)y, zHigh, uHi, vHi);
								tessellator.addVertexWithUV((double)(x + 1), (double)y, zHigh, uLo, vHi);
								tessellator.addVertexWithUV((double)(x + 1), (double)((float)y + 1.4F), zWideLow, uLo, vLo);
								xHigh = (double)x + 0.5D - 0.5D;
								xLow = (double)x + 0.5D + 0.5D;
								zHigh = (double)z + 0.5D - 0.5D;
								zLow = (double)z + 0.5D + 0.5D;
								uTmp = (double)x + 0.5D - 0.4D;
								xWideHigh = (double)x + 0.5D + 0.4D;
								zWideLow = (double)z + 0.5D - 0.4D;
								zWideHigh = (double)z + 0.5D + 0.4D;
								uLo = (double)((float)uTile / 256.0F);
								uHi = (double)(((float)uTile + 15.99F) / 256.0F);
								vLo = (double)((float)vTile / 256.0F);
								vHi = (double)(((float)vTile + 15.99F) / 256.0F);
								tessellator.addVertexWithUV(uTmp, (double)((float)y + 1.4F), (double)z, uLo, vLo);
								tessellator.addVertexWithUV(xHigh, (double)y, (double)z, uLo, vHi);
								tessellator.addVertexWithUV(xHigh, (double)y, (double)(z + 1), uHi, vHi);
								tessellator.addVertexWithUV(uTmp, (double)((float)y + 1.4F), (double)(z + 1), uHi, vLo);
								tessellator.addVertexWithUV(xWideHigh, (double)((float)y + 1.4F), (double)(z + 1), uLo, vLo);
								tessellator.addVertexWithUV(xLow, (double)y, (double)(z + 1), uLo, vHi);
								tessellator.addVertexWithUV(xLow, (double)y, (double)z, uHi, vHi);
								tessellator.addVertexWithUV(xWideHigh, (double)((float)y + 1.4F), (double)z, uHi, vLo);
								uLo = (double)((float)uTile / 256.0F);
								uHi = (double)(((float)uTile + 15.99F) / 256.0F);
								vLo = (double)((float)(vTile + 16) / 256.0F);
								vHi = (double)(((float)vTile + 15.99F + 16.0F) / 256.0F);
								tessellator.addVertexWithUV((double)x, (double)((float)y + 1.4F), zWideHigh, uLo, vLo);
								tessellator.addVertexWithUV((double)x, (double)y, zLow, uLo, vHi);
								tessellator.addVertexWithUV((double)(x + 1), (double)y, zLow, uHi, vHi);
								tessellator.addVertexWithUV((double)(x + 1), (double)((float)y + 1.4F), zWideHigh, uHi, vLo);
								tessellator.addVertexWithUV((double)(x + 1), (double)((float)y + 1.4F), zWideLow, uLo, vLo);
								tessellator.addVertexWithUV((double)(x + 1), (double)y, zHigh, uLo, vHi);
								tessellator.addVertexWithUV((double)x, (double)y, zHigh, uHi, vHi);
								tessellator.addVertexWithUV((double)x, (double)((float)y + 1.4F), zWideLow, uHi, vLo);
							}

							return true;
						} else if(renderType == 5) {
							// Ladder: uses the two adjacent texture columns
							// (the ladder rungs), swapped by world parity.
							tessellator = Tessellator.instance;
							textureId = block.getBlockTextureFromSide(0);
							if(this.overrideBlockTexture >= 0) {
								textureId = this.overrideBlockTexture;
							}

							torchBrightness = block.getBlockBrightness(this.blockAccess, x, y, z);
							tessellator.setColorOpaque_F(torchBrightness, torchBrightness, torchBrightness);
							int uLadderColumn = ((textureId & 15) << 4) + 16;
							int uBaseColumn = (textureId & 15) << 4;
							vTile = textureId & 240;
							if((x + y + z & 1) == 1) {
								uLadderColumn = (textureId & 15) << 4;
								uBaseColumn = ((textureId & 15) << 4) + 16;
							}

							double uColLo = (double)((float)uLadderColumn / 256.0F);
							double uColHi = (double)(((float)uLadderColumn + 15.99F) / 256.0F);
							double vTileLo = (double)((float)vTile / 256.0F);
							double vTileHi = (double)(((float)vTile + 15.99F) / 256.0F);
							double uBaseLo = (double)((float)uBaseColumn / 256.0F);
							double uBaseHi = (double)(((float)uBaseColumn + 15.99F) / 256.0F);
							double vBaseLo = (double)((float)vTile / 256.0F);
							double vBaseHi = (double)(((float)vTile + 15.99F) / 256.0F);
							if(this.blockAccess.isSolid(x - 1, y, z)) {
								tessellator.addVertexWithUV((double)((float)x + 0.05F), (double)((float)(y + 1) + 2.0F / 16.0F), (double)((float)(z + 1) + 2.0F / 16.0F), uColLo, vTileLo);
								tessellator.addVertexWithUV((double)((float)x + 0.05F), (double)((float)y - 2.0F / 16.0F), (double)((float)(z + 1) + 2.0F / 16.0F), uColLo, vTileHi);
								tessellator.addVertexWithUV((double)((float)x + 0.05F), (double)((float)y - 2.0F / 16.0F), (double)((float)z - 2.0F / 16.0F), uColHi, vTileHi);
								tessellator.addVertexWithUV((double)((float)x + 0.05F), (double)((float)(y + 1) + 2.0F / 16.0F), (double)((float)z - 2.0F / 16.0F), uColHi, vTileLo);
							}

							if(this.blockAccess.isSolid(x + 1, y, z)) {
								tessellator.addVertexWithUV((double)((float)(x + 1) - 0.05F), (double)((float)y - 2.0F / 16.0F), (double)((float)(z + 1) + 2.0F / 16.0F), uColHi, vTileHi);
								tessellator.addVertexWithUV((double)((float)(x + 1) - 0.05F), (double)((float)(y + 1) + 2.0F / 16.0F), (double)((float)(z + 1) + 2.0F / 16.0F), uColHi, vTileLo);
								tessellator.addVertexWithUV((double)((float)(x + 1) - 0.05F), (double)((float)(y + 1) + 2.0F / 16.0F), (double)((float)z - 2.0F / 16.0F), uColLo, vTileLo);
								tessellator.addVertexWithUV((double)((float)(x + 1) - 0.05F), (double)((float)y - 2.0F / 16.0F), (double)((float)z - 2.0F / 16.0F), uColLo, vTileHi);
							}

							if(this.blockAccess.isSolid(x, y, z - 1)) {
								tessellator.addVertexWithUV((double)((float)(x + 1) + 2.0F / 16.0F), (double)((float)y - 2.0F / 16.0F), (double)((float)z + 0.05F), uBaseHi, vBaseHi);
								tessellator.addVertexWithUV((double)((float)(x + 1) + 2.0F / 16.0F), (double)((float)(y + 1) + 2.0F / 16.0F), (double)((float)z + 0.05F), uBaseHi, vBaseLo);
								tessellator.addVertexWithUV((double)((float)x - 2.0F / 16.0F), (double)((float)(y + 1) + 2.0F / 16.0F), (double)((float)z + 0.05F), uBaseLo, vBaseLo);
								tessellator.addVertexWithUV((double)((float)x - 2.0F / 16.0F), (double)((float)y - 2.0F / 16.0F), (double)((float)z + 0.05F), uBaseLo, vBaseHi);
							}

							if(this.blockAccess.isSolid(x, y, z + 1)) {
								tessellator.addVertexWithUV((double)((float)(x + 1) + 2.0F / 16.0F), (double)((float)(y + 1) + 2.0F / 16.0F), (double)((float)(z + 1) - 0.05F), uBaseLo, vBaseLo);
								tessellator.addVertexWithUV((double)((float)(x + 1) + 2.0F / 16.0F), (double)((float)y - 2.0F / 16.0F), (double)((float)(z + 1) - 0.05F), uBaseLo, vBaseHi);
								tessellator.addVertexWithUV((double)((float)x - 2.0F / 16.0F), (double)((float)y - 2.0F / 16.0F), (double)((float)(z + 1) - 0.05F), uBaseHi, vBaseHi);
								tessellator.addVertexWithUV((double)((float)x - 2.0F / 16.0F), (double)((float)(y + 1) + 2.0F / 16.0F), (double)((float)(z + 1) - 0.05F), uBaseHi, vBaseLo);
							}

							return true;
						} else {
							return false;
						}
					}
				}
			}
		}
	}

	private void renderBlockTorch(Block block, double x, double y, double z, double leanX, double leanZ) {
		Tessellator tessellator = Tessellator.instance;
		int textureId = block.getBlockTextureFromSide(0);
		if(this.overrideBlockTexture >= 0) {
			textureId = this.overrideBlockTexture;
		}

		int uTile = (textureId & 15) << 4;
		textureId &= 240;
		float uLo = (float)uTile / 256.0F;
		float uHi = ((float)uTile + 15.99F) / 256.0F;
		float vLo = (float)textureId / 256.0F;
		float vHi = ((float)textureId + 15.99F) / 256.0F;
		// The lighter-coloured cross on the torch head.
		double headEdgeU = (double)uLo + 1.75D / 64.0D;
		double headTopV = (double)vLo + 6.0D / 256.0D;
		double headEdgeU2 = (double)uLo + 9.0D / 256.0D;
		double headBottomV = (double)vLo + 1.0D / 32.0D;
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

	/** Render type 1: the two crossed two-sided quads that make a plant. */
	private void renderBlockPlant(Block block, int metadata, double x, double y, double z) {
		Tessellator tessellator = Tessellator.instance;
		int textureId = block.getBlockTextureFromSideAndMetadata(0, metadata);
		if(this.overrideBlockTexture >= 0) {
			textureId = this.overrideBlockTexture;
		}

		int uTile = (textureId & 15) << 4;
		textureId &= 240;
		double uLo = (double)((float)uTile / 256.0F);
		double uHi = (double)(((float)uTile + 15.99F) / 256.0F);
		double vLo = (double)((float)textureId / 256.0F);
		double vHi = (double)(((float)textureId + 15.99F) / 256.0F);
		// 0.45F arm width keeps the four leaves visually separated.
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

	/**
	 * Render type 6: a double cross (both X- and Z-parallel planes) with the
	 * stage-specific tile chosen via metadata.
	 */
	private void renderBlockCrops(Block block, int metadata, double x, double y, double z) {
		Tessellator tessellator = Tessellator.instance;
		int textureId = block.getBlockTextureFromSideAndMetadata(0, metadata);
		if(this.overrideBlockTexture >= 0) {
			textureId = this.overrideBlockTexture;
		}

		int uTile = (textureId & 15) << 4;
		textureId &= 240;
		double uLo = (double)((float)uTile / 256.0F);
		double uHi = (double)(((float)uTile + 15.99F) / 256.0F);
		double vLo = (double)((float)textureId / 256.0F);
		double vHi = (double)(((float)textureId + 15.99F) / 256.0F);
		// X-parallel planes, half a block deep in Z.
		double xLow = x + 0.5D - 0.25D;
		double xHigh = x + 0.5D + 0.25D;
		double zLow = z + 0.5D - 0.5D;
		double zHigh = z + 0.5D + 0.5D;
		tessellator.addVertexWithUV(xLow, y + 1.0D, zLow, uLo, vLo);
		tessellator.addVertexWithUV(xLow, y, zLow, uLo, vHi);
		tessellator.addVertexWithUV(xLow, y, zHigh, uHi, vHi);
		tessellator.addVertexWithUV(xLow, y + 1.0D, zHigh, uHi, vLo);
		tessellator.addVertexWithUV(xLow, y + 1.0D, zHigh, uLo, vLo);
		tessellator.addVertexWithUV(xLow, y, zHigh, uLo, vHi);
		tessellator.addVertexWithUV(xLow, y, zLow, uHi, vHi);
		tessellator.addVertexWithUV(xLow, y + 1.0D, zLow, uHi, vLo);
		tessellator.addVertexWithUV(xHigh, y + 1.0D, zHigh, uLo, vLo);
		tessellator.addVertexWithUV(xHigh, y, zHigh, uLo, vHi);
		tessellator.addVertexWithUV(xHigh, y, zLow, uHi, vHi);
		tessellator.addVertexWithUV(xHigh, y + 1.0D, zLow, uHi, vLo);
		tessellator.addVertexWithUV(xHigh, y + 1.0D, zLow, uLo, vLo);
		tessellator.addVertexWithUV(xHigh, y, zLow, uLo, vHi);
		tessellator.addVertexWithUV(xHigh, y, zHigh, uHi, vHi);
		tessellator.addVertexWithUV(xHigh, y + 1.0D, zHigh, uHi, vLo);
		// Z-parallel planes, half a block deep in X.
		xLow = x + 0.5D - 0.5D;
		xHigh = x + 0.5D + 0.5D;
		zLow = z + 0.5D - 0.25D;
		zHigh = z + 0.5D + 0.25D;
		tessellator.addVertexWithUV(xLow, y + 1.0D, zLow, uLo, vLo);
		tessellator.addVertexWithUV(xLow, y, zLow, uLo, vHi);
		tessellator.addVertexWithUV(xHigh, y, zLow, uHi, vHi);
		tessellator.addVertexWithUV(xHigh, y + 1.0D, zLow, uHi, vLo);
		tessellator.addVertexWithUV(xHigh, y + 1.0D, zLow, uLo, vLo);
		tessellator.addVertexWithUV(xHigh, y, zLow, uLo, vHi);
		tessellator.addVertexWithUV(xLow, y, zLow, uHi, vHi);
		tessellator.addVertexWithUV(xLow, y + 1.0D, zLow, uHi, vLo);
		tessellator.addVertexWithUV(xHigh, y + 1.0D, zHigh, uLo, vLo);
		tessellator.addVertexWithUV(xHigh, y, zHigh, uLo, vHi);
		tessellator.addVertexWithUV(xLow, y, zHigh, uHi, vHi);
		tessellator.addVertexWithUV(xLow, y + 1.0D, zHigh, uHi, vLo);
		tessellator.addVertexWithUV(xLow, y + 1.0D, zHigh, uLo, vLo);
		tessellator.addVertexWithUV(xLow, y, zHigh, uLo, vHi);
		tessellator.addVertexWithUV(xHigh, y, zHigh, uHi, vHi);
		tessellator.addVertexWithUV(xHigh, y + 1.0D, zHigh, uHi, vLo);
	}

	/** Returns how far a liquid block fills its cell: 1.0 for solids, level/9 for water. */
	private float materialNotWater(int x, int y, int z) {
		return this.blockAccess.getBlockMaterial(x, y, z) != Material.water ? 1.0F : (float)this.blockAccess.getBlockMetadata(x, y, z) / 9.0F;
	}

	private void renderBlockBottom(Block block, double x, double y, double z, int side) {
		Tessellator tessellator = Tessellator.instance;
		if(this.overrideBlockTexture >= 0) {
			side = this.overrideBlockTexture;
		}

		int uTile = (side & 15) << 4;
		side &= 240;
		double uLo = (double)((float)uTile / 256.0F);
		double uHi = (double)(((float)uTile + 15.99F) / 256.0F);
		double vLo = (double)((float)side / 256.0F);
		double vHi = (double)(((float)side + 15.99F) / 256.0F);
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

	private void renderBlockTop(Block block, double x, double y, double z, int side) {
		Tessellator tessellator = Tessellator.instance;
		if(this.overrideBlockTexture >= 0) {
			side = this.overrideBlockTexture;
		}

		int uTile = (side & 15) << 4;
		side &= 240;
		double uLo = (double)((float)uTile / 256.0F);
		double uHi = (double)(((float)uTile + 15.99F) / 256.0F);
		double vLo = (double)((float)side / 256.0F);
		double vHi = (double)(((float)side + 15.99F) / 256.0F);
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

	private void renderBlockNorth(Block block, double x, double y, double z, int side) {
		Tessellator tessellator = Tessellator.instance;
		if(this.overrideBlockTexture >= 0) {
			side = this.overrideBlockTexture;
		}

		int uTile = (side & 15) << 4;
		side &= 240;
		double uLo = (double)((float)uTile / 256.0F);
		double uHi = ((double)uTile + 15.99D) / 256.0D;
		double vTop;
		double vBottom;
		// Non-full blocks (snow layers, water) stretch the tile over their height.
		if(block.minY >= 0.0D && block.maxY <= 1.0D) {
			vTop = ((double)side + block.minY * (double)15.99F) / 256.0D;
			vBottom = ((double)side + block.maxY * (double)15.99F) / 256.0D;
		} else {
			vTop = (double)((float)side / 256.0F);
			vBottom = (double)(((float)side + 15.99F) / 256.0F);
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

	private void renderBlockSouth(Block block, double x, double y, double z, int side) {
		Tessellator tessellator = Tessellator.instance;
		if(this.overrideBlockTexture >= 0) {
			side = this.overrideBlockTexture;
		}

		int uTile = (side & 15) << 4;
		side &= 240;
		double uLo = (double)((float)uTile / 256.0F);
		double uHi = (double)(((float)uTile + 15.99F) / 256.0F);
		double vTop;
		double vBottom;
		if(block.minY >= 0.0D && block.maxY <= 1.0D) {
			vTop = ((double)side + block.minY * (double)15.99F) / 256.0D;
			vBottom = ((double)side + block.maxY * (double)15.99F) / 256.0D;
		} else {
			vTop = (double)((float)side / 256.0F);
			vBottom = (double)(((float)side + 15.99F) / 256.0F);
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

	private void renderBlockWest(Block block, double x, double y, double z, int side) {
		Tessellator tessellator = Tessellator.instance;
		if(this.overrideBlockTexture >= 0) {
			side = this.overrideBlockTexture;
		}

		int uTile = (side & 15) << 4;
		side &= 240;
		double uLo = (double)((float)uTile / 256.0F);
		double uHi = (double)(((float)uTile + 15.99F) / 256.0F);
		double vTop;
		double vBottom;
		if(block.minY >= 0.0D && block.maxY <= 1.0D) {
			vTop = ((double)side + block.minY * (double)15.99F) / 256.0D;
			vBottom = ((double)side + block.maxY * (double)15.99F) / 256.0D;
		} else {
			vTop = (double)((float)side / 256.0F);
			vBottom = (double)(((float)side + 15.99F) / 256.0F);
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

	private void renderBlockEast(Block block, double x, double y, double z, int side) {
		Tessellator tessellator = Tessellator.instance;
		if(this.overrideBlockTexture >= 0) {
			side = this.overrideBlockTexture;
		}

		int uTile = (side & 15) << 4;
		side &= 240;
		double uLo = (double)((float)uTile / 256.0F);
		double uHi = (double)(((float)uTile + 15.99F) / 256.0F);
		double vTop;
		double vBottom;
		if(block.minY >= 0.0D && block.maxY <= 1.0D) {
			vTop = ((double)side + block.minY * (double)15.99F) / 256.0D;
			vBottom = ((double)side + block.maxY * (double)15.99F) / 256.0D;
		} else {
			vTop = (double)((float)side / 256.0F);
			vBottom = (double)(((float)side + 15.99F) / 256.0F);
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

	/** Draws a single block preview inside an inventory slot. */
	public final void renderBlockOnInventory(Block block) {
		Tessellator tessellator = Tessellator.instance;
		int renderType = block.getRenderType();
		if(renderType == 0) {
			GL11.glTranslatef(-0.5F, -0.5F, -0.5F);
			tessellator.startDrawingQuads();
			Tessellator.setNormal(0.0F, -1.0F, 0.0F);
			this.renderBlockBottom(block, 0.0D, 0.0D, 0.0D, block.getBlockTextureFromSide(0));
			tessellator.draw();
			tessellator.startDrawingQuads();
			Tessellator.setNormal(0.0F, 1.0F, 0.0F);
			this.renderBlockTop(block, 0.0D, 0.0D, 0.0D, block.getBlockTextureFromSide(1));
			tessellator.draw();
			tessellator.startDrawingQuads();
			Tessellator.setNormal(0.0F, 0.0F, -1.0F);
			this.renderBlockNorth(block, 0.0D, 0.0D, 0.0D, block.getBlockTextureFromSide(2));
			tessellator.draw();
			tessellator.startDrawingQuads();
			Tessellator.setNormal(0.0F, 0.0F, 1.0F);
			this.renderBlockSouth(block, 0.0D, 0.0D, 0.0D, block.getBlockTextureFromSide(3));
			tessellator.draw();
			tessellator.startDrawingQuads();
			Tessellator.setNormal(-1.0F, 0.0F, 0.0F);
			this.renderBlockWest(block, 0.0D, 0.0D, 0.0D, block.getBlockTextureFromSide(4));
			tessellator.draw();
			tessellator.startDrawingQuads();
			Tessellator.setNormal(1.0F, 0.0F, 0.0F);
			this.renderBlockEast(block, 0.0D, 0.0D, 0.0D, block.getBlockTextureFromSide(5));
			tessellator.draw();
			GL11.glTranslatef(0.5F, 0.5F, 0.5F);
		} else if(renderType == 1) {
			tessellator.startDrawingQuads();
			Tessellator.setNormal(0.0F, -1.0F, 0.0F);
			this.renderBlockPlant(block, -1, -0.5D, -0.5D, -0.5D);
			tessellator.draw();
		} else if(renderType == 6) {
			tessellator.startDrawingQuads();
			Tessellator.setNormal(0.0F, -1.0F, 0.0F);
			this.renderBlockCrops(block, -1, -0.5D, -0.5D, -0.5D);
			tessellator.draw();
		} else {
			if(renderType == 2) {
				tessellator.startDrawingQuads();
				Tessellator.setNormal(0.0F, -1.0F, 0.0F);
				this.renderBlockTorch(block, -0.5D, -0.5D, -0.5D, 0.0D, 0.0D);
				tessellator.draw();
			}

		}
	}
}