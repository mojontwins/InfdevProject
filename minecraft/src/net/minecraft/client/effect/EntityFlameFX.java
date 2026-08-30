package net.minecraft.client.effect;

import net.minecraft.client.render.Tessellator;
import net.minecraft.game.world.World;

// A flame particle for torches/fire: it emits full light, burns with a
// flickering scale that shrinks near the end of its life, and rises slightly.
public final class EntityFlameFX extends EntityFX {
	private float flameScale;

	public EntityFlameFX(World world, double x, double y, double z) {
		super(world, x, y, z, 0.0D, 0.0D, 0.0D);
		this.motionX *= (double)0.01F;
		this.motionY *= (double)0.01F;
		this.motionZ *= (double)0.01F;
		this.rand.nextFloat();
		this.rand.nextFloat();
		this.rand.nextFloat();
		this.rand.nextFloat();
		this.rand.nextFloat();
		this.rand.nextFloat();
		this.flameScale = this.particleScale;
		this.particleRed = this.particleGreen = this.particleBlue = 1.0F;
		this.particleMaxAge = (int)(8.0D / (Math.random() * 0.8D + 0.2D)) + 4;
		this.noClip = true;
		this.particleTextureIndex = 48;
	}

	public final void renderParticle(Tessellator tessellator, float partialTick, float offsetX, float offsetY, float offsetZ, float surfU, float surfV) {
		// Life ratio from 0 (born) to 1 (about to die).
		float lifeRatio = ((float)this.particleAge + partialTick) / (float)this.particleMaxAge;
		// The flame progressively shrinks as it ages (up to half its size).
		this.particleScale = this.flameScale * (1.0F - lifeRatio * lifeRatio * 0.5F);
		super.renderParticle(tessellator, partialTick, offsetX, offsetY, offsetZ, surfU, surfV);
	}

	// A flame always glows at full brightness, fading toward the base world
	// brightness only near the very end of its life.
	public final float getEntityBrightness(float partialTick) {
		float lifeRatio = ((float)this.particleAge + partialTick) / (float)this.particleMaxAge;
		if(lifeRatio < 0.0F) {
			lifeRatio = 0.0F;
		}

		if(lifeRatio > 1.0F) {
			lifeRatio = 1.0F;
		}

		partialTick = super.getEntityBrightness(partialTick);
		return partialTick * lifeRatio + (1.0F - lifeRatio);
	}

	public final void onUpdate() {
		this.prevPosX = this.posX;
		this.prevPosY = this.posY;
		this.prevPosZ = this.posZ;
		if(this.particleAge++ >= this.particleMaxAge) {
			super.isDead = true;
		}

		this.moveEntity(this.motionX, this.motionY, this.motionZ);
		this.motionX *= (double)0.96F;
		this.motionY *= (double)0.96F;
		this.motionZ *= (double)0.96F;
		if(this.onGround) {
			this.motionX *= (double)0.7F;
			this.motionZ *= (double)0.7F;
		}

	}
}
