package net.minecraft.client.gui.container;

import net.minecraft.game.IInventory;
import net.minecraft.game.item.ItemStack;

/** A single inventory slot used by container GUIs: maps a position in an IInventory to a 2D screen location. */
public class Slot {
	public final int slotIndex;
	public final int xPos;
	public final int yPos;
	public final IInventory inventory;
	private final GuiContainer guiHandler;

	public Slot(GuiContainer guiHandler, IInventory inventory, int slotIndex, int xPos, int yPos) {
		this.guiHandler = guiHandler;
		this.inventory = inventory;
		this.slotIndex = slotIndex;
		this.xPos = xPos;
		this.yPos = yPos;
	}

	/** Returns true if the given screen coordinates fall within (or slightly around) this slot. */
	public final boolean isAtCursorPos(int mouseX, int mouseY) {
		int guiLeft = (this.guiHandler.width - this.guiHandler.xSize) / 2;
		int guiTop = (this.guiHandler.height - this.guiHandler.ySize) / 2;
		// Convert to container-local coordinates by subtracting the GUI origin.
		mouseX -= guiLeft;
		mouseY -= guiTop;
		return mouseX >= this.xPos - 1 && mouseX < this.xPos + 16 + 1 && mouseY >= this.yPos - 1 && mouseY < this.yPos + 16 + 1;
	}

	public void onPickupFromSlot() {
		this.onSlotChanged();
	}

	public boolean isItemValid(ItemStack stack) {
		return true;
	}

	public final void putStack(ItemStack stack) {
		this.inventory.setInventorySlotContents(this.slotIndex, stack);
		this.onSlotChanged();
	}

	public int getBackgroundIconIndex() {
		return -1;
	}

	public final void onSlotChanged() {
		this.inventory.onInventoryChanged();
	}
}
