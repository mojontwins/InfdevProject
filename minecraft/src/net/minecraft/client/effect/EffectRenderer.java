package net.minecraft.client.effect;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.client.render.RenderEngine;
import net.minecraft.client.render.Tessellator;
import net.minecraft.game.entity.Entity;
import net.minecraft.game.world.World;
import net.minecraft.game.world.block.Block;
import org.lwjgl.opengl.GL11;
import util.MathHelper;

public final class EffectRenderer {
	private World worldObj;
	@SuppressWarnings("unchecked")
	private List<EntityFX>[] fxLayers = (List<EntityFX>[])new List<?>[3];
	private RenderEngine renderer;
	private Random rand = new Random();

	// Owns all active particles. They are split across three render layers
	// (terrain-textured crack bits, particle-texture world particles, and
	// lit/pickup particles drawn without the world), each updated and drawn here.
	public EffectRenderer(World world, RenderEngine renderEngine) {
		if(world != null) {
			this.worldObj = world;
		}

		this.renderer = renderEngine;

		for(int layer = 0; layer < 3; ++layer) {
			this.fxLayers[layer] = new ArrayList<>();
		}

	}

	public final void addEffect(EntityFX fx) {
		int layer = fx.getFXLayer();
		this.fxLayers[layer].add(fx);
	}

	public final void updateEffects() {
		// Iterate with an index because dead particles are removed mid-loop.
		for(int layer = 0; layer < 3; ++layer) {
			for(int index = 0; index < this.fxLayers[layer].size(); ++index) {
				EntityFX fx = this.fxLayers[layer].get(index);
				fx.onUpdate();
				if(fx.isDead) {
					this.fxLayers[layer].remove(index--);
				}
			}
		}

	}

	public final void renderParticles(Entity entity, float partialTick) {
		// Build the camera's three basis axes from the player's rotation; these
		// orient each particle quad so it always faces the camera.
		float yawCos = MathHelper.cos(entity.rotationYaw * (float)Math.PI / 180.0F);
		float yawSin = MathHelper.sin(entity.rotationYaw * (float)Math.PI / 180.0F);
		float pitchYawCos = -yawSin * MathHelper.sin(entity.rotationPitch * (float)Math.PI / 180.0F);
		float pitchYawSin = yawCos * MathHelper.sin(entity.rotationPitch * (float)Math.PI / 180.0F);
		float pitchCos = MathHelper.cos(entity.rotationPitch * (float)Math.PI / 180.0F);
		EntityFX.interpPosX = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * (double)partialTick;
		EntityFX.interpPosY = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * (double)partialTick;
		EntityFX.interpPosZ = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * (double)partialTick;

		for(int layer = 0; layer < 2; ++layer) {
			if(this.fxLayers[layer].size() != 0) {
				int texture = 0;
				if(layer == 0) {
					texture = this.renderer.getTexture("/particles.png");
				}

				if(layer == 1) {
					texture = this.renderer.getTexture("/terrain.png");
				}

				GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
				Tessellator tessellator = Tessellator.instance;
				tessellator.startDrawingQuads();

				for(int index = 0; index < this.fxLayers[layer].size(); ++index) {
					EntityFX fx = this.fxLayers[layer].get(index);
					fx.renderParticle(tessellator, partialTick, yawCos, pitchCos, yawSin, pitchYawCos, pitchYawSin);
				}

				tessellator.draw();
			}
		}

	}

	public final void renderLitParticles(float partialTick) {
		if(this.fxLayers[2].size() != 0) {
			Tessellator tessellator = Tessellator.instance;

			for(int index = 0; index < this.fxLayers[2].size(); ++index) {
				EntityFX fx = this.fxLayers[2].get(index);
				fx.renderParticle(tessellator, partialTick, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
			}

		}
	}

	public final void clearEffects(World world) {
		this.worldObj = world;

		for(int layer = 0; layer < 3; ++layer) {
			this.fxLayers[layer].clear();
		}

	}

	// Spawns a burst of 64 crack particles evenly spaced through the block's
	// volume, each flying outward from the block's center.
	public final void addBlockDestroyEffects(int x, int y, int z) {
		int blockId = this.worldObj.getBlockId(x, y, z);
		if(blockId != 0) {
			Block block = Block.blocksList[blockId];

			for(int dx = 0; dx < 4; ++dx) {
				for(int dy = 0; dy < 4; ++dy) {
					for(int dz = 0; dz < 4; ++dz) {
						double posX = (double)x + ((double)dx + 0.5D) / 4.0D;
						double posY = (double)y + ((double)dy + 0.5D) / 4.0D;
						double posZ = (double)z + ((double)dz + 0.5D) / 4.0D;
						this.addEffect(new EntityDiggingFX(this.worldObj, posX, posY, posZ, posX - (double)x - 0.5D, posY - (double)y - 0.5D, posZ - (double)z - 0.5D, block));
					}
				}
			}

		}
	}

	// Spawns a single crack particle where the block face was hit; `side` selects
	// which face so the particle is placed just outside that surface.
	public final void addBlockHitEffects(int x, int y, int z, int side) {
		int blockId = this.worldObj.getBlockId(x, y, z);
		if(blockId != 0) {
			Block block = Block.blocksList[blockId];
			double posX = (double)x + this.rand.nextDouble() * (block.maxX - block.minX - (double)0.2F) + (double)0.1F + block.minX;
			double posY = (double)y + this.rand.nextDouble() * (block.maxY - block.minY - (double)0.2F) + (double)0.1F + block.minY;
			double posZ = (double)z + this.rand.nextDouble() * (block.maxZ - block.minZ - (double)0.2F) + (double)0.1F + block.minZ;
			if(side == 0) {
				posY = (double)y + block.minY - (double)0.1F;
			}

			if(side == 1) {
				posY = (double)y + block.maxY + (double)0.1F;
			}

			if(side == 2) {
				posZ = (double)z + block.minZ - (double)0.1F;
			}

			if(side == 3) {
				posZ = (double)z + block.maxZ + (double)0.1F;
			}

			if(side == 4) {
				posX = (double)x + block.minX - (double)0.1F;
			}

			if(side == 5) {
				posX = (double)x + block.maxX + (double)0.1F;
			}

			EntityDiggingFX fx = new EntityDiggingFX(this.worldObj, posX, posY, posZ, 0.0D, 0.0D, 0.0D, block);
			fx.motionX *= (double)0.2F;
			fx.motionY = (fx.motionY - (double)0.1F) * (double)0.2F + (double)0.1F;
			fx.motionZ *= (double)0.2F;
			this.addEffect(fx.multiplyParticleScaleBy(0.6F));
		}
	}

	public final String getStatistics() {
		return "" + (this.fxLayers[0].size() + this.fxLayers[1].size() + this.fxLayers[2].size());
	}
}
