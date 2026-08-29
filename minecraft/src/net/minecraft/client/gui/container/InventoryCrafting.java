package net.minecraft.client.gui.container;

import net.minecraft.game.IInventory;
import net.minecraft.game.item.ItemStack;

/**
 * The craft matrix shared by the crafting GUIs: the small 2×2 one on the
 * survival screen and the full 3×3 of the crafting table. It is a flat grid of
 * slots with a real width and height so the recipe framework
 * ({@link net.minecraft.game.item.recipe.IRecipe}) can address individual cells
 * via {@link #getStackInRowAndColumn(int, int)}. Every mutation of a slot makes
 * the owning GUI re-evaluate which recipe the matrix currently matches.
 */
public final class InventoryCrafting implements IInventory {
	private ItemStack[] stackList;
	private int inventoryWidth;
	private int inventoryHeight;
	private GuiContainer eventHandler;

	public InventoryCrafting(GuiContainer eventHandler, int inventoryWidth, int inventoryHeight) {
		this.inventoryWidth = inventoryWidth;
		this.inventoryHeight = inventoryHeight;
		this.stackList = new ItemStack[inventoryWidth * inventoryHeight];
		this.eventHandler = eventHandler;
	}

	public final int getInventorySize() {
		return this.inventoryWidth * this.inventoryHeight;
	}

	/** Number of columns of the craft matrix. */
	public final int getInventoryWidth() {
		return this.inventoryWidth;
	}

	/** Number of rows of the craft matrix. */
	public final int getInventoryHeight() {
		return this.inventoryHeight;
	}

	public final ItemStack getStackInSlot(int slot) {
		return this.stackList[slot];
	}

	/** The stack in the matrix cell at (column, row), with the slots flattened column-major. */
	public final ItemStack getStackInRowAndColumn(int column, int row) {
		return this.stackList[column + row * this.inventoryWidth];
	}

	public final String getInvName() {
		return "Crafting";
	}

	public final ItemStack decrStackSize(int slot, int amount) {
		if (this.stackList[slot] != null) {
			ItemStack splitStack;
			if (this.stackList[slot].stackSize <= amount) {
				splitStack = this.stackList[slot];
				this.stackList[slot] = null;
				this.eventHandler.guiCraftingItemsCheck();
				return splitStack;
			} else {
				splitStack = this.stackList[slot].splitStack(amount);
				if (this.stackList[slot].stackSize == 0) {
					this.stackList[slot] = null;
				}
				this.eventHandler.guiCraftingItemsCheck();
				return splitStack;
			}
		} else {
			return null;
		}
	}

	public final void setInventorySlotContents(int slot, ItemStack itemStack) {
		this.stackList[slot] = itemStack;
		this.eventHandler.guiCraftingItemsCheck();
	}

	public final int getInventoryStackLimit() {
		return 64;
	}

	public final void onInventoryChanged() {
	}
}