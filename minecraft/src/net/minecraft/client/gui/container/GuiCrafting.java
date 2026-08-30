package net.minecraft.client.gui.container;

import net.minecraft.client.render.RenderEngine;
import net.minecraft.game.IInventory;
import net.minecraft.game.entity.player.InventoryPlayer;
import net.minecraft.game.item.ItemStack;
import net.minecraft.game.item.recipe.CraftingManager;
import org.lwjgl.opengl.GL11;

/** The workbench crafting GUI: a 3x3 crafting grid, the result slot, and the player inventory. */
public final class GuiCrafting extends GuiContainer {
	private InventoryCrafting inventoryCrafting = new InventoryCrafting(this, 3, 3);
	private IInventory iInventory = new InventoryCraftResult();

	public GuiCrafting(InventoryPlayer inventoryPlayer) {
		this.inventorySlots.add(new SlotCrafting(this, this.inventoryCrafting, this.iInventory, 0, 124, 35));

		int row;
		int column;
		// The 3x3 crafting grid.
		for(row = 0; row < 3; ++row) {
			for(column = 0; column < 3; ++column) {
				this.inventorySlots.add(new Slot(this, this.inventoryCrafting, column + row * 3, 30 + column * 18, 17 + row * 18));
			}
		}

		// The player's main inventory (3 rows of 9).
		for(row = 0; row < 3; ++row) {
			for(column = 0; column < 9; ++column) {
				this.inventorySlots.add(new Slot(this, inventoryPlayer, column + (row + 1) * 9, 8 + column * 18, 84 + row * 18));
			}
		}

		// The player's hotbar (1 row of 9).
		for(row = 0; row < 9; ++row) {
			this.inventorySlots.add(new Slot(this, inventoryPlayer, row, 8 + row * 18, 142));
		}

	}

	/** When closed, drop any crafting grid materials still held onto the ground. */
	public final void onGuiClosed() {
		super.onGuiClosed();

		for(int slotIndex = 0; slotIndex < 9; ++slotIndex) {
			ItemStack stack = this.inventoryCrafting.getStackInSlot(slotIndex);
			if(stack != null) {
				this.mc.thePlayer.dropPlayerItem(stack);
			}
		}

	}

	/** Recomputes the result slot based on the current crafting grid contents. */
	public final void guiCraftingItemsCheck() {
		this.iInventory.setInventorySlotContents(0, CraftingManager.getInstance().findMatchingRecipe(this.inventoryCrafting));
	}

	/** Draws the "Crafting" and "Inventory" labels. */
	protected final void drawGuiContainerForegroundLayer() {
		this.fontRenderer.drawString("Crafting", 28, 6, 4210752);
		this.fontRenderer.drawString("Inventory", 8, this.ySize - 96 + 2, 4210752);
	}

	/** Draws the crafting table background texture. */
	protected final void drawGuiContainerBackgroundLayer() {
		int texture = this.mc.renderEngine.getTexture("/gui/crafting.png");
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		RenderEngine.bindTexture(texture);
		int guiLeft = (this.width - this.xSize) / 2;
		int guiTop = (this.height - this.ySize) / 2;
		this.drawTexturedModalRect(guiLeft, guiTop, 0, 0, this.xSize, this.ySize);
	}
}
