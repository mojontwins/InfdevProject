package net.minecraft.client.render.entity;

import net.minecraft.client.render.Tessellator;
import net.minecraft.game.entity.Entity;
import net.minecraft.game.physics.AxisAlignedBB;
import org.lwjgl.opengl.GL11;

public final class RenderEntity extends Render {
	public final void doRender(Entity entity, double x, double y, double z, float yaw, float partialTick) {
		GL11.glPushMatrix();
		GL11.glTranslatef((float)(x - entity.lastTickPosX), (float)(y - entity.lastTickPosY), (float)(z - entity.lastTickPosZ));
		AxisAlignedBB boundingBox = entity.boundingBox;
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		Tessellator tessellator = Tessellator.instance;
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		tessellator.startDrawingQuads();
		Tessellator.setNormal(0.0F, 0.0F, -1.0F);
		tessellator.addVertex(boundingBox.minX, boundingBox.maxY, boundingBox.minZ);
		tessellator.addVertex(boundingBox.maxX, boundingBox.maxY, boundingBox.minZ);
		tessellator.addVertex(boundingBox.maxX, boundingBox.minY, boundingBox.minZ);
		tessellator.addVertex(boundingBox.minX, boundingBox.minY, boundingBox.minZ);
		Tessellator.setNormal(0.0F, 0.0F, 1.0F);
		tessellator.addVertex(boundingBox.minX, boundingBox.minY, boundingBox.maxZ);
		tessellator.addVertex(boundingBox.maxX, boundingBox.minY, boundingBox.maxZ);
		tessellator.addVertex(boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ);
		tessellator.addVertex(boundingBox.minX, boundingBox.maxY, boundingBox.maxZ);
		Tessellator.setNormal(0.0F, -1.0F, 0.0F);
		tessellator.addVertex(boundingBox.minX, boundingBox.minY, boundingBox.minZ);
		tessellator.addVertex(boundingBox.maxX, boundingBox.minY, boundingBox.minZ);
		tessellator.addVertex(boundingBox.maxX, boundingBox.minY, boundingBox.maxZ);
		tessellator.addVertex(boundingBox.minX, boundingBox.minY, boundingBox.maxZ);
		Tessellator.setNormal(0.0F, 1.0F, 0.0F);
		tessellator.addVertex(boundingBox.minX, boundingBox.maxY, boundingBox.maxZ);
		tessellator.addVertex(boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ);
		tessellator.addVertex(boundingBox.maxX, boundingBox.maxY, boundingBox.minZ);
		tessellator.addVertex(boundingBox.minX, boundingBox.maxY, boundingBox.minZ);
		Tessellator.setNormal(-1.0F, 0.0F, 0.0F);
		tessellator.addVertex(boundingBox.minX, boundingBox.minY, boundingBox.maxZ);
		tessellator.addVertex(boundingBox.minX, boundingBox.maxY, boundingBox.maxZ);
		tessellator.addVertex(boundingBox.minX, boundingBox.maxY, boundingBox.minZ);
		tessellator.addVertex(boundingBox.minX, boundingBox.minY, boundingBox.minZ);
		Tessellator.setNormal(1.0F, 0.0F, 0.0F);
		tessellator.addVertex(boundingBox.maxX, boundingBox.minY, boundingBox.minZ);
		tessellator.addVertex(boundingBox.maxX, boundingBox.maxY, boundingBox.minZ);
		tessellator.addVertex(boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ);
		tessellator.addVertex(boundingBox.maxX, boundingBox.minY, boundingBox.maxZ);
		tessellator.draw();
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glPopMatrix();
	}
}