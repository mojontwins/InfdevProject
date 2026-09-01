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
	/**
	 * Per-stack NBT data. {@code null} when the stack has no extra data; when
	 * non-null it is saved alongside the rest of the stack and read back on
	 * load. Use {@link #getTagCompound} / {@link #setTagCompound} to access
	 * it (the helper {@link #hasTagCompound} reports whether one is set).
	 */
	public NBTTagCompound stackTagCompound;

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
		if(tag.hasKey("tag")) {
			this.stackTagCompound = tag.getCompoundTag("tag");
		}
	}

	/** Removes the given amount from this stack and returns what was split off. */
	public final ItemStack splitStack(int amount) {
		ItemStack dest = new ItemStack(this.itemID, amount, this.itemDamage);
		if(this.stackTagCompound != null) {
			dest.stackTagCompound = (NBTTagCompound) this.stackTagCompound.copy();
		}
		this.stackSize -= amount;
		return dest;
	}

	/** An independent copy of this stack — id, quantity and damage — with no shared state. */
	public final ItemStack copy() {
		ItemStack duplicate = new ItemStack(this.itemID, this.stackSize, this.itemDamage);
		if(this.stackTagCompound != null) {
			duplicate.stackTagCompound = (NBTTagCompound) this.stackTagCompound.copy();
		}
		return duplicate;
	}

	public final Item getItem() {
		return Item.itemsList[this.itemID];
	}

	public final NBTTagCompound writeToNBT(NBTTagCompound tag) {
		tag.setShort("id", (short) this.itemID);
		tag.setByte("Count", (byte) this.stackSize);
		tag.setShort("Damage", (short) this.itemDamage);
		if(this.stackTagCompound != null) {
			tag.setCompoundTag("tag", this.stackTagCompound);
		}
		return tag;
	}

	/**
	 * Reads the stack fields out of {@code tag} into this stack. Equivalent to
	 * the {@link #ItemStack(NBTTagCompound)} constructor, but in-place: useful
	 * when an {@code ItemStack} is being deserialised from a list and the
	 * caller already holds the empty instance.
	 */
	public final void readFromNBT(NBTTagCompound tag) {
		this.itemID = tag.getShort("id");
		this.stackSize = tag.getByte("Count");
		this.itemDamage = tag.getShort("Damage");
		if(tag.hasKey("tag")) {
			this.stackTagCompound = tag.getCompoundTag("tag");
		}
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

	/** True when this stack carries an extra NBT payload. */
	public final boolean hasTagCompound() {
		return this.stackTagCompound != null;
	}

	/** Returns the NBT payload, or {@code null} if none is set. */
	public final NBTTagCompound getTagCompound() {
		return this.stackTagCompound;
	}

	/** Sets or clears the NBT payload. */
	public final void setTagCompound(NBTTagCompound tag) {
		this.stackTagCompound = tag;
	}

	public final String toString() {
		return "ItemStack[item=" + this.itemID + ", size=" + this.stackSize + ", damage=" + this.itemDamage
				+ (this.stackTagCompound != null ? ", tag=" + this.stackTagCompound : "") + "]";
	}
}