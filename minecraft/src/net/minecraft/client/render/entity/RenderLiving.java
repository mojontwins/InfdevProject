package net.minecraft.client.render.entity;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.game.entity.Entity;
import net.minecraft.game.entity.EntityLiving;
import net.minecraft.game.item.Item;
import net.minecraft.game.item.ItemArmor;
import net.minecraft.game.item.ItemStack;
import org.lwjgl.opengl.GL11;
import util.MathHelper;

public class RenderLiving extends Render {
	protected ModelBase mainModel;
	private ModelBase renderPassModel;
	private ModelBiped modelArmorChestplate = new ModelBiped(1.0F);
	private ModelBiped modelArmor = new ModelBiped(0.5F);
	private static final String[] armorFilenamePrefix = new String[]{"cloth", "chain", "iron", "diamond", "gold"};

	public RenderLiving(ModelBase mainModel, float shadowSize) {
		this.mainModel = mainModel;
		this.shadowSize = shadowSize;
	}

	public final void setRenderPassModel(ModelBase renderPassModel) {
		this.renderPassModel = renderPassModel;
	}

	public void a(EntityLiving entity, double x, double y, double z, float yaw, float partialTick) {
		GL11.glPushMatrix();
		GL11.glDisable(GL11.GL_CULL_FACE);

		try {
			yaw = entity.prevRenderYawOffset + (entity.renderYawOffset - entity.prevRenderYawOffset) * partialTick;
			float interpolatedYaw = entity.prevRotationYaw + (entity.rotationYaw - entity.prevRotationYaw) * partialTick;
			float interpolatedPitch = entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * partialTick;
			GL11.glTranslatef((float)x, (float)y, (float)z);
			float ticks = (float)entity.ticksExisted + partialTick;
			GL11.glRotatef(180.0F - yaw, 0.0F, 1.0F, 0.0F);
			float deathRotation;
			if(entity.deathTime > 0) {
				deathRotation = ((float)entity.deathTime + partialTick - 1.0F) / 20.0F * 1.6F;
				deathRotation = MathHelper.sqrt_float(deathRotation);
				if(deathRotation > 1.0F) {
					deathRotation = 1.0F;
				}

				GL11.glRotatef(deathRotation * this.getDeathMaxRotation(entity), 0.0F, 0.0F, 1.0F);
			}

			GL11.glScalef(-(1.0F / 16.0F), -(1.0F / 16.0F), 1.0F / 16.0F);
			this.preRenderCallback(entity, partialTick);
			GL11.glTranslatef(0.0F, -24.0F, 0.0F);
			GL11.glEnable(GL11.GL_NORMALIZE);
			float limbSwingProgress = entity.prevLimbSwing + (entity.limbSwing - entity.prevLimbSwing) * partialTick;
			float headPitch = entity.limbSwingPitch - entity.limbSwing * (1.0F - partialTick);
			if(limbSwingProgress > 1.0F) {
				limbSwingProgress = 1.0F;
			}

			this.loadDownloadableImageTexture(entity.skinUrl, entity.getEntityTexture());
			GL11.glEnable(GL11.GL_ALPHA_TEST);
			this.mainModel.render(headPitch, limbSwingProgress, ticks, interpolatedYaw - yaw, interpolatedPitch, 1.0F);

			for(int pass = 0; pass < 4; ++pass) {
				if(this.shouldRenderPass(entity, pass)) {
					this.renderPassModel.render(headPitch, limbSwingProgress, ticks, interpolatedYaw - yaw, interpolatedPitch, 1.0F);
					GL11.glDisable(GL11.GL_BLEND);
					GL11.glEnable(GL11.GL_ALPHA_TEST);
				}
			}

			float brightness = entity.getEntityBrightness(partialTick);
			int colorMultiplier = this.getColorMultiplier(entity, brightness, partialTick);
			if(colorMultiplier >>> 24 > 0 || entity.hurtTime > 0 || entity.deathTime > 0) {
				GL11.glDisable(GL11.GL_TEXTURE_2D);
				GL11.glDisable(GL11.GL_ALPHA_TEST);
				GL11.glEnable(GL11.GL_BLEND);
				GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
				GL11.glDepthFunc(GL11.GL_EQUAL);
				if(entity.hurtTime > 0 || entity.deathTime > 0) {
					GL11.glColor4f(brightness, 0.0F, 0.0F, 0.4F);
					this.mainModel.render(headPitch, limbSwingProgress, ticks, interpolatedYaw - yaw, interpolatedPitch, 1.0F);

					for(int pass = 0; pass < 4; ++pass) {
						if(this.shouldRenderPass(entity, pass)) {
							GL11.glColor4f(brightness, 0.0F, 0.0F, 0.4F);
							this.renderPassModel.render(headPitch, limbSwingProgress, ticks, interpolatedYaw - yaw, interpolatedPitch, 1.0F);
						}
					}
				}

				if(colorMultiplier >>> 24 > 0) {
					float red = (float)(colorMultiplier >> 16 & 255) / 255.0F;
					float green = (float)(colorMultiplier >> 8 & 255) / 255.0F;
					float blue = (float)(colorMultiplier & 255) / 255.0F;
					float alpha = (float)(colorMultiplier >>> 24) / 255.0F;
					GL11.glColor4f(red, green, blue, alpha);
					this.mainModel.render(headPitch, limbSwingProgress, ticks, interpolatedYaw - yaw, interpolatedPitch, 1.0F);

					for(int pass = 0; pass < 4; ++pass) {
						if(this.shouldRenderPass(entity, pass)) {
							GL11.glColor4f(red, green, blue, alpha);
							this.renderPassModel.render(headPitch, limbSwingProgress, ticks, interpolatedYaw - yaw, interpolatedPitch, 1.0F);
						}
					}
				}

				GL11.glDepthFunc(GL11.GL_LEQUAL);
				GL11.glDisable(GL11.GL_BLEND);
				GL11.glEnable(GL11.GL_ALPHA_TEST);
				GL11.glEnable(GL11.GL_TEXTURE_2D);
			}

			GL11.glDisable(GL11.GL_NORMALIZE);
		} catch (Exception exception) {
			exception.printStackTrace();
		}

		GL11.glEnable(GL11.GL_CULL_FACE);
		GL11.glPopMatrix();
	}

