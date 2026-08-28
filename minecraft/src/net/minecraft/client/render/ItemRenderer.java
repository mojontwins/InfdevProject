package net.minecraft.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.RenderHelper;
import net.minecraft.client.player.EntityPlayerSP;
import net.minecraft.client.render.entity.Render;
import net.minecraft.client.render.entity.RenderManager;
import net.minecraft.client.render.entity.RenderPlayer;
import net.minecraft.game.item.ItemStack;
import net.minecraft.game.world.block.Block;
import org.lwjgl.opengl.GL11;
import util.MathHelper;

public final class ItemRenderer {
	private Minecraft mc;
	private ItemStack itemToRender = null;
	private float equippedProgress = 0.0F;
	private float prevEquippedProgress = 0.0F;
	private int equippedItemSlot = 0;
	private boolean itemRenderBool = false;
	private RenderBlocks renderBlocksInstance = new RenderBlocks();

	public ItemRenderer(Minecraft minecraft) {
		this.mc = minecraft;
	}

	public final void renderItemInFirstPerson(float partialTick) {
		float equipProgress = this.prevEquippedProgress + (this.equippedProgress - this.prevEquippedProgress) * partialTick;
		EntityPlayerSP player = this.mc.thePlayer;
		GL11.glPushMatrix();
		GL11.glRotatef(player.prevRotationPitch + (player.rotationPitch - player.prevRotationPitch) * partialTick, 1.0F, 0.0F, 0.0F);
		GL11.glRotatef(player.prevRotationYaw + (player.rotationYaw - player.prevRotationYaw) * partialTick, 0.0F, 1.0F, 0.0F);
		RenderHelper.enableStandardItemLighting();
		GL11.glPopMatrix();
		float brightness = this.mc.theWorld.getBrightness(MathHelper.floor_double(player.posX), MathHelper.floor_double(player.posY), MathHelper.floor_double(player.posZ));
		GL11.glColor4f(brightness, brightness, brightness, 1.0F);
		float swingProgress;
		float swingSin;
		if(this.itemToRender != null) {
			GL11.glPushMatrix();
			if(this.itemRenderBool) {
				brightness = ((float)this.equippedItemSlot + partialTick) / 8.0F;
				swingProgress = MathHelper.sin(brightness * (float)Math.PI);
				swingSin = MathHelper.sin(MathHelper.sqrt_float(brightness) * (float)Math.PI);
				GL11.glTranslatef(-swingSin * 0.4F, MathHelper.sin(MathHelper.sqrt_float(brightness) * (float)Math.PI * 2.0F) * 0.2F, -swingProgress * 0.2F);
			}

			GL11.glTranslatef(0.56F, -0.52F - (1.0F - equipProgress) * 0.6F, -0.71999997F);
			GL11.glRotatef(45.0F, 0.0F, 1.0F, 0.0F);
			GL11.glEnable(GL11.GL_NORMALIZE);
			if(this.itemRenderBool) {
				brightness = ((float)this.equippedItemSlot + partialTick) / 8.0F;
				swingProgress = MathHelper.sin(brightness * brightness * (float)Math.PI);
				swingSin = MathHelper.sin(MathHelper.sqrt_float(brightness) * (float)Math.PI);
				GL11.glRotatef(-swingProgress * 20.0F, 0.0F, 1.0F, 0.0F);
				GL11.glRotatef(-swingSin * 20.0F, 0.0F, 0.0F, 1.0F);
				GL11.glRotatef(-swingSin * 80.0F, 1.0F, 0.0F, 0.0F);
			}

			GL11.glScalef(0.4F, 0.4F, 0.4F);
			if(this.itemToRender.itemID < 256 && Block.blocksList[this.itemToRender.itemID].getRenderType() == 0) {
				GL11.glBindTexture(GL11.GL_TEXTURE_2D, this.mc.renderEngine.getTexture("/terrain.png"));
				this.renderBlocksInstance.renderBlockOnInventory(Block.blocksList[this.itemToRender.itemID]);
			} else {
				if(this.itemToRender.itemID < 256) {
					GL11.glBindTexture(GL11.GL_TEXTURE_2D, this.mc.renderEngine.getTexture("/terrain.png"));
				} else {
					GL11.glBindTexture(GL11.GL_TEXTURE_2D, this.mc.renderEngine.getTexture("/gui/items.png"));
				}

				Tessellator tessellator = Tessellator.instance;
				float texU1 = (float)(this.itemToRender.getItem().getIconFromDamage() % 16 << 4) / 256.0F;
				float texU2 = (float)((this.itemToRender.getItem().getIconFromDamage() % 16 << 4) + 16) / 256.0F;
				float texV1 = (float)(this.itemToRender.getItem().getIconFromDamage() / 16 << 4) / 256.0F;
				float texV2 = (float)((this.itemToRender.getItem().getIconFromDamage() / 16 << 4) + 16) / 256.0F;
				GL11.glEnable(GL11.GL_NORMALIZE);
				GL11.glTranslatef(0.0F, -0.3F, 0.0F);
				GL11.glScalef(1.5F, 1.5F, 1.5F);
				GL11.glRotatef(50.0F, 0.0F, 1.0F, 0.0F);
				GL11.glRotatef(335.0F, 0.0F, 0.0F, 1.0F);
				GL11.glTranslatef(-(15.0F / 16.0F), -(1.0F / 16.0F), 0.0F);
				Tessellator.setNormal(0.0F, 0.0F, 1.0F);
				tessellator.startDrawingQuads();
				tessellator.addVertexWithUV(0.0D, 0.0D, 0.0D, (double)texU2, (double)texV2);
				tessellator.addVertexWithUV(1.0D, 0.0D, 0.0D, (double)texU1, (double)texV2);
				tessellator.addVertexWithUV(1.0D, 1.0D, 0.0D, (double)texU1, (double)texV1);
				tessellator.addVertexWithUV(0.0D, 1.0D, 0.0D, (double)texU2, (double)texV1);
				tessellator.draw();
				Tessellator.setNormal(0.0F, 0.0F, -1.0F);
				tessellator.startDrawingQuads();
				tessellator.addVertexWithUV(0.0D, 1.0D, -0.0625D, (double)texU2, (double)texV1);
				tessellator.addVertexWithUV(1.0D, 1.0D, -0.0625D, (double)texU1, (double)texV1);
				tessellator.addVertexWithUV(1.0D, 0.0D, -0.0625D, (double)texU1, (double)texV2);
				tessellator.addVertexWithUV(0.0D, 0.0D, -0.0625D, (double)texU2, (double)texV2);
				tessellator.draw();
				Tessellator.setNormal(-1.0F, 0.0F, 0.0F);
				tessellator.startDrawingQuads();
				float texPos;
				for(int i = 0; i < 16; ++i) {
					float uPos = (float)i / 16.0F;
					texPos = texU2 + (texU1 - texU2) * uPos - 0.001953125F;
					tessellator.addVertexWithUV((double)uPos, 0.0D, -0.0625D, (double)texPos, (double)texV2);
					tessellator.addVertexWithUV((double)uPos, 0.0D, 0.0D, (double)texPos, (double)texV2);
					tessellator.addVertexWithUV((double)uPos, 1.0D, 0.0D, (double)texPos, (double)texV1);
					tessellator.addVertexWithUV((double)uPos, 1.0D, -0.0625D, (double)texPos, (double)texV1);
				}

				tessellator.draw();
				Tessellator.setNormal(1.0F, 0.0F, 0.0F);
				tessellator.startDrawingQuads();

				for(int i = 0; i < 16; ++i) {
					float uPos = (float)i / 16.0F;
					texPos = texU2 + (texU1 - texU2) * uPos - 0.001953125F;
					uPos = uPos * 1.0F + 1.0F / 16.0F;
					tessellator.addVertexWithUV((double)uPos, 1.0D, -0.0625D, (double)texPos, (double)texV1);
					tessellator.addVertexWithUV((double)uPos, 1.0D, 0.0D, (double)texPos, (double)texV1);
					tessellator.addVertexWithUV((double)uPos, 0.0D, 0.0D, (double)texPos, (double)texV2);
					tessellator.addVertexWithUV((double)uPos, 0.0D, -0.0625D, (double)texPos, (double)texV2);
				}

				tessellator.draw();
				Tessellator.setNormal(0.0F, 1.0F, 0.0F);
				tessellator.startDrawingQuads();

				for(int i = 0; i < 16; ++i) {
					float uPos = (float)i / 16.0F;
					texPos = texV2 + (texV1 - texV2) * uPos - 0.001953125F;
					uPos = uPos * 1.0F + 1.0F / 16.0F;
					tessellator.addVertexWithUV(0.0D, (double)uPos, 0.0D, (double)texU2, (double)texPos);
					tessellator.addVertexWithUV(1.0D, (double)uPos, 0.0D, (double)texU1, (double)texPos);
					tessellator.addVertexWithUV(1.0D, (double)uPos, -0.0625D, (double)texU1, (double)texPos);
					tessellator.addVertexWithUV(0.0D, (double)uPos, -0.0625D, (double)texU2, (double)texPos);
				}

				tessellator.draw();
				Tessellator.setNormal(0.0F, -1.0F, 0.0F);
				tessellator.startDrawingQuads();

				for(int i = 0; i < 16; ++i) {
					float uPos = (float)i / 16.0F;
					texPos = texV2 + (texV1 - texV2) * uPos - 0.001953125F;
					tessellator.addVertexWithUV(1.0D, (double)uPos, 0.0D, (double)texU1, (double)texPos);
					tessellator.addVertexWithUV(0.0D, (double)uPos, 0.0D, (double)texU2, (double)texPos);
					tessellator.addVertexWithUV(0.0D, (double)uPos, -0.0625D, (double)texU2, (double)texPos);
					tessellator.addVertexWithUV(1.0D, (double)uPos, -0.0625D, (double)texU1, (double)texPos);
				}

				tessellator.draw();
				GL11.glDisable(GL11.GL_NORMALIZE);
			}

			GL11.glPopMatrix();
		} else {
			GL11.glPushMatrix();
			if(this.itemRenderBool) {
				brightness = ((float)this.equippedItemSlot + partialTick) / 8.0F;
				swingProgress = MathHelper.sin(brightness * (float)Math.PI);
				swingSin = MathHelper.sin(MathHelper.sqrt_float(brightness) * (float)Math.PI);
				GL11.glTranslatef(-swingSin * 0.3F, MathHelper.sin(MathHelper.sqrt_float(brightness) * (float)Math.PI * 2.0F) * 0.4F, -swingProgress * 0.4F);
			}

			GL11.glTranslatef(0.64000005F, -0.6F - (1.0F - equipProgress) * 0.6F, -0.71999997F);
			GL11.glRotatef(45.0F, 0.0F, 1.0F, 0.0F);
			GL11.glEnable(GL11.GL_NORMALIZE);
			if(this.itemRenderBool) {
				brightness = ((float)this.equippedItemSlot + partialTick) / 8.0F;
				swingProgress = MathHelper.sin(brightness * brightness * (float)Math.PI);
				swingSin = MathHelper.sin(MathHelper.sqrt_float(brightness) * (float)Math.PI);
				GL11.glRotatef(swingSin * 70.0F, 0.0F, 1.0F, 0.0F);
				GL11.glRotatef(-swingProgress * 20.0F, 0.0F, 0.0F, 1.0F);
			}

			GL11.glBindTexture(GL11.GL_TEXTURE_2D, this.mc.renderEngine.getTextureForDownloadableImage(this.mc.thePlayer.skinUrl, this.mc.thePlayer.getEntityTexture()));
			GL11.glTranslatef(-0.2F, -0.3F, 0.1F);
			GL11.glRotatef(120.0F, 0.0F, 0.0F, 1.0F);
			GL11.glRotatef(200.0F, 1.0F, 0.0F, 0.0F);
			GL11.glRotatef(-135.0F, 0.0F, 1.0F, 0.0F);
			GL11.glScalef(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
			GL11.glTranslatef(6.0F, 0.0F, 0.0F);
			Render render = RenderManager.instance.getEntityRenderObject(this.mc.thePlayer);
			RenderPlayer renderPlayer = (RenderPlayer)render;
			renderPlayer.drawFirstPersonHand();
			GL11.glPopMatrix();
		}

		GL11.glDisable(GL11.GL_NORMALIZE);
		RenderHelper.disableStandardItemLighting();
	}

	public final void renderOverlays(float partialTick) {
		GL11.glDisable(GL11.GL_ALPHA_TEST);
		int textureId;
		Tessellator tessellator;
		float uMin;
		float vMin;
		if(this.mc.thePlayer.fire > 0) {
			textureId = this.mc.renderEngine.getTexture("/terrain.png");
			GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
			tessellator = Tessellator.instance;
			GL11.glColor4f(1.0F, 1.0F, 1.0F, 0.9F);
			GL11.glEnable(GL11.GL_BLEND);
			GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

			for(textureId = 0; textureId < 2; ++textureId) {
				GL11.glPushMatrix();
				int fireTexture = Block.fire.blockIndexInTexture + (textureId << 4);
				int texX = (fireTexture & 15) << 4;
				fireTexture &= 240;
				float uMax = (float)texX / 256.0F;
				float uEdge = ((float)texX + 15.99F) / 256.0F;
				uMin = (float)fireTexture / 256.0F;
				vMin = ((float)fireTexture + 15.99F) / 256.0F;
				GL11.glTranslatef((float)(-((textureId << 1) - 1)) * 0.24F, -0.3F, 0.0F);
				GL11.glRotatef((float)((textureId << 1) - 1) * 10.0F, 0.0F, 1.0F, 0.0F);
				tessellator.startDrawingQuads();
				tessellator.addVertexWithUV(-0.5D, -0.5D, -0.5D, (double)uEdge, (double)vMin);
				tessellator.addVertexWithUV(0.5D, -0.5D, -0.5D, (double)uMax, (double)vMin);
				tessellator.addVertexWithUV(0.5D, 0.5D, -0.5D, (double)uMax, (double)uMin);
				tessellator.addVertexWithUV(-0.5D, 0.5D, -0.5D, (double)uEdge, (double)uMin);
				tessellator.draw();
				GL11.glPopMatrix();
			}

			GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
			GL11.glDisable(GL11.GL_BLEND);
		}

		if(this.mc.thePlayer.isInsideOfMaterial()) {
			textureId = this.mc.renderEngine.getTexture("/water.png");
			GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
			tessellator = Tessellator.instance;
			float brightness = this.mc.thePlayer.getEntityBrightness(partialTick);
			GL11.glColor4f(brightness, brightness, brightness, 0.5F);
			GL11.glEnable(GL11.GL_BLEND);
			GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
			GL11.glPushMatrix();
			uMin = -this.mc.thePlayer.rotationYaw / 64.0F;
			vMin = this.mc.thePlayer.rotationPitch / 64.0F;
			tessellator.startDrawingQuads();
			tessellator.addVertexWithUV(-1.0D, -1.0D, -0.5D, (double)(uMin + 4.0F), (double)(vMin + 4.0F));
			tessellator.addVertexWithUV(1.0D, -1.0D, -0.5D, (double)(uMin + 0.0F), (double)(vMin + 4.0F));
			tessellator.addVertexWithUV(1.0D, 1.0D, -0.5D, (double)(uMin + 0.0F), (double)(vMin + 0.0F));
			tessellator.addVertexWithUV(-1.0D, 1.0D, -0.5D, (double)(uMin + 4.0F), (double)(vMin + 0.0F));
			tessellator.draw();
			GL11.glPopMatrix();
			GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
			GL11.glDisable(GL11.GL_BLEND);
		}

		GL11.glEnable(GL11.GL_ALPHA_TEST);
	}

	public final void updateEquippedItem() {
		this.prevEquippedProgress = this.equippedProgress;
		if(this.itemRenderBool) {
			++this.equippedItemSlot;
			if(this.equippedItemSlot == 8) {
				this.equippedItemSlot = 0;
				this.itemRenderBool = false;
			}
		}

		EntityPlayerSP player = this.mc.thePlayer;
		ItemStack stack = player.inventory.getCurrentItem();
		float targetProgress = stack == this.itemToRender ? 1.0F : 0.0F;
		targetProgress -= this.equippedProgress;
		if(targetProgress < -0.4F) {
			targetProgress = -0.4F;
		}

		if(targetProgress > 0.4F) {
			targetProgress = 0.4F;
		}

		this.equippedProgress += targetProgress;
		if(this.equippedProgress < 0.1F) {
			this.itemToRender = stack;
		}

	}

	public final void resetEquippedProgress2() {
		this.equippedProgress = 0.0F;
	}

	public final void equippedItemRender() {
		this.equippedItemSlot = -1;
		this.itemRenderBool = true;
	}

	public final void resetEquippedProgress() {
		this.equippedProgress = 0.0F;
	}
}