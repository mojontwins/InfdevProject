package net.minecraft.client.render.entity;

import net.minecraft.client.render.RenderBlocks;
import net.minecraft.game.entity.Entity;
import net.minecraft.game.entity.misc.EntityFallingSand;
import net.minecraft.game.world.block.Block;
import org.lwjgl.opengl.GL11;

public final class RenderFallingSand extends Render {
	private RenderBlocks blockRenderer = new RenderBlocks();

	public RenderFallingSand() {
		this.shadowSize = 0.5F;
	}

	@Override
	public final void doRender(Entity entity, double posX, double posY, double posZ, float yaw, float partialTick) {
		EntityFallingSand fallingSand = (EntityFallingSand)entity;
		GL11.glPushMatrix();
		GL11.glTranslatef((float)posX, (float)posY, (float)posZ);
		this.loadTexture("/terrain.png");
		Block block = Block.blocksList[fallingSand.blockID];
		this.blockRenderer.renderBlockOnInventory(block, 0);
		GL11.glPopMatrix();
	}
}
