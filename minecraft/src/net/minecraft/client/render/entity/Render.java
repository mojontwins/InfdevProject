package net.minecraft.client.render.entity;

import net.minecraft.client.render.RenderEngine;
import net.minecraft.client.render.Tessellator;
import net.minecraft.game.entity.Entity;
import net.minecraft.game.world.World;
import net.minecraft.game.world.block.Block;
import org.lwjgl.opengl.GL11;
import util.AtlasUV;
import util.MathHelper;
import util.TextureAtlas;

public abstract class Render {
	protected RenderManager renderManager;
	protected float shadowSize;
	protected float shadowOpaque;

	public Render() {
		this.shadowSize = 0.0F;
		this.shadowOpaque = 1.0F;
	}

	public abstract void doRender(Entity entity, double x, double y, double z, float yaw, float partialTick);

	protected final void loadTexture(String textureName) {
		RenderEngine renderEngine = this.renderManager.renderEngine;
		RenderEngine.bindTexture(renderEngine.getTexture(textureName));
	}

	protected final void loadDownloadableImageTexture(String imageName, String fallbackName) {
		RenderEngine renderEngine = this.renderManager.renderEngine;
		RenderEngine.bindTexture(renderEngine.getTextureForDownloadableImage(imageName, fallbackName));
	}

	public final void setRenderManager(RenderManager renderManager) {
		this.renderManager = renderManager;
	}

	public final void renderShadow(Entity entity, double x, double y, double z, float partialTick) {
		if(this.shadowSize > 0.0F) {
			double distance = this.renderManager.getDistanceToCamera(x, y, z);
			partialTick = (float)((1.0D - distance / 256.0D) * (double)this.shadowOpaque);
			if(partialTick > 0.0F) {
				float shadowAlpha = partialTick;
				World world = this.renderManager.worldObj;
				GL11.glEnable(GL11.GL_BLEND);
				RenderEngine renderEngine = this.renderManager.renderEngine;
				RenderEngine.bindTexture(renderEngine.getTexture("%%/shadow.png"));
				GL11.glDepthMask(false);
				float shadowRadius = this.shadowSize;
				int x0 = MathHelper.floor_double(x - (double)shadowRadius);
				int x1 = MathHelper.floor_double(x + (double)shadowRadius);
				int y0 = MathHelper.floor_double(y - (double)shadowRadius);
				int y1 = MathHelper.floor_double(y);
				int z0 = MathHelper.floor_double(z - (double)shadowRadius);
				int z1 = MathHelper.floor_double(z + (double)shadowRadius);

				for(int blockX = x0; blockX <= x1; ++blockX) {
					for(int blockY = y0; blockY <= y1; ++blockY) {
						for(int blockZ = z0; blockZ <= z1; ++blockZ) {
							int blockId = world.getBlockId(blockX, blockY - 1, blockZ);
							if(blockId > 0 && world.getBlockLightValue(blockX, blockY, blockZ) > 3) {
								Block block = Block.blocksList[blockId];
								Tessellator tessellator = Tessellator.instance;
								double fade = ((double)shadowAlpha - (y - (double)blockY) / 2.0D) * 0.5D * (double)this.renderManager.worldObj.getBrightness(blockX, blockY, blockZ);
								if(fade >= 0.0D) {
									GL11.glColor4f(1.0F, 1.0F, 1.0F, (float)fade);
									tessellator.startDrawingQuads();
									double minX = (double)blockX + block.minX;
									double maxX = (double)blockX + block.maxX;
									double minY = (double)blockY + block.minY;
									double minZ = (double)blockZ + block.minZ;
									double maxZ = (double)blockZ + block.maxZ;
									float uMin = (float)((x - minX) / 2.0D / (double)shadowRadius + 0.5D);
									float uMax = (float)((x - maxX) / 2.0D / (double)shadowRadius + 0.5D);
									float vMin = (float)((z - minZ) / 2.0D / (double)shadowRadius + 0.5D);
									float vMax = (float)((z - maxZ) / 2.0D / (double)shadowRadius + 0.5D);
									tessellator.addVertexWithUV(minX, minY, minZ, (double)uMin, (double)vMin);
									tessellator.addVertexWithUV(minX, minY, maxZ, (double)uMin, (double)vMax);
									tessellator.addVertexWithUV(maxX, minY, maxZ, (double)uMax, (double)vMax);
									tessellator.addVertexWithUV(maxX, minY, minZ, (double)uMax, (double)vMin);
									tessellator.draw();
								}
							}
						}
					}
				}

				GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
				GL11.glDisable(GL11.GL_BLEND);
				GL11.glDepthMask(true);
			}
		}

		if(entity.fire > 0) {
			GL11.glDisable(GL11.GL_LIGHTING);
			int fireTextureIndex = Block.fire.blockIndexInTexture;
			AtlasUV.calc(fireTextureIndex, TextureAtlas.TERRAIN);
			float uMin = (float)AtlasUV.u1;
			float uMax = (float)AtlasUV.u2;
			float vMin = (float)AtlasUV.v1;
			float vMax = (float)AtlasUV.v2;
			GL11.glPushMatrix();
			GL11.glTranslatef((float)x, (float)y, (float)z);
			float fireScale = entity.width * 1.4F;
			GL11.glScalef(fireScale, fireScale, fireScale);
			this.loadTexture("/terrain.png");
			Tessellator tessellator = Tessellator.instance;
			float fireWidth = 1.0F;
			float yOffset = 0.0F;
			float remainingLayers = entity.height / entity.width;
			GL11.glRotatef(-this.renderManager.playerViewY, 0.0F, 1.0F, 0.0F);
			GL11.glTranslatef(0.0F, 0.0F, 0.4F + (float)((int)remainingLayers) * 0.02F);
			GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
			tessellator.startDrawingQuads();

			while(remainingLayers > 0.0F) {
				tessellator.addVertexWithUV((double)(fireWidth - 0.5F), (double)(0.0F - yOffset), 0.0D, (double)uMax, (double)vMax);
				tessellator.addVertexWithUV(-0.5D, (double)(0.0F - yOffset), 0.0D, (double)uMin, (double)vMax);
				tessellator.addVertexWithUV(-0.5D, (double)(1.4F - yOffset), 0.0D, (double)uMin, (double)vMin);
				tessellator.addVertexWithUV((double)(fireWidth - 0.5F), (double)(1.4F - yOffset), 0.0D, (double)uMax, (double)vMin);
				--remainingLayers;
				--yOffset;
				fireWidth *= 0.9F;
				GL11.glTranslatef(0.0F, 0.0F, -0.04F);
			}

			tessellator.draw();
			GL11.glPopMatrix();
			GL11.glEnable(GL11.GL_LIGHTING);
		}
	}
}