package net.minecraft.client.effect;

import net.minecraft.game.world.World;
import net.minecraft.game.world.material.Material;
import util.MathHelper;

// A small air bubble that rises through water until it reaches the surface
// and pops. Dies if it is ever no longer inside a water block.
public final class EntityBubbleFX extends EntityFX {
	public EntityBubbleFX(World world, double x, double y, double z, double speedX, double speedY, double speedZ) {
		super(world, x, y, z, speedX, speedY, speedZ);
		this.particleRed = 1.0F;
		this.particleGreen = 1.0F;
		this.particleBlue = 1.0F;
		this.particleTextureIndex = 32;
		this.setSize(0.02F, 0.02F);
		this.particleScale *= this.rand.nextFloat() * 0.6F + 0.2F;
		this.motionX = speedX * (double)0.2F + (double)((float)(Math.random() * 2.0D - 1.0D) * 0.02F);
		this.motionY = speedY * (double)0.2F + (double)((float)(Math.random() * 2.0D - 1.0D) * 0.02F);
		this.motionZ = speedZ * (double)0.2F + (double)((float)(Math.random() * 2.0D - 1.0D) * 0.02F);
		this.particleMaxAge = (int)(8.0D / (Math.random() * 0.8D + 0.2D));
	}

	// Unlike normal particles, a bubble exerts gentle upward thrust (negative
	// gravity) and drifts, then pops when it escapes the water.
	public final void onUpdate() {
		this.prevPosX = this.posX;
		this.prevPosY = this.posY;
		this.prevPosZ = this.posZ;
		this.motionY += 0.002D;
		this.moveEntity(this.motionX, this.motionY, this.motionZ);
		this.motionX *= (double)0.85F;
		this.motionY *= (double)0.85F;
		this.motionZ *= (double)0.85F;
		if(this.worldObj.getBlockMaterial(MathHelper.floor_double(this.posX), MathHelper.floor_double(this.posY), MathHelper.floor_double(this.posZ)) != Material.water) {
			super.isDead = true;
		}

		if(this.particleMaxAge-- <= 0) {
			super.isDead = true;
		}

	}
}
