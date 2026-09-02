package net.minecraft.client.render.entity;

import java.util.Random;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.render.RenderBlocks;
import net.minecraft.client.render.RenderEngine;
import net.minecraft.client.render.Tessellator;
import net.minecraft.game.entity.Entity;
import net.minecraft.game.entity.misc.EntityItem;
import net.minecraft.game.item.ItemStack;
import net.minecraft.game.world.block.Block;
import org.lwjgl.opengl.GL11;
import util.AtlasUV;
import util.MathHelper;
import util.TextureAtlas;

public final class RenderItem extends Render {
	private RenderBlocks renderBlocks = new RenderBlocks();
	private Random random = new Random();

	public RenderItem() {
		this.shadowSize = 0.15F;
		this.shadowOpaque = 12.0F / 16.0F;
	}

	public final void renderItemIntoGUI(RenderEngine renderEngine, ItemStack itemStack, int x, int y) {
		if(itemStack != null) {
			if(itemStack.itemID < 256 && Block.blocksList[itemStack.itemID].getRenderType() == 0) {
				RenderEngine.bindTexture(renderEngine.getTexture("/terrain.png"));
				Block block = Block.blocksList[itemStack.itemID];
				GL11.glPushMatrix();
				GL11.glTranslatef((float)(x - 2), (float)(y + 3), 0.0F);
				GL11.glScalef(10.0F, 10.0F, 10.0F);
				GL11.glTranslatef(1.0F, 0.5F, 8.0F);
				GL11.glRotatef(210.0F, 1.0F, 0.0F, 0.0F);
				GL11.glRotatef(45.0F, 0.0F, 1.0F, 0.0F);
				GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
				this.renderBlocks.renderBlockOnInventory(block, itemStack.itemDamage, 1.0F);
				GL11.glPopMatrix();
			} else {
				if(itemStack.getItem().getIconFromDamage(itemStack.itemDamage) >= 0) {
					GL11.glDisable(GL11.GL_LIGHTING);
					if(itemStack.itemID < 256) {
						RenderEngine.bindTexture(renderEngine.getTexture("/terrain.png"));
					} else {
						RenderEngine.bindTexture(renderEngine.getTexture("/gui/items.png"));
					}

					int icon = itemStack.getItem().getIconFromDamage(itemStack.itemDamage);
					int iconX = icon % 16 << 4;
					int iconY = icon / 16 << 4;
					int tintColor = itemStack.getItem().getColorFromDamage(itemStack.itemDamage);
					float tr = ((tintColor >> 16) & 0xFF) / 255.0F;
					float tg = ((tintColor >> 8) & 0xFF) / 255.0F;
					float tb = (tintColor & 0xFF) / 255.0F;
					GL11.glColor3f(tr, tg, tb);
					Tessellator tessellator = Tessellator.instance;
					tessellator.startDrawingQuads();
					tessellator.addVertexWithUV((double)x, (double)(y + 16), 0.0D, (double)((float)iconX * 0.00390625F), (double)((float)(iconY + 16) * 0.00390625F));
					tessellator.addVertexWithUV((double)(x + 16), (double)(y + 16), 0.0D, (double)((float)(iconX + 16) * 0.00390625F), (double)((float)(iconY + 16) * 0.00390625F));
					tessellator.addVertexWithUV((double)(x + 16), (double)y, 0.0D, (double)((float)(iconX + 16) * 0.00390625F), (double)((float)iconY * 0.00390625F));
					tessellator.addVertexWithUV((double)x, (double)y, 0.0D, (double)((float)iconX * 0.00390625F), (double)((float)iconY * 0.00390625F));
					tessellator.draw();
					GL11.glColor3f(1.0F, 1.0F, 1.0F);
					GL11.glEnable(GL11.GL_LIGHTING);
				}

			}
		}
	}

	public final void renderItemOverlayIntoGUI(FontRenderer fontRenderer, ItemStack itemStack, int x, int y) {
		if(itemStack != null) {
			if(itemStack.stackSize > 1) {
				String stackSize = "" + itemStack.stackSize;
				GL11.glDisable(GL11.GL_LIGHTING);
				GL11.glDisable(GL11.GL_DEPTH_TEST);
				fontRenderer.drawStringWithShadow(stackSize, x + 19 - 2 - fontRenderer.getStringWidth(stackSize), y + 6 + 3, 16777215);
				GL11.glEnable(GL11.GL_LIGHTING);
				GL11.glEnable(GL11.GL_DEPTH_TEST);
			}

			if(itemStack.itemDamage > 0 && !itemStack.getItem().getHasSubTypes()) {
				int damagedLength = 13 - itemStack.itemDamage * 13 / itemStack.getMaxDamage();
				int damageColor = 255 - itemStack.itemDamage * 255 / itemStack.getMaxDamage();
				GL11.glDisable(GL11.GL_LIGHTING);
				GL11.glDisable(GL11.GL_DEPTH_TEST);
				GL11.glDisable(GL11.GL_TEXTURE_2D);
				Tessellator tessellator = Tessellator.instance;
				int damagedColor = (255 - damageColor) << 16 | damageColor << 8;
				int backgroundColor = (255 - damageColor) / 4 << 16 | 16128;
				renderQuad(tessellator, x + 2, y + 13, 13, 2, 0);
				renderQuad(tessellator, x + 2, y + 13, 12, 1, backgroundColor);
				renderQuad(tessellator, x + 2, y + 13, damagedLength, 1, damagedColor);
				GL11.glEnable(GL11.GL_TEXTURE_2D);
				GL11.glEnable(GL11.GL_LIGHTING);
				GL11.glEnable(GL11.GL_DEPTH_TEST);
				GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
			}

		}
	}

