package net.minecraft.game;

import net.minecraft.game.item.ItemStack;

/**
 * Glues two adjacent chest halves into a single virtual inventory so the
 * GUI sees one large 54-slot chest instead of two 27-slot ones.
 *
 * <p>Reads/writes are forwarded to the {@link #upperChest} for slots in its
 * range, and to the {@link #lowerChest} for slots beyond it.  Both halves
 * are notified of any change.
 */
public final class InventoryLargeChest implements IInventory {
    /** Display name for the combined inventory (e.g. "Large Chest"). */
    private String name;
    /** The first half of the chest (slots 0 .. upperChest.getInventorySize() - 1). */
    private IInventory upperChest;
    /** The second half of the chest (slots upperChest.getInventorySize() .. end). */
    private IInventory lowerChest;

    public InventoryLargeChest(String name, IInventory upperChest, IInventory lowerChest) {
        this.name = name;
        this.upperChest = upperChest;
        this.lowerChest = lowerChest;
    }

    public final int getInventorySize() {
        int upperSize = this.upperChest.getInventorySize();
        return upperSize + this.lowerChest.getInventorySize();
    }

    public final String getInvName() {
        return this.name;
    }

    public final ItemStack getStackInSlot(int slot) {
        int upperSize = this.upperChest.getInventorySize();
        return slot >= upperSize
            ? this.lowerChest.getStackInSlot(slot - upperSize)
            : this.upperChest.getStackInSlot(slot);
    }

    public final ItemStack decrStackSize(int slot, int amount) {
        int upperSize = this.upperChest.getInventorySize();
        return slot >= upperSize
            ? this.lowerChest.decrStackSize(slot - upperSize, amount)
            : this.upperChest.decrStackSize(slot, amount);
    }

    public final void setInventorySlotContents(int slot, ItemStack stack) {
        int upperSize = this.upperChest.getInventorySize();
        if (slot >= upperSize) {
            this.lowerChest.setInventorySlotContents(slot - upperSize, stack);
        } else {
            this.upperChest.setInventorySlotContents(slot, stack);
        }
    }

    public final int getInventoryStackLimit() {
        return this.upperChest.getInventoryStackLimit();
    }

    public final void onInventoryChanged() {
        this.upperChest.onInventoryChanged();
        this.lowerChest.onInventoryChanged();
    }
}
