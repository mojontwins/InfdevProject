package net.minecraft.client.gui.container;

import net.minecraft.client.render.RenderEngine;
import net.minecraft.game.IInventory;
import org.lwjgl.opengl.GL11;

/** The chest container GUI: lays out the double inventory (upper chest, lower chest and player inventory) as slots. */
public final class GuiChest extends GuiContainer {
	private IInventory upperChestInventory;
	private IInventory lowerChestInventory;
	private int inventoryRows = 0;

	public GuiChest(IInventory upperInventory, IInventory lowerInventory) {
		this.upperChestInventory = upperInventory;
		this.lowerChestInventory = lowerInventory;
		this.allowUserInput = false;
		this.inventoryRows = lowerInventory.getInventorySize() / 9;
		this.ySize = 114 + this.inventoryRows * 18;
		int extraSlots = (this.inventoryRows - 4) * 18;

		int row;
		int column;
		// The chest's own storage, one row of 9 slots per inventory row.
		for(row = 0; row < this.inventoryRows; ++row) {
			for(column = 0; column < 9; ++column) {
				this.inventorySlots.add(new Slot(this, lowerInventory, column + row * 9, 8 + column * 18, 18 + row * 18));
			}
		}

		// The player's main inventory (3 rows of 9), offset down by the chest size.
		for(row = 0; row < 3; ++row) {
			for(column = 0; column < 9; ++column) {
				this.inventorySlots.add(new Slot(this, upperInventory, column + (row + 1) * 9, 8 + column * 18, 103 + row * 18 + extraSlots));
			}
		}

		// The player's hotbar (1 row of 9).
		for(row = 0; row < 9; ++row) {
			this.inventorySlots.add(new Slot(this, upperInventory, row, 8 + row * 18, extraSlots + 161));
		}

	}

	/** Draws the container titles in the foreground layer. */
	protected final void drawGuiContainerForegroundLayer() {
		this.fontRenderer.drawString(this.lowerChestInventory.getInvName(), 8, 6, 4210752);
		this.fontRenderer.drawString(this.upperChestInventory.getInvName(), 8, this.ySize - 96 + 2, 4210752);
	}

	/** Draws the chest background texture, sized to the number of inventory rows. */
	protected final void drawGuiContainerBackgroundLayer() {
		int texture = this.mc.renderEngine.getTexture("/gui/container.png");
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		RenderEngine.bindTexture(texture);
		int guiLeft = (this.width - this.xSize) / 2;
		int guiTop = (this.height - this.ySize) / 2;
		this.drawTexturedModalRect(guiLeft, guiTop, 0, 0, this.xSize, this.inventoryRows * 18 + 17);
		this.drawTexturedModalRect(guiLeft, guiTop + this.inventoryRows * 18 + 17, 0, 126, this.xSize, 96);
	}
}
