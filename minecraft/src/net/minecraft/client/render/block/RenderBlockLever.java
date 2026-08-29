package net.minecraft.client.render.block;

import net.minecraft.client.render.RenderBlocks;
import net.minecraft.client.render.Tessellator;
import net.minecraft.game.physics.Vec3D;
import net.minecraft.game.world.IBlockAccess;
import net.minecraft.game.world.block.Block;
import util.AtlasTexel;
import util.AtlasUV;
import util.MathHelper;
import util.TextureAtlas;

/**
 * Render type 12 — lever, ported from the Alpha 1.1.2 renderer. A cobblestone
 * base plate is drawn as the standard cube with the lever-shaped bounds, then
 * the handle is an eight-cornered box rotated by hand: the metadata bit 8
 * flips the pulled/pushed state, walls (metadata &gt; 5) tilt the handle about
 * X, floors about Y. Cell-local vectors are used because this version's
 * {@link Vec3D} has no rotation helpers, so the two rotation passes are written
 * out here with the A1.1.2 formulas.
 */
public final class RenderBlockLever implements BlockRenderHandler {
	@Override
	public final boolean renderBlock(RenderBlocks renderBlocks, Block block, int x, int y, int z) {
		IBlockAccess blockAccess = renderBlocks.blockAccess;
		Tessellator tessellator = Tessellator.instance;
		int metadata = blockAccess.getBlockMetadata(x, y, z);
		int facing = metadata & 7;
		boolean active = (metadata & 8) > 0;
		boolean alreadyOverridden = renderBlocks.overrideBlockTexture >= 0;
		if(!alreadyOverridden) {
			renderBlocks.overrideBlockTexture = Block.cobblestone.blockIndexInTexture;
		}

		float halfHandle = 0.25F;
		float halfPlate = 3.0F / 16.0F;
		float plateThickness = 3.0F / 16.0F;
		switch(facing) {
		case 1:
			RenderBlockUtil.setBounds(block, 0.0F, 0.5F - halfHandle, 0.5F - halfPlate, plateThickness, 0.5F + halfHandle, 0.5F + halfPlate);
			break;
		case 2:
			RenderBlockUtil.setBounds(block, 1.0F - plateThickness, 0.5F - halfHandle, 0.5F - halfPlate, 1.0F, 0.5F + halfHandle, 0.5F + halfPlate);
			break;
		case 3:
			RenderBlockUtil.setBounds(block, 0.5F - halfPlate, 0.5F - halfHandle, 0.0F, 0.5F + halfPlate, 0.5F + halfHandle, plateThickness);
			break;
		case 4:
			RenderBlockUtil.setBounds(block, 0.5F - halfPlate, 0.5F - halfHandle, 1.0F - plateThickness, 0.5F + halfPlate, 0.5F + halfHandle, 1.0F);
			break;
		case 5:
			RenderBlockUtil.setBounds(block, 0.5F - halfPlate, 0.0F, 0.5F - halfHandle, 0.5F + halfPlate, plateThickness, 0.5F + halfHandle);
			break;
		case 6:
			RenderBlockUtil.setBounds(block, 0.5F - halfHandle, 0.0F, 0.5F - halfPlate, 0.5F + halfHandle, plateThickness, 0.5F + halfPlate);
			break;
		}

		RenderBlockUtil.renderStandardBlock(renderBlocks, block, x, y, z);
		if(!alreadyOverridden) {
			renderBlocks.overrideBlockTexture = -1;
		}

		float brightness = block.getBlockBrightness(blockAccess, x, y, z);
		if(Block.lightValue[block.blockID] > 0) {
			brightness = 1.0F;
		}

		tessellator.setColorOpaque_F(brightness, brightness, brightness);
		int textureId = block.getBlockTextureFromSide(0);
		if(renderBlocks.overrideBlockTexture >= 0) {
			textureId = renderBlocks.overrideBlockTexture;
		}

		AtlasUV.calc(textureId, TextureAtlas.TERRAIN);
		float uLo = (float)AtlasUV.u1;
		float uHi = (float)AtlasUV.u2;
		float vLo = (float)AtlasUV.v1;
		float vHi = (float)AtlasUV.v2;
		Vec3D[] corners = new Vec3D[8];
		float quarter = 1.0F / 16.0F;
		float armLength = 10.0F / 16.0F;
		corners[0] = new Vec3D((double)(-quarter), 0.0D, (double)(-quarter));
		corners[1] = new Vec3D((double)quarter, 0.0D, (double)(-quarter));
		corners[2] = new Vec3D((double)quarter, 0.0D, (double)quarter);
		corners[3] = new Vec3D((double)(-quarter), 0.0D, (double)quarter);
		corners[4] = new Vec3D((double)(-quarter), (double)armLength, (double)(-quarter));
		corners[5] = new Vec3D((double)quarter, (double)armLength, (double)(-quarter));
		corners[6] = new Vec3D((double)quarter, (double)armLength, (double)quarter);
		corners[7] = new Vec3D((double)(-quarter), (double)armLength, (double)quarter);
		for(int i = 0; i < 8; ++i) {
			if(active) {
				corners[i].zCoord -= 1.0D / 16.0D;
				rotateAroundX(corners[i], (float)Math.PI * 2.0F / 9.0F);
			} else {
				corners[i].zCoord += 1.0D / 16.0D;
				rotateAroundX(corners[i], -((float)Math.PI * 2.0F / 9.0F));
			}

			if(facing == 6) {
				rotateAroundY(corners[i], (float)Math.PI * 0.5F);
			}

			if(facing < 5) {
				corners[i].yCoord -= 0.375D;
				rotateAroundX(corners[i], (float)Math.PI * 0.5F);
				if(facing == 4) {
					rotateAroundY(corners[i], 0.0F);
				}

				if(facing == 3) {
					rotateAroundY(corners[i], (float)Math.PI);
				}

				if(facing == 2) {
					rotateAroundY(corners[i], (float)Math.PI * 0.5F);
				}

				if(facing == 1) {
					rotateAroundY(corners[i], (float)Math.PI * -0.5F);
				}
				corners[i].xCoord += (double)x + 0.5D;
				corners[i].yCoord += (double)((float)y + 0.5F);
				corners[i].zCoord += (double)z + 0.5D;
			} else {
				corners[i].xCoord += (double)x + 0.5D;
				corners[i].yCoord += (double)((float)y + 2.0F / 16.0F);
				corners[i].zCoord += (double)z + 0.5D;
			}
		}

		Vec3D v0 = null;
		Vec3D v1 = null;
		Vec3D v2 = null;
		Vec3D v3 = null;
		for(int face = 0; face < 6; ++face) {
			if(face == 0) {
				// Handle base: a 2x2 pixel patch of the tile.
				AtlasUV.calcPixels(AtlasTexel.u + 7, AtlasTexel.v + 6, 1.99F, 1.99F, TextureAtlas.TERRAIN);
				uLo = (float)AtlasUV.u1;
				uHi = (float)AtlasUV.u2;
				vLo = (float)AtlasUV.v1;
				vHi = (float)AtlasUV.v2;
			} else if(face == 2) {
				// Handle face: a 2-pixel-wide strip spanning most of the tile.
				AtlasUV.calcPixels(AtlasTexel.u + 7, AtlasTexel.v + 6, 1.99F, 9.99F, TextureAtlas.TERRAIN);
				uLo = (float)AtlasUV.u1;
				uHi = (float)AtlasUV.u2;
				vLo = (float)AtlasUV.v1;
				vHi = (float)AtlasUV.v2;
			}

			if(face == 0) {
				v0 = corners[0];
				v1 = corners[1];
				v2 = corners[2];
				v3 = corners[3];
			} else if(face == 1) {
				v0 = corners[7];
				v1 = corners[6];
				v2 = corners[5];
				v3 = corners[4];
			} else if(face == 2) {
				v0 = corners[1];
				v1 = corners[0];
				v2 = corners[4];
				v3 = corners[5];
			} else if(face == 3) {
				v0 = corners[2];
				v1 = corners[1];
				v2 = corners[5];
				v3 = corners[6];
			} else if(face == 4) {
				v0 = corners[3];
				v1 = corners[2];
				v2 = corners[6];
				v3 = corners[7];
			} else if(face == 5) {
				v0 = corners[0];
				v1 = corners[3];
				v2 = corners[7];
				v3 = corners[4];
			}

			tessellator.addVertexWithUV(v0.xCoord, v0.yCoord, v0.zCoord, (double)uLo, (double)vHi);
			tessellator.addVertexWithUV(v1.xCoord, v1.yCoord, v1.zCoord, (double)uHi, (double)vHi);
			tessellator.addVertexWithUV(v2.xCoord, v2.yCoord, v2.zCoord, (double)uHi, (double)vLo);
			tessellator.addVertexWithUV(v3.xCoord, v3.yCoord, v3.zCoord, (double)uLo, (double)vLo);
		}

		return true;
	}

	/** Tilts {@code v} about the X axis by {@code rot}, A1.1.2-style. */
	private static void rotateAroundX(Vec3D v, float rot) {
		float cos = MathHelper.cos(rot);
		float sin = MathHelper.sin(rot);
		double oldY = v.yCoord;
		v.yCoord = v.yCoord * (double)cos + v.zCoord * (double)sin;
		v.zCoord = v.zCoord * (double)cos - oldY * (double)sin;
	}

	/** Turns {@code v} about the Y axis by {@code rot}, A1.1.2-style. */
	private static void rotateAroundY(Vec3D v, float rot) {
		float cos = MathHelper.cos(rot);
		float sin = MathHelper.sin(rot);
		double oldX = v.xCoord;
		v.xCoord = v.xCoord * (double)cos + v.zCoord * (double)sin;
		v.zCoord = v.zCoord * (double)cos - oldX * (double)sin;
	}
}