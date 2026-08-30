package net.minecraft.client.effect;

import net.minecraft.client.render.Tessellator;
import net.minecraft.game.world.World;

// Smoke that drifts upward, expands many-fold over its life, fades out, and
// picks a smaller texture tile as it ages. The optional scale argument lets
// callers (e.g. erupting lava) control its start size and lifespan.
public final class EntitySmokeFX extends EntityFX {
	private float smokeParticleScale;

	public EntitySmokeFX(World world, double x, double y, double z) {
		this(world, x, y, z, 1.0F);
	}

	public EntitySmokeFX(World world, double x, double y, double z, float scale) {
		super(world, x, y, z, 0.0D, 0.0D, 0.0D);
		this.motionX *= (double)0.1F;
		this.motionY *= (double)0.1F;
		this.motionZ *= (double)0.1F;
		this.particleRed = this.particleGreen = this.particleBlue = (float)(Math.random() * (double)0.3F);
		// Fit to a 12x12 tile inside the 16x16 smoke sprite, then apply caller scale.
		this.particleScale *= 12.0F / 16.0F;
		this.particleScale *= scale;
		this.smokeParticleScale = this.particleScale;
		this.particleMaxAge = (int)(8.0D / (Math.random() * 0.8D + 0.2D));
		// Scale both the size and the lifespan by the requested scale factor.
		this.particleMaxAge = (int)((float)this.particleMaxAge * scale);
		this.noClip = false;
	}

	public final void renderParticle(Tessellator tessellator, float partialTick, float offsetX, float offsetY, float offsetZ, float surfU, float surfV) {
		// Life ratio over 32 frames, clamped to [0,1]: smoke grows from zero up
		// to its full starting size as it drifts.
		float lifeRatio = ((float)this.particleAge + partialTick) / (float)this.particleMaxAge * 32.0F;
		if(lifeRatio < 0.0F) {
			lifeRatio = 0.0F;
		}

		if(lifeRatio > 1.0F) {
			lifeRatio = 1.0F;
		}

		this.particleScale = this.smokeParticleScale * lifeRatio;
		super.renderParticle(tessellator, partialTick, offsetX, offsetY, offsetZ, surfU, surfV);
	}

	public final void onUpdate() {
		this.prevPosX = this.posX;
		this.prevPosY = this.posY;
		this.prevPosZ = this.posZ;
		if(this.particleAge++ >= this.particleMaxAge) {
			super.isDead = true;
		}

		this.particleTextureIndex = 7 - (this.particleAge << 3) / this.particleMaxAge;
		this.motionY += 0.004D;
		this.moveEntity(this.motionX, this.motionY, this.motionZ);
		if(this.posY == this.prevPosY) {
			this.motionX *= 1.1D;
			this.motionZ *= 1.1D;
		}

		this.motionX *= (double)0.96F;
		this.motionY *= (double)0.96F;
		this.motionZ *= (double)0.96F;
		if(this.onGround) {
			this.motionX *= (double)0.7F;
			this.motionZ *= (double)0.7F;
		}

	}
}
