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

	public final void renderParticle(Tessellator tessellator, float partialTick, float offsetX, float offsetY, float offsetZ, float surfU, float surfV) {
		TextureAtlas terrain = TextureAtlas.TERRAIN;
		AtlasUV.calc(this.particleTextureIndex, terrain);
		// The jitter picks one crack sprite of the 4x4 sub-tile arrangement, and
		// only changes the UVs — never the quad's orientation.
		float textureJitterU = this.particleTextureJitterX * 4.0F;
		float textureJitterV = this.particleTextureJitterY * 4.0F;
		float u1 = (float)AtlasUV.u1 + TexelScale.u(terrain, textureJitterU);
		float u2 = u1 + TexelScale.u(terrain, 4);
		float v1 = (float)AtlasUV.v1 + TexelScale.v(terrain, textureJitterV);
		float v2 = v1 + TexelScale.v(terrain, 4);

		float scaleSize = 0.1F * this.particleScale;
		float interpolatedX = (float)(this.prevPosX + (this.posX - this.prevPosX) * (double)partialTick - interpPosX);
		float interpolatedY = (float)(this.prevPosY + (this.posY - this.prevPosY) * (double)partialTick - interpPosY);
		float interpolatedZ = (float)(this.prevPosZ + (this.posZ - this.prevPosZ) * (double)partialTick - interpPosZ);
		float brightness = this.getEntityBrightness(partialTick);
		tessellator.setColorOpaque_F(brightness * this.particleRed, brightness * this.particleGreen, brightness * this.particleBlue);
		tessellator.addVertexWithUV((double)(interpolatedX - offsetX * scaleSize - surfU * scaleSize), (double)(interpolatedY - offsetY * scaleSize), (double)(interpolatedZ - offsetZ * scaleSize - surfV * scaleSize), (double)u1, (double)v2);
		tessellator.addVertexWithUV((double)(interpolatedX - offsetX * scaleSize + surfU * scaleSize), (double)(interpolatedY + offsetY * scaleSize), (double)(interpolatedZ - offsetZ * scaleSize + surfV * scaleSize), (double)u1, (double)v1);
		tessellator.addVertexWithUV((double)(interpolatedX + offsetX * scaleSize + surfU * scaleSize), (double)(interpolatedY + offsetY * scaleSize), (double)(interpolatedZ + offsetZ * scaleSize + surfV * scaleSize), (double)u2, (double)v1);
		tessellator.addVertexWithUV((double)(interpolatedX + offsetX * scaleSize - surfU * scaleSize), (double)(interpolatedY - offsetY * scaleSize), (double)(interpolatedZ + offsetZ * scaleSize - surfV * scaleSize), (double)u2, (double)v2);
	}
}
