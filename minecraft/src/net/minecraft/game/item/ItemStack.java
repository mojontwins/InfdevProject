package net.minecraft.game.item;

import com.mojang.nbt.NBTTagCompound;
import net.minecraft.game.world.block.Block;

/**
 * A handful of a thing: the quantity and (for tools, armour and damaged goods)
 * the remaining durability of one "slot" of inventory content. It wraps the
 * numeric item id, converts back and forth to the block/item classes, adapts its
 * damage with {@link #damageItem}, splits in half with {@link #splitStack}, and
 * is the unit serialized into save files.
 */
public final class ItemStack {
	public int stackSize;
	public int animationsToGo;
	public int itemID;
	public int itemDamage;

	public ItemStack(Block block) {
		this(block, 1);
	}

	public ItemStack(Block block, int stackSize) {
		this(block.blockID, stackSize);
	}

	public ItemStack(Item item) {
		this(item, 1);
	}

	public ItemStack(Item item, int stackSize) {
		this(item.shiftedIndex, stackSize);
	}

	public ItemStack(int itemID) {
		this(itemID, 1);
	}

	public ItemStack(int itemID, int stackSize) {
		this.itemID = itemID;
		this.stackSize = stackSize;
	}

	public ItemStack(int itemID, int stackSize, int itemDamage) {
		this.itemID = itemID;
		this.stackSize = stackSize;
		this.itemDamage = itemDamage;
	}

	public ItemStack(NBTTagCompound tag) {
		this.itemID = tag.getShort("id");
		this.stackSize = tag.getByte("Count");
		this.itemDamage = tag.getShort("Damage");
	}

	/** Removes the given amount from this stack and returns what was split off. */
	public final ItemStack splitStack(int amount) {
		this.stackSize -= amount;
		return new ItemStack(this.itemID, amount, this.itemDamage);
	}

	public final Item getItem() {
		return Item.itemsList[this.itemID];
	}

	public final NBTTagCompound writeToNBT(NBTTagCompound tag) {
		tag.setShort("id", (short) this.itemID);
		tag.setByte("Count", (byte) this.stackSize);
		tag.setShort("Damage", (short) this.itemDamage);
		return tag;
	}

	public final int getMaxDamage() {
		return Item.itemsList[this.itemID].getMaxDamage();
	}

	/**
	 * Wears this stack down. When the accumulated damage passes the item's
	 * durability one item is destroyed and the damage counter resets.
	 */
	public final void damageItem(int damage) {
		this.itemDamage += damage;
		if (this.itemDamage > this.getMaxDamage()) {
			--this.stackSize;
			if (this.stackSize < 0) {
				this.stackSize = 0;
			}

			this.itemDamage = 0;
		}
	}
}