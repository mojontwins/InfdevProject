package net.minecraft.client.effect;

import net.minecraft.client.render.Tessellator;
import net.minecraft.game.world.World;

// Debris particles thrown outward by an explosion; they fly out with random
// jittered velocity, slowly shrink onto a smaller texture tile, and grow dimmer.
public final class EntityExplodeFX extends EntityFX {
	public EntityExplodeFX(World world, double x, double y, double z, double speedX, double speedY, double speedZ) {
		super(world, x, y, z, speedX, speedY, speedZ);
		this.motionX = speedX + (double)((float)(Math.random() * 2.0D - 1.0D) * 0.05F);
		this.motionY = speedY + (double)((float)(Math.random() * 2.0D - 1.0D) * 0.05F);
		this.motionZ = speedZ + (double)((float)(Math.random() * 2.0D - 1.0D) * 0.05F);
		this.particleRed = this.particleGreen = this.particleBlue = this.rand.nextFloat() * 0.3F + 0.7F;
		this.particleScale = this.rand.nextFloat() * this.rand.nextFloat() * 6.0F + 1.0F;
		this.particleMaxAge = (int)(16.0D / ((double)this.rand.nextFloat() * 0.8D + 0.2D)) + 2;
	}

	public final void renderParticle(Tessellator tessellator, float partialTick, float offsetX, float offsetY, float offsetZ, float surfU, float surfV) {
		super.renderParticle(tessellator, partialTick, offsetX, offsetY, offsetZ, surfU, surfV);
	}

	public final void onUpdate() {
		this.prevPosX = this.posX;
		this.prevPosY = this.posY;
		this.prevPosZ = this.posZ;
		if(this.particleAge++ >= this.particleMaxAge) {
			super.isDead = true;
		}

		// Progressively flip to smaller texture tiles (tile 7 down to 0) as the
		// particle ages, so the debris visually shrinks over its lifetime.
		this.particleTextureIndex = 7 - (this.particleAge << 3) / this.particleMaxAge;
		this.motionY += 0.004D;
		this.moveEntity(this.motionX, this.motionY, this.motionZ);
		this.motionX *= (double)0.9F;
		this.motionY *= (double)0.9F;
		this.motionZ *= (double)0.9F;
		if(this.onGround) {
			this.motionX *= (double)0.7F;
			this.motionZ *= (double)0.7F;
		}

	}
}
