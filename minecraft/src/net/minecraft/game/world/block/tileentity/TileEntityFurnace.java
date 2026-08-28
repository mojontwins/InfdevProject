package net.minecraft.game.world.block.tileentity;

import com.mojang.nbt.NBTTagCompound;
import com.mojang.nbt.NBTTagList;
import net.minecraft.game.IInventory;
import net.minecraft.game.item.Item;
import net.minecraft.game.item.ItemStack;
import net.minecraft.game.world.block.Block;
import net.minecraft.game.world.material.Material;

public class TileEntityFurnace extends TileEntity implements IInventory {
	private ItemStack[] furnaceItemStacks = new ItemStack[3];
	private int furnaceBurnTime = 0;
	private int currentItemBurnTime = 0;
	private int furnaceCookTime = 0;

	@Override
	public final int getInventorySize() {
		return this.furnaceItemStacks.length;
	}

	@Override
	public final ItemStack getStackInSlot(int slotIndex) {
		return this.furnaceItemStacks[slotIndex];
	}

	@Override
	public final ItemStack decrStackSize(int slotIndex, int amount) {
		if(this.furnaceItemStacks[slotIndex] != null) {
			ItemStack splitStack;
			if(this.furnaceItemStacks[slotIndex].stackSize <= amount) {
				splitStack = this.furnaceItemStacks[slotIndex];
				this.furnaceItemStacks[slotIndex] = null;
				return splitStack;
			} else {
				splitStack = this.furnaceItemStacks[slotIndex].splitStack(amount);
				if(this.furnaceItemStacks[slotIndex].stackSize == 0) {
					this.furnaceItemStacks[slotIndex] = null;
				}
				return splitStack;
			}
		} else {
			return null;
		}
	}

	@Override
	public final void setInventorySlotContents(int slotIndex, ItemStack itemStack) {
		this.furnaceItemStacks[slotIndex] = itemStack;
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
		this.furnaceItemStacks = new ItemStack[this.furnaceItemStacks.length];

		for(int i = 0; i < items.tagCount(); ++i) {
			NBTTagCompound itemTag = (NBTTagCompound)items.tagAt(i);
			byte slotIndex = itemTag.getByte("Slot");
			if(slotIndex >= 0 && slotIndex < this.furnaceItemStacks.length) {
				this.furnaceItemStacks[slotIndex] = new ItemStack(itemTag);
			}
		}

		this.furnaceBurnTime = tag.getShort("BurnTime");
		this.furnaceCookTime = tag.getShort("CookTime");
		this.currentItemBurnTime = getItemBurnTime(this.furnaceItemStacks[1]);
		System.out.println("Lit: " + this.furnaceBurnTime + "/" + this.currentItemBurnTime);
	}

	@Override
	public final void writeToNBT(NBTTagCompound tag) {
		super.writeToNBT(tag);
		tag.setShort("BurnTime", (short)this.furnaceBurnTime);
		tag.setShort("CookTime", (short)this.furnaceCookTime);
		NBTTagList items = new NBTTagList();

		for(int i = 0; i < this.furnaceItemStacks.length; ++i) {
			if(this.furnaceItemStacks[i] != null) {
				NBTTagCompound itemTag = new NBTTagCompound();
				itemTag.setByte("Slot", (byte)i);
				this.furnaceItemStacks[i].writeToNBT(itemTag);
				items.setTag(itemTag);
			}
		}

		tag.setTag("Items", items);
	}

	@Override
	public final int getInventoryStackLimit() {
		return 64;
	}

	public final int getCookProgressScaled(int scale) {
		return this.furnaceCookTime * 24 / 200;
	}

	public final int getBurnTimeRemainingScaled(int scale) {
		return this.furnaceBurnTime * 12 / this.currentItemBurnTime;
	}

	public final boolean isBurning() {
		return this.furnaceBurnTime > 0;
	}

