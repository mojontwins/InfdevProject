package net.minecraft.client.render.entity;

import net.minecraft.client.render.RenderBlocks;
import net.minecraft.game.entity.Entity;
import net.minecraft.game.entity.misc.EntityTNT;
import net.minecraft.game.world.block.Block;
import org.lwjgl.opengl.GL11;

public final class RenderTNT extends Render {
	private RenderBlocks blockRenderer = new RenderBlocks();

	public RenderTNT() {
		this.shadowSize = 0.5F;
	}

	public final void doRender(Entity entity, double x, double y, double z, float yaw, float partialTick) {
		EntityTNT tnt = (EntityTNT)entity;
		GL11.glPushMatrix();
		GL11.glTranslatef((float)x, (float)y, (float)z);
		float expansion;
		if((float)tnt.fuse - partialTick + 1.0F < 10.0F) {
			expansion = 1.0F - ((float)tnt.fuse - partialTick + 1.0F) / 10.0F;
			if(expansion < 0.0F) {
				expansion = 0.0F;
			}

			if(expansion > 1.0F) {
				expansion = 1.0F;
			}

			expansion *= expansion;
			expansion *= expansion;
			expansion = 1.0F + expansion * 0.3F;
			GL11.glScalef(expansion, expansion, expansion);
		}

		expansion = (1.0F - ((float)tnt.fuse - partialTick + 1.0F) / 100.0F) * 0.8F;
		this.loadTexture("/terrain.png");
		this.blockRenderer.renderBlockOnInventory(Block.tnt, 0);
		if(tnt.fuse / 5 % 2 == 0) {
			GL11.glDisable(GL11.GL_TEXTURE_2D);
			GL11.glDisable(GL11.GL_LIGHTING);
			GL11.glEnable(GL11.GL_BLEND);
			GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_DST_ALPHA);
			GL11.glColor4f(1.0F, 1.0F, 1.0F, expansion);
			this.blockRenderer.renderBlockOnInventory(Block.tnt, 0);
			GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
			GL11.glDisable(GL11.GL_BLEND);
			GL11.glEnable(GL11.GL_LIGHTING);
			GL11.glEnable(GL11.GL_TEXTURE_2D);
		}

		GL11.glPopMatrix();
	}
}