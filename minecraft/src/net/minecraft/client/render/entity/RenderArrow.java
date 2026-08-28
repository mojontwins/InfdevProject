package net.minecraft.client.render.entity;

import net.minecraft.client.render.Tessellator;
import net.minecraft.game.entity.Entity;
import net.minecraft.game.entity.projectile.EntityArrow;
import org.lwjgl.opengl.GL11;
import util.MathHelper;

public final class RenderArrow extends Render {
	public final void doRender(Entity entity, double x, double y, double z, float yaw, float partialTick) {
		EntityArrow arrow = (EntityArrow)entity;
		this.loadTexture("/item/arrows.png");
		GL11.glPushMatrix();
		GL11.glTranslatef((float)x, (float)y, (float)z);
		GL11.glRotatef(arrow.prevRotationYaw + (arrow.rotationYaw - arrow.prevRotationYaw) * partialTick - 90.0F, 0.0F, 1.0F, 0.0F);
		GL11.glRotatef(arrow.prevRotationPitch + (arrow.rotationPitch - arrow.prevRotationPitch) * partialTick, 0.0F, 0.0F, 1.0F);
		Tessellator tessellator = Tessellator.instance;
		GL11.glEnable(GL11.GL_NORMALIZE);
		float shake = (float)arrow.arrowShake - partialTick;
		if(shake > 0.0F) {
			shake = -MathHelper.sin(shake * 3.0F) * shake;
			GL11.glRotatef(shake, 0.0F, 0.0F, 1.0F);
		}

		GL11.glRotatef(45.0F, 1.0F, 0.0F, 0.0F);
		GL11.glScalef(0.05625F, 0.05625F, 0.05625F);
		GL11.glTranslatef(-4.0F, 0.0F, 0.0F);
		GL11.glNormal3f(0.05625F, 0.0F, 0.0F);
		tessellator.startDrawingQuads();
		tessellator.addVertexWithUV(-7.0D, -2.0D, -2.0D, 0.0D, 0.15625D);
		tessellator.addVertexWithUV(-7.0D, -2.0D, 2.0D, 0.15625D, 0.15625D);
		tessellator.addVertexWithUV(-7.0D, 2.0D, 2.0D, 0.15625D, 0.3125D);
		tessellator.addVertexWithUV(-7.0D, 2.0D, -2.0D, 0.0D, 0.3125D);
		tessellator.draw();
		GL11.glNormal3f(-0.05625F, 0.0F, 0.0F);
		tessellator.startDrawingQuads();
		tessellator.addVertexWithUV(-7.0D, 2.0D, -2.0D, 0.0D, 0.15625D);
		tessellator.addVertexWithUV(-7.0D, 2.0D, 2.0D, 0.15625D, 0.15625D);
		tessellator.addVertexWithUV(-7.0D, -2.0D, 2.0D, 0.15625D, 0.3125D);
		tessellator.addVertexWithUV(-7.0D, -2.0D, -2.0D, 0.0D, 0.3125D);
		tessellator.draw();

		for(int side = 0; side < 4; ++side) {
			GL11.glRotatef(90.0F, 1.0F, 0.0F, 0.0F);
			GL11.glNormal3f(0.0F, 0.0F, 0.05625F);
			tessellator.startDrawingQuads();
			tessellator.addVertexWithUV(-8.0D, -2.0D, 0.0D, 0.0D, 0.0D);
			tessellator.addVertexWithUV(8.0D, -2.0D, 0.0D, 0.5D, 0.0D);
			tessellator.addVertexWithUV(8.0D, 2.0D, 0.0D, 0.5D, 0.15625D);
			tessellator.addVertexWithUV(-8.0D, 2.0D, 0.0D, 0.0D, 0.15625D);
			tessellator.draw();
		}

		GL11.glDisable(GL11.GL_NORMALIZE);
		GL11.glPopMatrix();
	}
}