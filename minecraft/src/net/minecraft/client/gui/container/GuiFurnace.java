package net.minecraft.client.gui.container;

import net.minecraft.client.render.RenderEngine;
import net.minecraft.game.entity.player.InventoryPlayer;
import net.minecraft.game.world.block.tileentity.TileEntityFurnace;
import org.lwjgl.opengl.GL11;

/** The furnace GUI: shows the fuel/input/result slots plus progress and burning indicators. */
public final class GuiFurnace extends GuiContainer {
	private TileEntityFurnace furnaceInventory;

	public GuiFurnace(InventoryPlayer inventoryPlayer, TileEntityFurnace furnace) {
		new InventoryCraftResult();
		this.furnaceInventory = furnace;
		this.inventorySlots.add(new Slot(this, furnace, 0, 56, 17));
		this.inventorySlots.add(new Slot(this, furnace, 1, 56, 53));
		this.inventorySlots.add(new Slot(this, furnace, 2, 116, 35));

		int row;
		for(row = 0; row < 3; ++row) {
			for(int column = 0; column < 9; ++column) {
				this.inventorySlots.add(new Slot(this, inventoryPlayer, column + (row + 1) * 9, 8 + column * 18, 84 + row * 18));
			}
		}

		for(row = 0; row < 9; ++row) {
			this.inventorySlots.add(new Slot(this, inventoryPlayer, row, 8 + row * 18, 142));
		}

	}

	/** Draws the "Furnace" and "Inventory" labels. */
	protected final void drawGuiContainerForegroundLayer() {
		this.fontRenderer.drawString("Furnace", 60, 6, 4210752);
		this.fontRenderer.drawString("Inventory", 8, this.ySize - 96 + 2, 4210752);
	}

	/** Draws the furnace background, the flame while burning and the cook progress arrow. */
	protected final void drawGuiContainerBackgroundLayer() {
		int texture = this.mc.renderEngine.getTexture("/gui/furnace.png");
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		RenderEngine.bindTexture(texture);
		int guiLeft = (this.width - this.xSize) / 2;
		int guiTop = (this.height - this.ySize) / 2;
		this.drawTexturedModalRect(guiLeft, guiTop, 0, 0, this.xSize, this.ySize);
		int flameHeight;
		// Draw the flame, scaled in height as fuel is consumed.
		if(this.furnaceInventory.isBurning()) {
			flameHeight = this.furnaceInventory.getBurnTimeRemainingScaled(12);
			this.drawTexturedModalRect(guiLeft + 56, guiTop + 36 + 12 - flameHeight, 176, 12 - flameHeight, 14, flameHeight + 2);
		}

		// Draw the cook-progress arrow, scaled in width as the item cooks.
		flameHeight = this.furnaceInventory.getCookProgressScaled(24);
		this.drawTexturedModalRect(guiLeft + 79, guiTop + 34, 176, 14, flameHeight + 1, 16);
	}
}
