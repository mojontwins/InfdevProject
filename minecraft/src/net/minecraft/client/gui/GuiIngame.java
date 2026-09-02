package net.minecraft.client.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.client.ChatLine;
import net.minecraft.client.Minecraft;
import net.minecraft.client.RenderHelper;
import net.minecraft.client.render.entity.RenderItem;
import net.minecraft.game.entity.player.InventoryPlayer;
import net.minecraft.game.item.ItemStack;
import org.lwjgl.opengl.GL11;
import util.MathHelper;

/** The in-game HUD overlay: draws the hotbar, hearts/armour, air, chat messages and debug info. */
public final class GuiIngame extends Gui {
	private static RenderItem itemRenderer = new RenderItem();
	private List<ChatLine> chatMessageList = new ArrayList<>();
	private Random rand = new Random();
	private Minecraft mc;
	private int updateCounter = 0;

	public GuiIngame(Minecraft minecraft) {
		this.mc = minecraft;
	}

	/** Renders the entire heads-up display for the given partial-tick time. */
	public final void renderGameOverlay(float partialTicks) {
		ScaledResolution scaledResolution = new ScaledResolution(this.mc.displayWidth, this.mc.displayHeight);
		int scaledWidth = scaledResolution.getScaledWidth();
		int scaledHeight = scaledResolution.getScaledHeight();
		FontRenderer fontRenderer = this.mc.fontRenderer;
		this.mc.entityRenderer.setupOverlayRendering();
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, this.mc.renderEngine.getTexture("/gui/gui.png"));
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		GL11.glEnable(GL11.GL_BLEND);
		InventoryPlayer inventory = this.mc.thePlayer.inventory;
		this.zLevel = -90.0F;
		// Draw the hotbar background and the selection frame around the current item.
		this.drawTexturedModalRect(scaledWidth / 2 - 91, scaledHeight - 22, 0, 0, 182, 22);
		this.drawTexturedModalRect(scaledWidth / 2 - 91 - 1 + inventory.currentItem * 20, scaledHeight - 22 - 1, 0, 22, 24, 22);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, this.mc.renderEngine.getTexture("/gui/icons.png"));
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_ONE_MINUS_DST_COLOR, GL11.GL_ONE_MINUS_SRC_COLOR);
		// Draw the crosshair in the centre of the screen.
		this.drawTexturedModalRect(scaledWidth / 2 - 7, scaledHeight / 2 - 7, 0, 0, 16, 16);
		GL11.glDisable(GL11.GL_BLEND);
		boolean blinkHearts = this.mc.thePlayer.heartsLife / 3 % 2 == 1;
		if(this.mc.thePlayer.heartsLife < 10) {
			blinkHearts = false;
		}

		int health = this.mc.thePlayer.health;
		int prevHealth = this.mc.thePlayer.prevHealth;
		// Seed the random so the "low health" wobble is deterministic per frame.
		this.rand.setSeed((long)(this.updateCounter * 312871));
		int armourBarIndex;
		int heartIndex;
		if(this.mc.playerController.shouldDrawHUD()) {
			int armorValue = this.mc.thePlayer.inventory.getTotalArmorValue();
			int heartY;
			int slotIndex;
			for(slotIndex = 0; slotIndex < 10; ++slotIndex) {
				heartY = scaledHeight - 32;
				// Draw one of the three armour icons depending on how much armour
				// this heart's "half" falls within.
				if(armorValue > 0) {
					armourBarIndex = scaledWidth / 2 + 91 - (slotIndex << 3) - 9;
					if((slotIndex << 1) + 1 < armorValue) {
						this.drawTexturedModalRect(armourBarIndex, heartY, 34, 9, 9, 9);
					}

					if((slotIndex << 1) + 1 == armorValue) {
						this.drawTexturedModalRect(armourBarIndex, heartY, 25, 9, 9, 9);
					}

					if((slotIndex << 1) + 1 > armorValue) {
						this.drawTexturedModalRect(armourBarIndex, heartY, 16, 9, 9, 9);
					}
				}

				byte heartTexture = 0;
				if(blinkHearts) {
					heartTexture = 1;
				}

				int heartX = scaledWidth / 2 - 91 + (slotIndex << 3);
				if(health <= 4) {
					heartY += this.rand.nextInt(2);
				}

				// Draw the empty/background heart, then the falling damage overlay and the filled heart.
				this.drawTexturedModalRect(heartX, heartY, 16 + heartTexture * 9, 0, 9, 9);
				if(blinkHearts) {
					if((slotIndex << 1) + 1 < prevHealth) {
						this.drawTexturedModalRect(heartX, heartY, 70, 0, 9, 9);
					}

					if((slotIndex << 1) + 1 == prevHealth) {
						this.drawTexturedModalRect(heartX, heartY, 79, 0, 9, 9);
					}
				}

				if((slotIndex << 1) + 1 < health) {
					this.drawTexturedModalRect(heartX, heartY, 52, 0, 9, 9);
				}

				if((slotIndex << 1) + 1 == health) {
					this.drawTexturedModalRect(heartX, heartY, 61, 0, 9, 9);
				}
			}

			if(this.mc.thePlayer.isInsideOfMaterial()) {
				// Draw the air bubbles while the player is underwater: full then empty.
				slotIndex = (int)Math.ceil((double)(this.mc.thePlayer.air - 2) * 10.0D / 300.0D);
				heartY = (int)Math.ceil((double)this.mc.thePlayer.air * 10.0D / 300.0D) - slotIndex;

				for(armourBarIndex = 0; armourBarIndex < slotIndex + heartY; ++armourBarIndex) {
					if(armourBarIndex < slotIndex) {
						this.drawTexturedModalRect(scaledWidth / 2 - 91 + (armourBarIndex << 3), scaledHeight - 32 - 9, 16, 18, 9, 9);
					} else {
						this.drawTexturedModalRect(scaledWidth / 2 - 91 + (armourBarIndex << 3), scaledHeight - 32 - 9, 25, 18, 9, 9);
					}
				}
			}
		}

		GL11.glDisable(GL11.GL_BLEND);
		GL11.glEnable(GL11.GL_NORMALIZE);
		GL11.glPushMatrix();
		GL11.glRotatef(180.0F, 1.0F, 0.0F, 0.0F);
		RenderHelper.enableStandardItemLighting();
		GL11.glPopMatrix();

		// Draw each hotbar item, applying the "swing" scaling animation when used.
		for(armourBarIndex = 0; armourBarIndex < 9; ++armourBarIndex) {
			int itemX = scaledWidth / 2 - 90 + armourBarIndex * 20 + 2;
			int itemY = scaledHeight - 16 - 3;

			ItemStack stack = this.mc.thePlayer.inventory.mainInventory[armourBarIndex];
			if(stack != null) {
				float swingProgress = (float)stack.animationsToGo - partialTicks;
				if(swingProgress > 0.0F) {
					GL11.glPushMatrix();
					float scale = 1.0F + swingProgress / 5.0F;
					GL11.glTranslatef((float)(itemX + 8), (float)(itemY + 12), 0.0F);
					GL11.glScalef(1.0F / scale, (scale + 1.0F) / 2.0F, 1.0F);
					GL11.glTranslatef((float)(-(itemX + 8)), (float)(-(itemY + 12)), 0.0F);
				}

				itemRenderer.renderItemIntoGUI(this.mc.renderEngine, stack, itemX, itemY);
				if(swingProgress > 0.0F) {
					GL11.glPopMatrix();
				}

				itemRenderer.renderItemOverlayIntoGUI(this.mc.fontRenderer, stack, itemX, itemY);
			}
		}

		RenderHelper.disableStandardItemLighting();
		GL11.glDisable(GL11.GL_NORMALIZE);
		if(this.mc.gameSettings.showFPS) {
			// Categorise all loaded entities into animals, monsters and everything else.
			int animalCount = this.mc.theWorld.getAnimalCount();
			int monsterCount = this.mc.theWorld.getMonsterCount();
			int totalCount = this.mc.theWorld.getLoadedEntityList().size();
			int otherCount = totalCount - animalCount - monsterCount;

			// Work out the compass direction the player is facing from the yaw,
			// then show that plus the integer yaw, e.g. "Pos: 100 64 300 (W 273)".
			int facing = MathHelper.floor_double((double)(this.mc.thePlayer.rotationYaw * 4.0F / 360.0F) + 0.5D) & 3;
			String facingName = facing == 0 ? "S" : (facing == 1 ? "W" : (facing == 2 ? "N" : "E"));

			// In-game time as a 24-hour clock: a full day is 24000 ticks, with the
			// +6000 offset so dawn falls at 06:00, e.g. "Time: 13:45".
			long adjustedTime = (this.mc.theWorld.worldTime + 6000L) % 24000L;
			String timeText = String.format("%02d:%02d", adjustedTime / 1000L, (adjustedTime % 1000L) * 60L / 1000L);

			fontRenderer.drawStringWithShadow("Minecraft Infdev (" + this.mc.debug + ")", 2, 2, 16777215);
			fontRenderer.drawStringWithShadow("Entities: A:" + animalCount + " | M:" + monsterCount + " | O:" + otherCount + " | T:" + totalCount, 2, 12, 16777215);
			fontRenderer.drawStringWithShadow("Pos: " + (int)this.mc.thePlayer.posX + " " + (int)this.mc.thePlayer.posY + " " + (int)this.mc.thePlayer.posZ + " (" + facingName + " " + (int)this.mc.thePlayer.rotationYaw + ")", 2, 22, 16777215);
			fontRenderer.drawStringWithShadow("Time: " + timeText, 2, 32, 16777215);

			long maxMemory = Runtime.getRuntime().maxMemory();
			long totalMemory = Runtime.getRuntime().totalMemory();
			long freeMemory = Runtime.getRuntime().freeMemory();
			long usedMemory = totalMemory - freeMemory;
			String usedText = "Used: " + usedMemory * 100L / maxMemory + "% (" + usedMemory / 1024L / 1024L + "MB) of " + maxMemory / 1024L / 1024L + "MB";
			drawString(fontRenderer, usedText, scaledWidth - fontRenderer.getStringWidth(usedText) - 2, 2, 14737632);
			String seedText = "Seed: " + this.mc.theWorld.getSeed();
			drawString(fontRenderer, seedText, scaledWidth - fontRenderer.getStringWidth(seedText) - 2, 12, 14737632);
		} else {
			fontRenderer.drawStringWithShadow("Minecraft Infdev", 2, 2, 16777215);
		}

		// Draw up to the 10 most recent chat lines near the bottom of the screen.
		for(heartIndex = 0; heartIndex < this.chatMessageList.size() && heartIndex < 10; ++heartIndex) {
			if(this.chatMessageList.get(heartIndex).updateCounter < 200) {
				fontRenderer.drawStringWithShadow(this.chatMessageList.get(heartIndex).message, 2, scaledHeight - 8 - heartIndex * 9 - 20, 16777215);
			}
		}

	}

	/** Advances the tick/update counter and ages each chat message. */
	public final void updateTick() {
		++this.updateCounter;

		for(int index = 0; index < this.chatMessageList.size(); ++index) {
			++this.chatMessageList.get(index).updateCounter;
		}

	}
}
