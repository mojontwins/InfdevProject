package net.minecraft.client.render.entity;

import java.util.Random;
import net.minecraft.client.render.Tessellator;
import net.minecraft.game.entity.Entity;
import net.minecraft.game.entity.EntityPainting;
import net.minecraft.game.entity.EnumArt;
import org.lwjgl.opengl.GL11;
import util.MathHelper;

public final class RenderPainting extends Render {
	private Random rand = new Random();

	public final void doRender(Entity entity, double x, double y, double z, float yaw, float partialTick) {
		EntityPainting painting = (EntityPainting)entity;
		this.rand.setSeed(187L);
		GL11.glPushMatrix();
		GL11.glTranslatef((float)x, (float)y, (float)z);
		GL11.glRotatef(yaw, 0.0F, 1.0F, 0.0F);
		GL11.glEnable(GL11.GL_NORMALIZE);
		this.loadTexture("/art/kz.png");
		EnumArt art = painting.art;
		GL11.glScalef(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
		int offsetY = art.offsetY;
		int offsetX = art.offsetX;
		int sizeY = art.sizeY;
		int sizeX = art.sizeX;
		float xStart = (float)(-sizeX) / 2.0F;
		float yStart = (float)(-sizeY) / 2.0F;

		for(int tileX = 0; tileX < sizeX / 16; ++tileX) {
			for(int tileY = 0; tileY < sizeY / 16; ++tileY) {
				float maxX = xStart + (float)(tileX + 1 << 4);
				float minX = xStart + (float)(tileX << 4);
				float maxY = yStart + (float)(tileY + 1 << 4);
				float minY = yStart + (float)(tileY << 4);
				float xCenter = (maxX + minX) / 2.0F;
				float yCenter = (maxY + minY) / 2.0F;
				int blockX = MathHelper.floor_double(painting.posX);
				int blockY = MathHelper.floor_double(painting.posY + (double)(yCenter / 16.0F));
				int blockZ = MathHelper.floor_double(painting.posZ);
				if(painting.direction == 0) {
					blockX = MathHelper.floor_double(painting.posX + (double)(xCenter / 16.0F));
				}

				if(painting.direction == 1) {
					blockZ = MathHelper.floor_double(painting.posZ - (double)(xCenter / 16.0F));
				}

				if(painting.direction == 2) {
					blockX = MathHelper.floor_double(painting.posX - (double)(xCenter / 16.0F));
				}

				if(painting.direction == 3) {
					blockZ = MathHelper.floor_double(painting.posZ + (double)(xCenter / 16.0F));
				}

				float brightness = this.renderManager.worldObj.getBrightness(blockX, blockY, blockZ);
				GL11.glColor3f(brightness, brightness, brightness);
				float texUMinX = (float)(offsetX + sizeX - (tileX << 4)) / 256.0F;
				float texUMaxX = (float)(offsetX + sizeX - (tileX + 1 << 4)) / 256.0F;
				float texVMinY = (float)(offsetY + sizeY - (tileY << 4)) / 256.0F;
				float texVMaxY = (float)(offsetY + sizeY - (tileY + 1 << 4)) / 256.0F;
				Tessellator tessellator = Tessellator.instance;
				tessellator.startDrawingQuads();
				Tessellator.setNormal(0.0F, 0.0F, -1.0F);
				tessellator.addVertexWithUV((double)maxX, (double)minY, -0.5D, (double)texUMaxX, (double)texVMinY);
				tessellator.addVertexWithUV((double)minX, (double)minY, -0.5D, (double)texUMinX, (double)texVMinY);
				tessellator.addVertexWithUV((double)minX, (double)maxY, -0.5D, (double)texUMinX, (double)texVMaxY);
				tessellator.addVertexWithUV((double)maxX, (double)maxY, -0.5D, (double)texUMaxX, (double)texVMaxY);
				Tessellator.setNormal(0.0F, 0.0F, 1.0F);
				tessellator.addVertexWithUV((double)maxX, (double)maxY, 0.5D, 0.75D, 0.0D);
				tessellator.addVertexWithUV((double)minX, (double)maxY, 0.5D, 0.8125D, 0.0D);
				tessellator.addVertexWithUV((double)minX, (double)minY, 0.5D, 0.8125D, 1.0D / 16.0D);
				tessellator.addVertexWithUV((double)maxX, (double)minY, 0.5D, 0.75D, 1.0D / 16.0D);
				Tessellator.setNormal(0.0F, -1.0F, 0.0F);
				tessellator.addVertexWithUV((double)maxX, (double)maxY, -0.5D, 0.75D, 1.0D / 512.0D);
				tessellator.addVertexWithUV((double)minX, (double)maxY, -0.5D, 0.8125D, 1.0D / 512.0D);
				tessellator.addVertexWithUV((double)minX, (double)maxY, 0.5D, 0.8125D, 1.0D / 512.0D);
				tessellator.addVertexWithUV((double)maxX, (double)maxY, 0.5D, 0.75D, 1.0D / 512.0D);
				Tessellator.setNormal(0.0F, 1.0F, 0.0F);
				tessellator.addVertexWithUV((double)maxX, (double)minY, 0.5D, 0.75D, 1.0D / 512.0D);
				tessellator.addVertexWithUV((double)minX, (double)minY, 0.5D, 0.8125D, 1.0D / 512.0D);
				tessellator.addVertexWithUV((double)minX, (double)minY, -0.5D, 0.8125D, 1.0D / 512.0D);
				tessellator.addVertexWithUV((double)maxX, (double)minY, -0.5D, 0.75D, 1.0D / 512.0D);
				Tessellator.setNormal(-1.0F, 0.0F, 0.0F);
				tessellator.addVertexWithUV((double)maxX, (double)maxY, 0.5D, (double)0.7519531F, 0.0D);
				tessellator.addVertexWithUV((double)maxX, (double)minY, 0.5D, (double)0.7519531F, 1.0D / 16.0D);
				tessellator.addVertexWithUV((double)maxX, (double)minY, -0.5D, (double)0.7519531F, 1.0D / 16.0D);
				tessellator.addVertexWithUV((double)maxX, (double)maxY, -0.5D, (double)0.7519531F, 0.0D);
				Tessellator.setNormal(1.0F, 0.0F, 0.0F);
				tessellator.addVertexWithUV((double)minX, (double)maxY, -0.5D, (double)0.7519531F, 0.0D);
				tessellator.addVertexWithUV((double)minX, (double)minY, -0.5D, (double)0.7519531F, 1.0D / 16.0D);
				tessellator.addVertexWithUV((double)minX, (double)minY, 0.5D, (double)0.7519531F, 1.0D / 16.0D);
				tessellator.addVertexWithUV((double)minX, (double)maxY, 0.5D, (double)0.7519531F, 0.0D);
				tessellator.draw();
			}
		}

		GL11.glDisable(GL11.GL_NORMALIZE);
		GL11.glPopMatrix();
	}
}