	@Override
	public final void updateEntity() {
		boolean wasBurning = this.furnaceBurnTime > 0;
		if(this.furnaceBurnTime > 0) {
			--this.furnaceBurnTime;
		}

		if(this.furnaceBurnTime == 0 && this.canSmelt()) {
			this.currentItemBurnTime = this.furnaceBurnTime = getItemBurnTime(this.furnaceItemStacks[1]);
			if(this.furnaceBurnTime > 0 && this.furnaceItemStacks[1] != null) {
				--this.furnaceItemStacks[1].stackSize;
				if(this.furnaceItemStacks[1].stackSize == 0) {
					this.furnaceItemStacks[1] = null;
				}
			}
		}

		if(this.isBurning() && this.canSmelt()) {
			++this.furnaceCookTime;
			if(this.furnaceCookTime == 200) {
				this.furnaceCookTime = 0;
				if(this.canSmelt()) {
					int smeltedItemID = smeltItem(this.furnaceItemStacks[0].getItem().shiftedIndex);
					if(this.furnaceItemStacks[2] == null) {
						this.furnaceItemStacks[2] = new ItemStack(smeltedItemID, 1);
					} else if(this.furnaceItemStacks[2].itemID == smeltedItemID) {
						++this.furnaceItemStacks[2].stackSize;
					}

					--this.furnaceItemStacks[0].stackSize;
					if(this.furnaceItemStacks[0].stackSize <= 0) {
						this.furnaceItemStacks[0] = null;
					}
				}
			}
		} else {
			this.furnaceCookTime = 0;
		}

		if(wasBurning != this.isBurning()) {
			boolean burning = this.isBurning();
			int metadata = this.worldObj.getBlockMetadata(this.xCoord, this.yCoord, this.zCoord);
			TileEntity tileEntity = this.worldObj.getBlockTileEntity(this.xCoord, this.yCoord, this.zCoord);
			if(burning) {
				this.worldObj.setBlockWithNotify(this.xCoord, this.yCoord, this.zCoord, Block.stoneOvenActive.blockID);
			} else {
				this.worldObj.setBlockWithNotify(this.xCoord, this.yCoord, this.zCoord, Block.stoneOvenIdle.blockID);
			}
			this.worldObj.setBlockMetadataWithNotify(this.xCoord, this.yCoord, this.zCoord, metadata);
			this.worldObj.setBlockTileEntity(this.xCoord, this.yCoord, this.zCoord, tileEntity);
		}

		this.worldObj.updateTileEntityChunkAndDoNothing(this.xCoord, this.yCoord, this.zCoord);
	}

	private boolean canSmelt() {
		if(this.furnaceItemStacks[0] == null) {
			return false;
		} else {
			int smeltedItemID = smeltItem(this.furnaceItemStacks[0].getItem().shiftedIndex);
			if(smeltedItemID < 0) {
				return false;
			} else if(this.furnaceItemStacks[2] == null) {
				return true;
			} else if(this.furnaceItemStacks[2].itemID != smeltedItemID) {
				return false;
			} else {
				if(this.furnaceItemStacks[2].stackSize < 64) {
					ItemStack outputStack = this.furnaceItemStacks[2];
					if(this.furnaceItemStacks[2].stackSize < outputStack.getItem().getItemStackLimit()) {
						return true;
					}
				}
				return this.furnaceItemStacks[2].stackSize < Item.itemsList[smeltedItemID].getItemStackLimit();
			}
		}
	}

	private static int smeltItem(int itemID) {
		return itemID == Block.oreIron.blockID ? Item.ingotIron.shiftedIndex : (itemID == Block.oreGold.blockID ? Item.ingotGold.shiftedIndex : (itemID == Block.oreDiamond.blockID ? Item.diamod.shiftedIndex : (itemID == Block.sand.blockID ? Block.glass.blockID : (itemID == Item.porkRaw.shiftedIndex ? Item.porkCooked.shiftedIndex : (itemID == Block.cobblestone.blockID ? Block.stone.blockID : -1)))));
	}

	private static int getItemBurnTime(ItemStack itemStack) {
		if(itemStack == null) {
			return 0;
		} else {
			int shiftedIndex = itemStack.getItem().shiftedIndex;
			return shiftedIndex < 256 && Block.blocksList[shiftedIndex].blockMaterial == Material.wood ? 300 : (shiftedIndex == Item.stick.shiftedIndex ? 100 : (shiftedIndex == Item.coal.shiftedIndex ? 1600 : 0));
		}
	}

	@Override
	public final void onInventoryChanged() {
		this.worldObj.updateTileEntityChunkAndDoNothing(this.xCoord, this.yCoord, this.zCoord);
	}
}