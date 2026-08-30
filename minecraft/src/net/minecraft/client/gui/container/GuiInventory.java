package net.minecraft.client.gui.container;

import net.minecraft.client.RenderHelper;
import net.minecraft.client.render.RenderEngine;
import net.minecraft.client.render.entity.RenderManager;
import net.minecraft.game.IInventory;
import net.minecraft.game.item.ItemStack;
import net.minecraft.game.item.recipe.CraftingManager;
import org.lwjgl.opengl.GL11;

/** The survival inventory GUI: 2x2 crafting grid, result slot, armour slots, main inventory and a player preview. */
public final class GuiInventory extends GuiContainer {
	private InventoryCrafting inventoryCrafting = new InventoryCrafting(this, 2, 2);
	private IInventory iInventory = new InventoryCraftResult();
	private float xSize_lo;
	private float ySize_lo;

	public GuiInventory(IInventory inventoryPlayer) {
		this.allowUserInput = true;
		this.inventorySlots.add(new SlotCrafting(this, this.inventoryCrafting, this.iInventory, 0, 144, 36));

		int row;
		int column;
		// The 2x2 crafting grid.
		for(row = 0; row < 2; ++row) {
			for(column = 0; column < 2; ++column) {
				this.inventorySlots.add(new Slot(this, this.inventoryCrafting, column + (row << 1), 88 + column * 18, 26 + row * 18));
			}
		}

		// The four armour slots, one per armour type, down the left edge.
		for(row = 0; row < 4; ++row) {
			this.inventorySlots.add(new SlotArmor(this, this, inventoryPlayer, inventoryPlayer.getInventorySize() - 1 - row, 8, 8 + row * 18, row));
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

	/** When closed, drop any crafting grid materials back into the world. */
	public final void onGuiClosed() {
		super.onGuiClosed();

		for(int slotIndex = 0; slotIndex < this.inventoryCrafting.getInventorySize(); ++slotIndex) {
			ItemStack stack = this.inventoryCrafting.getStackInSlot(slotIndex);
			if(stack != null) {
				this.mc.thePlayer.dropPlayerItem(stack);
			}
		}

	}

	/** Recomputes the crafting result based on the current 2x2 grid. */
	public final void guiCraftingItemsCheck() {
		this.iInventory.setInventorySlotContents(0, CraftingManager.getInstance().findMatchingRecipe(this.inventoryCrafting));
	}

	/** Draws the "Crafting" label in the foreground. */
	protected final void drawGuiContainerForegroundLayer() {
		this.fontRenderer.drawString("Crafting", 86, 16, 4210752);
	}

	/** Stores the raw mouse coordinates (in the unscaled GUI space) for the 3D player preview. */
	public final void drawScreen(int mouseX, int mouseY, float partialTicks) {
		super.drawScreen(mouseX, mouseY, partialTicks);
		this.xSize_lo = (float)mouseX;
		this.ySize_lo = (float)mouseY;
	}

	/** Draws the inventory background and the rotating 3D preview of the player. */
	protected final void drawGuiContainerBackgroundLayer() {
		int texture = this.mc.renderEngine.getTexture("/gui/inventory.png");
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		RenderEngine.bindTexture(texture);
		int guiLeft = (this.width - this.xSize) / 2;
		int guiTop = (this.height - this.ySize) / 2;
		this.drawTexturedModalRect(guiLeft, guiTop, 0, 0, this.xSize, this.ySize);
		GL11.glEnable(GL11.GL_NORMALIZE);
		GL11.glEnable(GL11.GL_COLOR_MATERIAL);
		GL11.glPushMatrix();
		// Position the 3D player model in the centre of the inventory window.
		GL11.glTranslatef((float)(guiLeft + 51), (float)(guiTop + 75), 50.0F);
		GL11.glScalef(-30.0F, 30.0F, 30.0F);
		GL11.glRotatef(180.0F, 0.0F, 0.0F, 1.0F);
		float savedYawOffset = this.mc.thePlayer.renderYawOffset;
		float savedYaw = this.mc.thePlayer.rotationYaw;
		float savedPitch = this.mc.thePlayer.rotationPitch;
		// Face the model toward the cursor so it appears to look at the mouse.
		float mouseOffsetX = (float)(guiLeft + 51) - this.xSize_lo;
		float mouseOffsetY = (float)(guiTop + 75 - 50) - this.ySize_lo;
		GL11.glRotatef(135.0F, 0.0F, 1.0F, 0.0F);
		RenderHelper.enableStandardItemLighting();
		GL11.glRotatef(-135.0F, 0.0F, 1.0F, 0.0F);
		GL11.glRotatef(-((float)Math.atan((double)(mouseOffsetY / 40.0F))) * 20.0F, 1.0F, 0.0F, 0.0F);
		this.mc.thePlayer.renderYawOffset = (float)Math.atan((double)(mouseOffsetX / 40.0F)) * 20.0F;
		this.mc.thePlayer.rotationYaw = (float)Math.atan((double)(mouseOffsetX / 40.0F)) * 40.0F;
		this.mc.thePlayer.rotationPitch = -((float)Math.atan((double)(mouseOffsetY / 40.0F))) * 20.0F;
		GL11.glTranslatef(0.0F, this.mc.thePlayer.yOffset, 0.0F);
		RenderManager.instance.renderEntityWithPosYaw(this.mc.thePlayer, 0.0D, 0.0D, 0.0D, 0.0F, 1.0F);
		// Restore the player's real orientation once the preview is drawn.
		this.mc.thePlayer.renderYawOffset = savedYawOffset;
		this.mc.thePlayer.rotationYaw = savedYaw;
		this.mc.thePlayer.rotationPitch = savedPitch;
		GL11.glPopMatrix();
		RenderHelper.disableStandardItemLighting();
		GL11.glDisable(GL11.GL_NORMALIZE);
	}
}
