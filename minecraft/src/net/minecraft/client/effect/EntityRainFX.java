package net.minecraft.client.effect;

import net.minecraft.client.render.Tessellator;
import net.minecraft.game.world.World;
import net.minecraft.game.world.material.Material;
import util.MathHelper;

// A single falling rain droplet. It accelerates under light gravity until it
// lands or its short age runs out, and vanishes inside liquids/solid blocks.
public class EntityRainFX extends EntityFX {
	public EntityRainFX(World world, double x, double y, double z) {
		super(world, x, y, z, 0.0D, 0.0D, 0.0D);
		this.motionX *= (double)0.3F;
		this.motionY = (double)((float)Math.random() * 0.2F + 0.1F);
		this.motionZ *= (double)0.3F;
		this.particleRed = 1.0F;
		this.particleGreen = 1.0F;
		this.particleBlue = 1.0F;
		this.particleTextureIndex = 16;
		this.setSize(0.01F, 0.01F);
		this.particleGravity = 0.06F;
		this.particleMaxAge = (int)(8.0D / (Math.random() * 0.8D + 0.2D));
	}

	public final void renderParticle(Tessellator tessellator, float partialTick, float offsetX, float offsetY, float offsetZ, float surfU, float surfV) {
		super.renderParticle(tessellator, partialTick, offsetX, offsetY, offsetZ, surfU, surfV);
	}

	public final void onUpdate() {
		this.prevPosX = this.posX;
		this.prevPosY = this.posY;
		this.prevPosZ = this.posZ;
		// Apply gravity; the droplet speeds up as it falls.
		this.motionY -= (double)this.particleGravity;
		this.moveEntity(this.motionX, this.motionY, this.motionZ);
		this.motionX *= (double)0.98F;
		this.motionY *= (double)0.98F;
		this.motionZ *= (double)0.98F;
		if(this.particleMaxAge-- <= 0) {
			super.isDead = true;
		}

		// On the ground, half the droplets die immediately (a splash) and any
		// survivors slow down; the rest keep skidding.
		if(this.onGround) {
			if(Math.random() < 0.5D) {
				super.isDead = true;
			}

			this.motionX *= (double)0.7F;
			this.motionZ *= (double)0.7F;
		}

		// A droplet entering a liquid or a solid block (rain does not pass
		// through roofs forever) is removed.
		Material material = this.worldObj.getBlockMaterial(MathHelper.floor_double(this.posX), MathHelper.floor_double(this.posY), MathHelper.floor_double(this.posZ));
		if(material.getIsLiquid() || material.isSolid()) {
			super.isDead = true;
		}

	}
}
