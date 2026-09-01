package net.minecraft.game;

import net.minecraft.game.item.ItemStack;

/**
 * A fixed-size container of {@link ItemStack}s that notifies the
 * system whenever its contents change.  Implemented by chests,
 * furnaces, crafting grids, and the player inventory.
 */
public interface IInventory {
    /** Returns the number of slots in this inventory. */
    int getInventorySize();

    /** Returns the {@link ItemStack} currently in the given slot. */
    ItemStack getStackInSlot(int slot);

    /**
     * Removes up to {@code amount} items from the given slot and
     * returns them as a new stack.  The remainder stays in place.
     */
    ItemStack decrStackSize(int slot, int amount);

    /** Replaces the stack in {@code slot} with {@code stack}. */
    void setInventorySlotContents(int slot, ItemStack stack);

    /** Returns the name shown for this inventory in the GUI. */
    String getInvName();

    /** Maximum stack size a single slot can hold. */
    int getInventoryStackLimit();

    /** Called when the inventory's contents have changed externally. */
    void onInventoryChanged();
}
