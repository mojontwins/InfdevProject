package net.minecraft.client.effect;

import net.minecraft.client.render.Tessellator;
import net.minecraft.game.world.World;

// A molten lava ember that rises off lava flows, shrinks over its life, and
// sheds a smoke particle behind it more often as it matures.
public final class EntityLavaFX extends EntityFX {
	private float lavaParticleScale;

	public EntityLavaFX(World world, double x, double y, double z) {
		super(world, x, y, z, 0.0D, 0.0D, 0.0D);
		this.motionX *= (double)0.8F;
		this.motionY *= (double)0.8F;
		this.motionZ *= (double)0.8F;
		this.motionY = (double)(this.rand.nextFloat() * 0.4F + 0.05F);
		this.particleRed = this.particleGreen = this.particleBlue = 1.0F;
		this.particleScale *= this.rand.nextFloat() * 2.0F + 0.2F;
		this.lavaParticleScale = this.particleScale;
		this.particleMaxAge = (int)(16.0D / (Math.random() * 0.8D + 0.2D));
		this.noClip = false;
		this.particleTextureIndex = 49;
	}

	public final float getEntityBrightness(float partialTick) {
		// Glowing embers are always rendered at full brightness.
		return 1.0F;
	}

	public final void renderParticle(Tessellator tessellator, float partialTick, float offsetX, float offsetY, float offsetZ, float surfU, float surfV) {
		// Life ratio from 0 (born) to 1 (about to die).
		float lifeRatio = ((float)this.particleAge + partialTick) / (float)this.particleMaxAge;
		// The ember shrinks down to nothing as it ages (quadratic fade-out).
		this.particleScale = this.lavaParticleScale * (1.0F - lifeRatio * lifeRatio);
		super.renderParticle(tessellator, partialTick, offsetX, offsetY, offsetZ, surfU, surfV);
	}

	public final void onUpdate() {
		this.prevPosX = this.posX;
		this.prevPosY = this.posY;
		this.prevPosZ = this.posZ;
		if(this.particleAge++ >= this.particleMaxAge) {
			super.isDead = true;
		}

		// Life ratio; as it approaches 1 the ember spawns smoke more frequently,
		// modelling cooling lava (nextFloat() must beat the ratio to emit).
		float lifeRatio = (float)this.particleAge / (float)this.particleMaxAge;
		if(this.rand.nextFloat() > lifeRatio) {
			this.worldObj.spawnParticle("smoke", this.posX, this.posY, this.posZ, this.motionX, this.motionY, this.motionZ);
		}

		this.motionY -= 0.03D;
		this.moveEntity(this.motionX, this.motionY, this.motionZ);
		this.motionX *= (double)0.999F;
		this.motionY *= (double)0.999F;
		this.motionZ *= (double)0.999F;
		if(this.onGround) {
			this.motionX *= (double)0.7F;
			this.motionZ *= (double)0.7F;
		}

	}
}
