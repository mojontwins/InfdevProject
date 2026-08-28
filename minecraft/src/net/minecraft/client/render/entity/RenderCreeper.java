package net.minecraft.client.render.entity;

import net.minecraft.client.model.ModelCreeper;
import net.minecraft.game.entity.EntityLiving;
import net.minecraft.game.entity.monster.EntityCreeper;
import org.lwjgl.opengl.GL11;
import util.MathHelper;

public final class RenderCreeper extends RenderLiving {
	public RenderCreeper() {
		super(new ModelCreeper(), 0.5F);
	}

	protected final void preRenderCallback(EntityLiving entityLiving, float partialTick) {
		EntityCreeper creeper = (EntityCreeper)entityLiving;
		float fuseFlash = creeper.c(partialTick);
		float scale = 1.0F + MathHelper.sin(fuseFlash * 100.0F) * fuseFlash * 0.01F;
		if(fuseFlash < 0.0F) {
			fuseFlash = 0.0F;
		}

		if(fuseFlash > 1.0F) {
			fuseFlash = 1.0F;
		}

		fuseFlash *= fuseFlash;
		fuseFlash *= fuseFlash;
		float bodyScale = (1.0F + fuseFlash * 0.4F) * scale;
		float headScale = (1.0F + fuseFlash * 0.1F) / scale;
		GL11.glScalef(bodyScale, headScale, bodyScale);
	}

	protected final int getColorMultiplier(EntityLiving entityLiving, float brightness, float partialTick) {
		EntityCreeper creeper = (EntityCreeper)entityLiving;
		float fuseFlash = creeper.c(partialTick);
		if((int)(fuseFlash * 10.0F) % 2 == 0) {
			return 0;
		} else {
			int alpha = (int)(fuseFlash * 0.2F * 255.0F);
			if(alpha < 0) {
				alpha = 0;
			}

			if(alpha > 255) {
				alpha = 255;
			}

			return alpha << 24 | 16711680 | '\uff00' | 255;
		}
	}
}