package net.minecraft.client.gui.container;

import net.minecraft.game.IInventory;
import net.minecraft.game.item.ItemStack;

/** The crafting result slot; it accepts nothing itself and, when the result is taken, consumes the crafting grid. */
final class SlotCrafting extends Slot {
	private final IInventory craftMatrix;

	public SlotCrafting(GuiContainer guiHandler, IInventory craftMatrix, IInventory resultInventory, int slotIndex, int xPos, int yPos) {
		super(guiHandler, resultInventory, 0, xPos, yPos);
		this.craftMatrix = craftMatrix;
	}

	public final boolean isItemValid(ItemStack stack) {
		return false;
	}

	/** Consumes one ingredient from each occupied cell of the crafting matrix when the result is picked up. */
	public final void onPickupFromSlot() {
		for(int slotIndex = 0; slotIndex < this.craftMatrix.getInventorySize(); ++slotIndex) {
			if(this.craftMatrix.getStackInSlot(slotIndex) != null) {
				this.craftMatrix.decrStackSize(slotIndex, 1);
			}
		}

	}
}
