package net.minecraft.client.render.entity;

import net.minecraft.client.model.ModelBiped;
import net.minecraft.game.entity.Entity;
import net.minecraft.game.entity.EntityLiving;
import net.minecraft.game.entity.player.EntityPlayer;

public final class RenderPlayer extends RenderLiving {
	private ModelBiped modelBipedMain = (ModelBiped)this.mainModel;

	public RenderPlayer() {
		super(new ModelBiped(0.0F), 0.5F);
	}

	private void renderPlayer(EntityPlayer player, double x, double y, double z, float yaw, float partialTick) {
		super.a(player, x, y - (double)player.yOffset, z, yaw, partialTick);
	}

	public final void drawFirstPersonHand() {
		this.modelBipedMain.bipedRightArm.render(1.0F);
	}

	public final void a(EntityLiving entity, double x, double y, double z, float yaw, float partialTick) {
		this.renderPlayer((EntityPlayer)entity, x, y, z, yaw, partialTick);
	}

	public final void doRender(Entity entity, double x, double y, double z, float yaw, float partialTick) {
		this.renderPlayer((EntityPlayer)entity, x, y, z, yaw, partialTick);
	}
}