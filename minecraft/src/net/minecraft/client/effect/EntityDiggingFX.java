package net.minecraft.client.effect;

import net.minecraft.client.render.Tessellator;
import net.minecraft.game.world.World;
import net.minecraft.game.world.block.Block;
import util.AtlasUV;
import util.TextureAtlas;

public final class EntityDiggingFX extends EntityFX {
	public EntityDiggingFX(World var1, double var2, double var4, double var6, double var8, double var10, double var12, Block var14) {
		super(var1, var2, var4, var6, var8, var10, var12);
		this.particleTextureIndex = var14.blockIndexInTexture;
		this.particleGravity = var14.blockParticleGravity;
		this.particleRed = this.particleGreen = this.particleBlue = 0.6F;
		this.particleScale /= 2.0F;
	}

	public final int getFXLayer() {
		return 1;
	}

	public final void renderParticle(Tessellator var1, float var2, float var3, float var4, float var5, float var6, float var7) {
		TextureAtlas terrain = TextureAtlas.TERRAIN;
		AtlasUV.calc(this.particleTextureIndex, terrain);
		// The jitter nudges the crack sprite by a few texels within its tile.
		float jitterU = this.particleTextureJitterX * 4.0F / terrain.width;
		float jitterV = this.particleTextureJitterY * 4.0F / terrain.height;
		float var8 = (float)AtlasUV.u1 + jitterU;
		float var9 = (float)AtlasUV.u2 + jitterU;
		float var10 = (float)AtlasUV.v1 + jitterV;
		float var11 = (float)AtlasUV.v2 + jitterV;
		float var12 = 0.1F * this.particleScale;
		float var13 = (float)(this.prevPosX + (this.posX - this.prevPosX) * (double)var2 - interpPosX);
		float var14 = (float)(this.prevPosY + (this.posY - this.prevPosY) * (double)var2 - interpPosY);
		float var15 = (float)(this.prevPosZ + (this.posZ - this.prevPosZ) * (double)var2 - interpPosZ);
		var2 = this.getEntityBrightness(var2);
		var1.setColorOpaque_F(var2 * this.particleRed, var2 * this.particleGreen, var2 * this.particleBlue);
		var1.addVertexWithUV((double)(var13 - var3 * var12 - var6 * var12), (double)(var14 - var4 * var12), (double)(var15 - var5 * var12 - var7 * var12), (double)var8, (double)var11);
		var1.addVertexWithUV((double)(var13 - var3 * var12 + var6 * var12), (double)(var14 + var4 * var12), (double)(var15 - var5 * var12 + var7 * var12), (double)var8, (double)var10);
		var1.addVertexWithUV((double)(var13 + var3 * var12 + var6 * var12), (double)(var14 + var4 * var12), (double)(var15 + var5 * var12 + var7 * var12), (double)var9, (double)var10);
		var1.addVertexWithUV((double)(var13 + var3 * var12 - var6 * var12), (double)(var14 - var4 * var12), (double)(var15 + var5 * var12 - var7 * var12), (double)var9, (double)var11);
	}
}
