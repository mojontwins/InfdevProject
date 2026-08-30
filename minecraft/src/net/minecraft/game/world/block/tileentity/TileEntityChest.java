package net.minecraft.game.world.block.tileentity;

import com.mojang.nbt.NBTTagCompound;
import com.mojang.nbt.NBTTagList;
import net.minecraft.game.IInventory;
import net.minecraft.game.item.ItemStack;

public class TileEntityChest extends TileEntity implements IInventory {
	// The backing array is sized 36 while the chest only exposes 27 slots, and
	// NBT reads even re-size it to 27 - a harmless over-allocated field.
	private ItemStack[] chestContents = new ItemStack[36];

	@Override
	public final int getInventorySize() {
		return 27;
	}

	@Override
	public final ItemStack getStackInSlot(int slotIndex) {
		return this.chestContents[slotIndex];
	}

	@Override
	public final ItemStack decrStackSize(int slotIndex, int amount) {
		if(this.chestContents[slotIndex] != null) {
			ItemStack splitStack;
			if(this.chestContents[slotIndex].stackSize <= amount) {
				splitStack = this.chestContents[slotIndex];
				this.chestContents[slotIndex] = null;
				return splitStack;
			} else {
				splitStack = this.chestContents[slotIndex].splitStack(amount);
				if(this.chestContents[slotIndex].stackSize == 0) {
					this.chestContents[slotIndex] = null;
				}
				return splitStack;
			}
		} else {
			return null;
		}
	}

	@Override
	public final void setInventorySlotContents(int slotIndex, ItemStack itemStack) {
		this.chestContents[slotIndex] = itemStack;
		if(itemStack != null && itemStack.stackSize > 64) {
			itemStack.stackSize = 64;
		}
	}

	@Override
	public final String getInvName() {
		return "Chest";
	}

	@Override
	public final void readFromNBT(NBTTagCompound tag) {
		super.readFromNBT(tag);
		NBTTagList items = tag.getTagList("Items");
		this.chestContents = new ItemStack[27];

		for(int i = 0; i < items.tagCount(); ++i) {
			NBTTagCompound itemTag = (NBTTagCompound)items.tagAt(i);
			int slotIndex = itemTag.getByte("Slot") & 255;
			if(slotIndex >= 0 && slotIndex < this.chestContents.length) {
				this.chestContents[slotIndex] = new ItemStack(itemTag);
			}
		}
	}

	@Override
	public final void writeToNBT(NBTTagCompound tag) {
		super.writeToNBT(tag);
		NBTTagList items = new NBTTagList();

		for(int i = 0; i < this.chestContents.length; ++i) {
			if(this.chestContents[i] != null) {
				NBTTagCompound itemTag = new NBTTagCompound();
				itemTag.setByte("Slot", (byte)i);
				this.chestContents[i].writeToNBT(itemTag);
				items.setTag(itemTag);
			}
		}

		tag.setTag("Items", items);
	}

	@Override
	public final int getInventoryStackLimit() {
		return 64;
	}

	@Override
	public final void onInventoryChanged() {
		this.worldObj.updateTileEntityChunkAndDoNothing(this.xCoord, this.yCoord, this.zCoord);
	}
}