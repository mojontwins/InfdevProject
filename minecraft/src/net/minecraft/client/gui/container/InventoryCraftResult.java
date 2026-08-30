package net.minecraft.client.gui.container;

import net.minecraft.game.IInventory;
import net.minecraft.game.item.ItemStack;

/** A one-slot inventory holding the result of a crafting recipe. */
public final class InventoryCraftResult implements IInventory {
	private ItemStack[] stackResult = new ItemStack[1];

	public final int getInventorySize() {
		return 1;
	}

	public final ItemStack getStackInSlot(int slotIndex) {
		return this.stackResult[slotIndex];
	}

	public final String getInvName() {
		return "Result";
	}

	/** Removes and returns the whole crafted result, clearing the slot. */
	public final ItemStack decrStackSize(int slotIndex, int count) {
		if(this.stackResult[slotIndex] != null) {
			ItemStack result = this.stackResult[slotIndex];
			this.stackResult[slotIndex] = null;
			return result;
		} else {
			return null;
		}
	}

	public final void setInventorySlotContents(int slotIndex, ItemStack stack) {
		this.stackResult[slotIndex] = stack;
	}

	public final int getInventoryStackLimit() {
		return 64;
	}

	public final void onInventoryChanged() {
	}
}
