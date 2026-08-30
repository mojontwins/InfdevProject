package net.minecraft.client.effect;

import net.minecraft.client.render.Tessellator;
import net.minecraft.game.world.World;
import net.minecraft.game.world.block.Block;
import util.AtlasUV;
import util.TexelScale;
import util.TextureAtlas;

public final class EntityDiggingFX extends EntityFX {
	public EntityDiggingFX(World world, double x, double y, double z, double speedX, double speedY, double speedZ, Block block) {
		super(world, x, y, z, speedX, speedY, speedZ);
		this.particleTextureIndex = block.blockIndexInTexture;
		this.particleGravity = block.blockParticleGravity;
		this.particleRed = this.particleGreen = this.particleBlue = 0.6F;
		this.particleScale /= 2.0F;
	}

	public final int getFXLayer() {
		return 1;
	}

	public final void renderParticle(Tessellator tessellator, float partialTick, float offsetX, float offsetY, float offsetZ, float textureJitterXMultiplier, float textureJitterYMultiplier) {
		TextureAtlas terrain = TextureAtlas.TERRAIN;
		AtlasUV.calc(this.particleTextureIndex, terrain);
		// The jitter nudges the crack sprite by a few texels within its tile.
		float textureJitterU = this.particleTextureJitterX * 4.0F;
		float textureJitterV = this.particleTextureJitterY * 4.0F;
		float u1TexelStart = (float)AtlasUV.u1 + TexelScale.u(terrain, textureJitterU);
		float u1TexelEnd = u1TexelStart + TexelScale.u(terrain, 4); 
		float v1TexelStart = (float)AtlasUV.v1 + TexelScale.v(terrain, textureJitterV);
		float v1TexelEnd = v1TexelStart + TexelScale.v(terrain, 4);
		
		float scaleSize = 0.1F * this.particleScale;
		float interpolatedX = (float)(this.prevPosX + (this.posX - this.prevPosX) * (double)partialTick - interpPosX);
		float interpolatedY = (float)(this.prevPosY + (this.posY - this.posY) * (double)partialTick - interpPosY);
		float interpolatedZ = (float)(this.prevPosZ + (this.posZ - this.prevPosZ) * (double)partialTick - interpPosZ);
		float brightness = this.getEntityBrightness(partialTick);
		tessellator.setColorOpaque_F(brightness * this.particleRed, brightness * this.particleGreen, brightness * this.particleBlue);
		tessellator.addVertexWithUV((double)(interpolatedX - offsetX * scaleSize - textureJitterU * scaleSize), (double)(interpolatedY - offsetY * scaleSize), (double)(interpolatedZ - offsetZ * scaleSize - v1TexelStart), (double)u1TexelStart, (double)v1TexelEnd);
		tessellator.addVertexWithUV((double)(interpolatedX - offsetX * scaleSize + textureJitterU * scaleSize), (double)(interpolatedY + offsetY * scaleSize), (double)(interpolatedZ - offsetZ * scaleSize + v1TexelStart), (double)u1TexelStart, (double)v1TexelEnd);
		tessellator.addVertexWithUV((double)(interpolatedX + offsetX * scaleSize + textureJitterU * scaleSize), (double)(interpolatedY + offsetY * scaleSize), (double)(interpolatedZ + offsetZ * scaleSize + v1TexelStart), (double)u1TexelEnd, (double)v1TexelEnd);
		tessellator.addVertexWithUV((double)(interpolatedX + offsetX * scaleSize - textureJitterU * scaleSize), (double)(interpolatedY - offsetY * scaleSize), (double)(interpolatedZ + offsetZ * scaleSize - v1TexelStart), (double)u1TexelEnd, (double)v1TexelStart);
	}
}
