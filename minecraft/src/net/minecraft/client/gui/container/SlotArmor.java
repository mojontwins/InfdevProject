package net.minecraft.client.gui.container;

import net.minecraft.game.IInventory;
import net.minecraft.game.item.ItemArmor;
import net.minecraft.game.item.ItemStack;

/** An armour slot that only accepts items matching the armour piece type, and draws the appropriate placeholder icon. */
final class SlotArmor extends Slot {
	private int armorType;

	SlotArmor(GuiInventory parent, GuiContainer guiHandler, IInventory inventory, int slotIndex, int xPos, int yPos, int armorType) {
		super(guiHandler, inventory, slotIndex, 8, yPos);
		this.armorType = armorType;
	}

	public final boolean isItemValid(ItemStack stack) {
		return stack.getItem() instanceof ItemArmor ? ((ItemArmor)stack.getItem()).armorType == this.armorType : false;
	}

	public final int getBackgroundIconIndex() {
		return 15 + (this.armorType << 4);
	}
}
