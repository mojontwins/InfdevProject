package net.minecraft.game.entity.player;

import java.util.stream.IntStream;
import net.minecraft.game.IInventory;
import net.minecraft.game.item.ItemArmor;
import net.minecraft.game.item.ItemStack;

/**
 * The player's 36-slot main inventory plus the 4 armor slots (indexed 36..39)
 * layered on top. Slots are plain arrays for the same reason the protoype game
 * keeps them raw: the hotbar and the container screens read and write them
 * directly every frame.
 */
public final class InventoryPlayer implements IInventory {
	public ItemStack[] mainInventory = new ItemStack[36];
	public ItemStack[] armorInventory = new ItemStack[4];
	public int currentItem = 0;
	private EntityPlayer player;

	public InventoryPlayer(EntityPlayer player) {
		this.player = player;
	}

	public final ItemStack getCurrentItem() {
		return this.mainInventory[this.currentItem];
	}

	/** First main-inventory slot holding the given item id, or -1. */
	private int getInventorySlotContainItem(int itemID) {
		return IntStream.range(0, this.mainInventory.length)
				.filter(i -> this.mainInventory[i] != null && this.mainInventory[i].itemID == itemID)
				.findFirst()
				.orElse(-1);
	}

	/** First empty main-inventory slot, or -1. */
	private int storeItemStack() {
		return IntStream.range(0, this.mainInventory.length)
				.filter(i -> this.mainInventory[i] == null)
				.findFirst()
				.orElse(-1);
	}

	/** Selects the hotbar slot (0..8) holding the given item, if any. */
	public final void getFirstEmptyStack(int itemID) {
		itemID = this.getInventorySlotContainItem(itemID);
		if (itemID >= 0 && itemID < 9) {
			this.currentItem = itemID;
		}
	}

	public final boolean consumeInventoryItem(int itemID) {
		itemID = this.getInventorySlotContainItem(itemID);
		if (itemID < 0) {
			return false;
		} else {
			if (--this.mainInventory[itemID].stackSize <= 0) {
				this.mainInventory[itemID] = null;
			}

			return true;
		}
	}

	/**
	 * Tries to stash the given stack. It is first merged into an existing,
	 * non-full stack of the same item; whatever does not fit goes into an empty
	 * slot; and whatever still does not fit is left in the argument stack.
	 */
	public final boolean storePartialItemStack(ItemStack stack) {
		if (stack.itemDamage == 0) {
			int remaining = stack.stackSize;
			int itemID = stack.itemID;

			int slot = IntStream.range(0, this.mainInventory.length)
					.filter(i -> this.mainInventory[i] != null
							&& this.mainInventory[i].itemID == itemID
							&& this.mainInventory[i].stackSize < this.mainInventory[i].getItem().getItemStackLimit()
							&& this.mainInventory[i].stackSize < 64)
					.findFirst()
					.orElse(-1);
			if (slot < 0) {
				slot = this.storeItemStack();
			}

			if (slot >= 0) {
				if (this.mainInventory[slot] == null) {
					this.mainInventory[slot] = new ItemStack(itemID, 0);
				}

				int added = remaining;
				int slotLimit = this.mainInventory[slot].getItem().getItemStackLimit();
				if (added > slotLimit - this.mainInventory[slot].stackSize) {
					added = slotLimit - this.mainInventory[slot].stackSize;
				}

				if (added > 64 - this.mainInventory[slot].stackSize) {
					added = 64 - this.mainInventory[slot].stackSize;
				}

				if (added != 0) {
					remaining -= added;
					this.mainInventory[slot].stackSize += added;
					this.mainInventory[slot].animationsToGo = 5;
				}
			}

			stack.stackSize = remaining;
			if (stack.stackSize == 0) {
				return true;
			}
		}

		int emptySlot = this.storeItemStack();
		if (emptySlot >= 0) {
			this.mainInventory[emptySlot] = stack;
			this.mainInventory[emptySlot].animationsToGo = 5;
			return true;
		} else {
			return false;
		}
	}

	public final ItemStack decrStackSize(int slot, int amount) {
		ItemStack[] inventory = this.mainInventory;
		if (slot >= this.mainInventory.length) {
			inventory = this.armorInventory;
			slot -= this.mainInventory.length;
		}

		if (inventory[slot] != null) {
			ItemStack taken;
			if (inventory[slot].stackSize <= amount) {
				taken = inventory[slot];
				inventory[slot] = null;
				return taken;
			} else {
				taken = inventory[slot].splitStack(amount);
				if (inventory[slot].stackSize == 0) {
					inventory[slot] = null;
				}

				return taken;
			}
		} else {
			return null;
		}
	}

	public final void setInventorySlotContents(int slot, ItemStack stack) {
		ItemStack[] inventory = this.mainInventory;
		if (slot >= this.mainInventory.length) {
			inventory = this.armorInventory;
			slot -= this.mainInventory.length;
		}

		inventory[slot] = stack;
	}

	public final int getInventorySize() {
		return this.mainInventory.length + 4;
	}

	public final ItemStack getStackInSlot(int slot) {
		ItemStack[] inventory = this.mainInventory;
		if (slot >= this.mainInventory.length) {
			inventory = this.armorInventory;
			slot -= this.mainInventory.length;
		}

		return inventory[slot];
	}

	public final String getInvName() {
		return "Inventory";
	}

	public final int getInventoryStackLimit() {
		return 64;
	}

	public final int getPlayerArmorValue() {
		int damageReduceTotal = 0;
		int remainingDurability = 0;
		int totalDurability = 0;

		for (int i = 0; i < this.armorInventory.length; ++i) {
			ItemStack armor = this.armorInventory[i];
			if (armor != null && armor.getItem() instanceof ItemArmor) {
				int maxDamage = armor.getMaxDamage();
				remainingDurability += maxDamage - armor.itemDamage;
				totalDurability += maxDamage;
				damageReduceTotal += ((ItemArmor) armor.getItem()).damageReduceAmount;
			}
		}

		if (totalDurability == 0) {
			return 0;
		} else {
			return (damageReduceTotal - 1) * remainingDurability / totalDurability + 1;
		}
	}

	public final void dropAllItems() {
		int slot;
		for (slot = 0; slot < this.mainInventory.length; ++slot) {
			if (this.mainInventory[slot] != null) {
				this.player.dropPlayerItemWithRandomChoice(this.mainInventory[slot], true);
				this.mainInventory[slot] = null;
			}
		}

		for (slot = 0; slot < this.armorInventory.length; ++slot) {
			if (this.armorInventory[slot] != null) {
				this.player.dropPlayerItemWithRandomChoice(this.armorInventory[slot], true);
				this.armorInventory[slot] = null;
			}
		}
	}

	public final void onInventoryChanged() {
	}
}