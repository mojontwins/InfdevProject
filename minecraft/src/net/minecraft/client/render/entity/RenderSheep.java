package net.minecraft.client.render.entity;

import net.minecraft.client.model.ModelBase;
import net.minecraft.game.entity.EntityLiving;
import net.minecraft.game.entity.animal.EntitySheep;

public final class RenderSheep extends RenderLiving {
	public RenderSheep(ModelBase mainModel, ModelBase furModel, float shadowSize) {
		super(mainModel, shadowSize);
		this.setRenderPassModel(furModel);
	}

	protected final boolean shouldRenderPass(EntityLiving entity, int renderPass) {
		EntitySheep sheep = (EntitySheep)entity;
		this.loadTexture("/mob/sheep_fur.png");
		return renderPass == 0 && !sheep.sheared;
	}
}