	private static void renderQuad(Tessellator tessellator, int x, int y, int width, int height, int color) {
		tessellator.startDrawingQuads();
		tessellator.setColorOpaque_I(color);
		tessellator.addVertex((double)x, (double)y, 0.0D);
		tessellator.addVertex((double)x, (double)(y + height), 0.0D);
		tessellator.addVertex((double)(x + width), (double)(y + height), 0.0D);
		tessellator.addVertex((double)(x + width), (double)y, 0.0D);
		tessellator.draw();
	}

	public final void doRender(Entity entity, double x, double y, double z, float yaw, float partialTick) {
		EntityItem item = (EntityItem)entity;
		this.random.setSeed(187L);
		ItemStack itemStack = item.item;
		GL11.glPushMatrix();
		float bob = MathHelper.sin(((float)item.age + partialTick) / 10.0F + item.hoverStart) * 0.1F + 0.1F;
		float rotation = (((float)item.age + partialTick) / 20.0F + item.hoverStart) * (180.0F / (float)Math.PI);
		byte itemCount = 1;
		if(item.item.stackSize > 1) {
			itemCount = 2;
		}

		if(item.item.stackSize > 5) {
			itemCount = 3;
		}

		if(item.item.stackSize > 20) {
			itemCount = 4;
		}

		GL11.glTranslatef((float)x, (float)y + bob, (float)z);
		GL11.glEnable(GL11.GL_NORMALIZE);
		float scale;
		float texU1;
		if(itemStack.itemID < 256 && Block.blocksList[itemStack.itemID].getRenderType() == 0) {
			GL11.glRotatef(rotation, 0.0F, 1.0F, 0.0F);
			this.loadTexture("/terrain.png");
			scale = 0.25F;
			if(!Block.blocksList[itemStack.itemID].renderAsNormalBlock() && itemStack.itemID != Block.stairSingle.blockID) {
				scale = 0.5F;
			}

			GL11.glScalef(scale, scale, scale);

			for(int count = 0; count < itemCount; ++count) {
				GL11.glPushMatrix();
				if(count > 0) {
					float offsetX = (this.random.nextFloat() * 2.0F - 1.0F) * 0.2F / scale;
					float offsetY = (this.random.nextFloat() * 2.0F - 1.0F) * 0.2F / scale;
					float offsetZ = (this.random.nextFloat() * 2.0F - 1.0F) * 0.2F / scale;
					GL11.glTranslatef(offsetX, offsetY, offsetZ);
				}

				this.renderBlocks.renderBlockOnInventory(Block.blocksList[itemStack.itemID], itemStack.itemDamage, item.getEntityBrightness(partialTick));
				GL11.glPopMatrix();
			}
		} else {
			GL11.glScalef(0.5F, 0.5F, 0.5F);
			int icon = itemStack.getItem().getIconFromDamage(itemStack.itemDamage);
			int tintColor = itemStack.getItem().getColorFromDamage(itemStack.itemDamage);
			float tr = ((tintColor >> 16) & 0xFF) / 255.0F;
			float tg = ((tintColor >> 8) & 0xFF) / 255.0F;
			float tb = (tintColor & 0xFF) / 255.0F;
			if(itemStack.itemID < 256) {
				this.loadTexture("/terrain.png");
			} else {
				this.loadTexture("/gui/items.png");
			}

			Tessellator tessellator = Tessellator.instance;
			// Block items are cut from the terrain atlas, everything else from the item atlas.
			TextureAtlas itemAtlas = itemStack.itemID < 256 ? TextureAtlas.TERRAIN : TextureAtlas.ITEMS;
			AtlasUV.calc(icon, itemAtlas);
			texU1 = (float)AtlasUV.u1;
			float texU2 = (float)AtlasUV.u2;
			float texV1 = (float)AtlasUV.v1;
			float texV2 = (float)AtlasUV.v2;

			for(int count = 0; count < itemCount; ++count) {
				GL11.glPushMatrix();
				if(count > 0) {
					float offsetX = (this.random.nextFloat() * 2.0F - 1.0F) * 0.3F;
					float offsetY = (this.random.nextFloat() * 2.0F - 1.0F) * 0.3F;
					float offsetZ = (this.random.nextFloat() * 2.0F - 1.0F) * 0.3F;
					GL11.glTranslatef(offsetX, offsetY, offsetZ);
				}

				GL11.glRotatef(180.0F - this.renderManager.playerViewY, 0.0F, 1.0F, 0.0F);
				GL11.glColor3f(tr, tg, tb);
				tessellator.startDrawingQuads();
				Tessellator.setNormal(0.0F, 1.0F, 0.0F);
				tessellator.addVertexWithUV(-0.5D, -0.25D, 0.0D, (double)texU1, (double)texV2);
				tessellator.addVertexWithUV(0.5D, -0.25D, 0.0D, (double)texU2, (double)texV2);
				tessellator.addVertexWithUV(0.5D, 0.75D, 0.0D, (double)texU2, (double)texV1);
				tessellator.addVertexWithUV(-0.5D, 0.75D, 0.0D, (double)texU1, (double)texV1);
				tessellator.draw();
				GL11.glPopMatrix();
			}
			GL11.glColor3f(1.0F, 1.0F, 1.0F);
		}

		GL11.glDisable(GL11.GL_NORMALIZE);
		GL11.glPopMatrix();
	}
}