	/**
	 * Draws the armour overlay for any living being that wears some (players
	 * via the inventory, mobs via their own armour slots). The four passes map
	 * one to one onto the armour slots — helmet, chestplate, leggings, boots —
	 * using the two fitted biped models.
	 */
	protected boolean shouldRenderPass(EntityLiving entity, int renderPass) {
		int armorSlot = 3 - renderPass;
		ItemStack stack = entity.getArmorInSlot(armorSlot);
		if(stack != null) {
			Item item = stack.getItem();
			if(item instanceof ItemArmor) {
				ItemArmor armor = (ItemArmor)item;
				this.loadTexture("/armor/" + armorFilenamePrefix[armor.renderIndex] + "_" + (renderPass == 2 ? 2 : 1) + ".png");
				ModelBiped model = renderPass == 2 ? this.modelArmor : this.modelArmorChestplate;
				model.bipedHead.showModel = renderPass == 0;
				model.bipedHeadwear.showModel = renderPass == 0;
				model.bipedBody.showModel = renderPass == 1 || renderPass == 2;
				model.bipedRightArm.showModel = renderPass == 1;
				model.bipedLeftArm.showModel = renderPass == 1;
				model.bipedRightLeg.showModel = renderPass == 2 || renderPass == 3;
				model.bipedLeftLeg.showModel = renderPass == 2 || renderPass == 3;
				this.setRenderPassModel(model);
				return true;
			}
		}

		return false;
	}

	protected float getDeathMaxRotation(EntityLiving entity) {
		return 90.0F;
	}

	protected int getColorMultiplier(EntityLiving entity, float brightness, float partialTick) {
		return 0;
	}

	protected void preRenderCallback(EntityLiving entity, float partialTick) {
	}

	public void doRender(Entity entity, double x, double y, double z, float yaw, float partialTick) {
		this.a((EntityLiving)entity, x, y, z, yaw, partialTick);
	}
}