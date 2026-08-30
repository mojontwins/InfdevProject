package net.minecraft.client.effect;

import com.mojang.nbt.NBTTagCompound;
import net.minecraft.client.render.Tessellator;
import net.minecraft.game.entity.Entity;
import net.minecraft.game.world.World;
import util.AtlasUV;
import util.MathHelper;
import util.TextureAtlas;

public class EntityFX extends Entity {
	protected int particleTextureIndex;
	protected float particleTextureJitterX;
	protected float particleTextureJitterY;
	protected int particleAge = 0;
	protected int particleMaxAge = 0;
	protected float particleScale;
	protected float particleGravity;
	protected float particleRed;
	protected float particleGreen;
	protected float particleBlue;
	public static double interpPosX;
	public static double interpPosY;
	public static double interpPosZ;

	// Base particle class: every particle (bubble, smoke, flame, ...) shares the
	// fields and per-tick motion/age logic defined here, and is rendered as a
	// camera-facing quad from its texture tile.
	public EntityFX(World world, double x, double y, double z, double speedX, double speedY, double speedZ) {
		super(world);
		this.setSize(0.2F, 0.2F);
		this.yOffset = this.height / 2.0F;
		this.setPosition(x, y, z);
		this.particleRed = this.particleGreen = this.particleBlue = 1.0F;
		this.motionX = speedX + (double)((float)(Math.random() * 2.0D - 1.0D) * 0.4F);
		this.motionY = speedY + (double)((float)(Math.random() * 2.0D - 1.0D) * 0.4F);
		this.motionZ = speedZ + (double)((float)(Math.random() * 2.0D - 1.0D) * 0.4F);
		// Renormalize the (already jittered) velocity to a random speed between
		// 0.15 and 0.45, then nudge it upward slightly so particles rise at first.
		float speed = (float)(Math.random() + Math.random() + 1.0D) * 0.15F;
		float speedMagnitude = MathHelper.sqrt_double(this.motionX * this.motionX + this.motionY * this.motionY + this.motionZ * this.motionZ);
		this.motionX = this.motionX / (double)speedMagnitude * (double)speed * (double)0.4F;
		this.motionY = this.motionY / (double)speedMagnitude * (double)speed * (double)0.4F + (double)0.1F;
		this.motionZ = this.motionZ / (double)speedMagnitude * (double)speed * (double)0.4F;
		this.particleTextureJitterX = this.rand.nextFloat() * 3.0F;
		this.particleTextureJitterY = this.rand.nextFloat() * 3.0F;
		this.particleScale = (this.rand.nextFloat() * 0.5F + 0.5F) * 2.0F;
		this.particleMaxAge = (int)(4.0F / (this.rand.nextFloat() * 0.9F + 0.1F));
		this.particleAge = 0;
		this.entityWalks = false;
	}

	public final EntityFX multiplyParticleScaleBy(float scale) {
		this.setSize(0.120000005F, 0.120000005F);
		this.particleScale *= 0.6F;
		return this;
	}

	public void onUpdate() {
		this.prevPosX = this.posX;
		this.prevPosY = this.posY;
		this.prevPosZ = this.posZ;
		if(this.particleAge++ >= this.particleMaxAge) {
			super.isDead = true;
		}

		this.motionY -= 0.04D * (double)this.particleGravity;
		this.moveEntity(this.motionX, this.motionY, this.motionZ);
		this.motionX *= (double)0.98F;
		this.motionY *= (double)0.98F;
		this.motionZ *= (double)0.98F;
		if(this.onGround) {
			this.motionX *= (double)0.7F;
			this.motionZ *= (double)0.7F;
		}

	}

	public void renderParticle(Tessellator tessellator, float partialTick, float offsetX, float offsetY, float offsetZ, float surfU, float surfV) {
		AtlasUV.calc(this.particleTextureIndex, TextureAtlas.TERRAIN);
		float u1 = (float)AtlasUV.u1;
		float u2 = (float)AtlasUV.u2;
		float v1 = (float)AtlasUV.v1;
		float v2 = (float)AtlasUV.v2;
		// Half-extent of the quad, scaled by the particle's size.
		float scaleSize = 0.1F * this.particleScale;
		// Interpolate the particle position between its previous and current
		// position by partialTick, then shift into camera-relative space.
		float interpolatedX = (float)(this.prevPosX + (this.posX - this.prevPosX) * (double)partialTick - interpPosX);
		float interpolatedY = (float)(this.prevPosY + (this.posY - this.prevPosY) * (double)partialTick - interpPosY);
		float interpolatedZ = (float)(this.prevPosZ + (this.posZ - this.prevPosZ) * (double)partialTick - interpPosZ);
		partialTick = this.getEntityBrightness(partialTick);
		tessellator.setColorOpaque_F(this.particleRed * partialTick, this.particleGreen * partialTick, this.particleBlue * partialTick);
		tessellator.addVertexWithUV((double)(interpolatedX - offsetX * scaleSize - surfU * scaleSize), (double)(interpolatedY - offsetY * scaleSize), (double)(interpolatedZ - offsetZ * scaleSize - surfV * scaleSize), (double)u1, (double)v2);
		tessellator.addVertexWithUV((double)(interpolatedX - offsetX * scaleSize + surfU * scaleSize), (double)(interpolatedY + offsetY * scaleSize), (double)(interpolatedZ - offsetZ * scaleSize + surfV * scaleSize), (double)u1, (double)v1);
		tessellator.addVertexWithUV((double)(interpolatedX + offsetX * scaleSize + surfU * scaleSize), (double)(interpolatedY + offsetY * scaleSize), (double)(interpolatedZ + offsetZ * scaleSize + surfV * scaleSize), (double)u2, (double)v1);
		tessellator.addVertexWithUV((double)(interpolatedX + offsetX * scaleSize - surfU * scaleSize), (double)(interpolatedY - offsetY * scaleSize), (double)(interpolatedZ + offsetZ * scaleSize - surfV * scaleSize), (double)u2, (double)v2);
	}

	public int getFXLayer() {
		return 0;
	}

	public final void writeEntityToNBT(NBTTagCompound nbtTagCompound) {
	}

	public final void readEntityFromNBT(NBTTagCompound nbtTagCompound) {
	}
}
