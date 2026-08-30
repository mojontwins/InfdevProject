package net.minecraft.client.gui.container;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.RenderHelper;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.player.EntityPlayerSP;
import net.minecraft.client.render.RenderEngine;
import net.minecraft.client.render.entity.RenderItem;
import net.minecraft.game.IInventory;
import net.minecraft.game.item.ItemStack;
import org.lwjgl.opengl.GL11;

/** Base class for container GUIs (chest, crafting, furnace, inventory): manages slots and held-item drag logic. */
public abstract class GuiContainer extends GuiScreen {
	private static RenderItem itemRenderer = new RenderItem();
	private ItemStack itemStack = null;
	protected int xSize = 176;
	protected int ySize = 166;
	protected List<Slot> inventorySlots = new ArrayList<>();

	/** Draws each slot (with its item or placeholder icon), the held stack, and highlights the hovered slot. */
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		this.drawDefaultBackground();
		int guiLeft = (this.width - this.xSize) / 2;
		int guiTop = (this.height - this.ySize) / 2;
		this.drawGuiContainerBackgroundLayer();
		GL11.glPushMatrix();
		GL11.glRotatef(180.0F, 1.0F, 0.0F, 0.0F);
		RenderHelper.enableStandardItemLighting();
		GL11.glPopMatrix();
		GL11.glPushMatrix();
		GL11.glTranslatef((float)guiLeft, (float)guiTop, 0.0F);
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		GL11.glEnable(GL11.GL_NORMALIZE);

		for(int slotIndex = 0; slotIndex < this.inventorySlots.size(); ++slotIndex) {
			Slot slot;
			label24: {
				slot = this.inventorySlots.get(slotIndex);
				IInventory inventory = slot.inventory;
				int stackSlot = slot.slotIndex;
				int slotX = slot.xPos;
				int slotY = slot.yPos;
				ItemStack stack = inventory.getStackInSlot(stackSlot);
				if(stack == null) {
					// Empty slot: draw its placeholder icon (e.g. armour shape) if it has one.
					int backgroundIcon = slot.getBackgroundIconIndex();
					if(backgroundIcon >= 0) {
						GL11.glDisable(GL11.GL_LIGHTING);
						RenderEngine.bindTexture(this.mc.renderEngine.getTexture("/gui/items.png"));
						this.drawTexturedModalRect(slotX, slotY, backgroundIcon % 16 << 4, backgroundIcon / 16 << 4, 16, 16);
						GL11.glEnable(GL11.GL_LIGHTING);
						break label24;
					}
				}

				itemRenderer.renderItemIntoGUI(this.mc.renderEngine, stack, slotX, slotY);
				itemRenderer.renderItemOverlayIntoGUI(this.fontRenderer, stack, slotX, slotY);
			}

			// Highlight the slot currently under the mouse cursor.
			if(slot.isAtCursorPos(mouseX, mouseY)) {
				GL11.glDisable(GL11.GL_LIGHTING);
				GL11.glDisable(GL11.GL_DEPTH_TEST);
				int slotX = slot.xPos;
				int slotY = slot.yPos;
				drawGradientRect(slotX, slotY, slotX + 16, slotY + 16, -2130706433, -2130706433);
				GL11.glEnable(GL11.GL_LIGHTING);
				GL11.glEnable(GL11.GL_DEPTH_TEST);
			}
		}

		// Draw the held stack following the cursor.
		if(this.itemStack != null) {
			GL11.glTranslatef(0.0F, 0.0F, 32.0F);
			itemRenderer.renderItemIntoGUI(this.mc.renderEngine, this.itemStack, mouseX - guiLeft - 8, mouseY - guiTop - 8);
			itemRenderer.renderItemOverlayIntoGUI(this.fontRenderer, this.itemStack, mouseX - guiLeft - 8, mouseY - guiTop - 8);
		}

		GL11.glDisable(GL11.GL_NORMALIZE);
		RenderHelper.disableStandardItemLighting();
		GL11.glDisable(GL11.GL_LIGHTING);
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		this.drawGuiContainerForegroundLayer();
		GL11.glEnable(GL11.GL_LIGHTING);
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glPopMatrix();
	}

	protected void drawGuiContainerForegroundLayer() {
	}

	protected abstract void drawGuiContainerBackgroundLayer();

	/**
	 * Handles left/right mouse clicks on slots: pickup, place, stack and merge
	 * logic for moving item stacks between the cursor and container slots, plus
	 * dropping items when clicking outside the container.
	 */
	protected final void mouseClicked(int mouseX, int mouseY, int button) {
		if(button == 0 || button == 1) {
			int clickY = mouseY;
			int clickX = mouseX;
			int index = 0;
			Slot foundSlot;
			// Find the slot under the cursor, if any.
			while(true) {
				if(index >= this.inventorySlots.size()) {
					foundSlot = null;
					break;
				}

				Slot candidate = this.inventorySlots.get(index);
				if(candidate.isAtCursorPos(clickX, clickY)) {
					foundSlot = candidate;
					break;
				}

				++index;
			}

			Slot slot = foundSlot;
			if(slot != null) {
				slot.onSlotChanged();
				ItemStack stackInSlot = slot.inventory.getStackInSlot(slot.slotIndex);
				if(stackInSlot == null && this.itemStack == null) {
					return;
				}

				if(stackInSlot != null && this.itemStack == null) {
					// Pick up the whole stack, or half of it when right-clicking.
					int transferSize = button == 0 ? stackInSlot.stackSize : (stackInSlot.stackSize + 1) / 2;
					this.itemStack = slot.inventory.decrStackSize(slot.slotIndex, transferSize);
					if(stackInSlot.stackSize == 0) {
						slot.putStack((ItemStack)null);
					}

					slot.onPickupFromSlot();
				} else if(stackInSlot == null && this.itemStack != null && slot.isItemValid(this.itemStack)) {
					// Place the held stack into the empty slot.
					int transferSize = button == 0 ? this.itemStack.stackSize : 1;
					if(transferSize > slot.inventory.getInventoryStackLimit()) {
						transferSize = slot.inventory.getInventoryStackLimit();
					}

					slot.putStack(this.itemStack.splitStack(transferSize));
					if(this.itemStack.stackSize == 0) {
						this.itemStack = null;
					}
				} else {
					if(stackInSlot == null || this.itemStack == null) {
						return;
					}

					ItemStack heldItem;
					if(!slot.isItemValid(this.itemStack)) {
						// Cursor item can't sit in this slot; if it matches the slot's item, add it to the stored stack.
						if(stackInSlot.itemID == this.itemStack.itemID) {
							heldItem = this.itemStack;
							if(heldItem.getItem().getItemStackLimit() > 1) {
								int transferSize = stackInSlot.stackSize;
								if(transferSize > 0) {
									int combinedSize = transferSize + this.itemStack.stackSize;
									heldItem = this.itemStack;
									if(combinedSize <= heldItem.getItem().getItemStackLimit()) {
										this.itemStack.stackSize += transferSize;
										stackInSlot.splitStack(transferSize);
										if(stackInSlot.stackSize == 0) {
											slot.putStack((ItemStack)null);
										}

										slot.onPickupFromSlot();
										return;
									}
								}

								return;
							}
						}

						return;
					}

					if(stackInSlot.itemID != this.itemStack.itemID) {
						// Different items: swap the stored slot contents with the held stack.
						if(this.itemStack.stackSize > slot.inventory.getInventoryStackLimit()) {
							return;
						}

						slot.putStack(this.itemStack);
						this.itemStack = stackInSlot;
					} else {
						if(stackInSlot.itemID != this.itemStack.itemID) {
							return;
						}

						if(button == 0) {
							// Left click: move as much of the held stack as fits into the slot.
							int transferSize = this.itemStack.stackSize;
							if(transferSize > slot.inventory.getInventoryStackLimit() - stackInSlot.stackSize) {
								transferSize = slot.inventory.getInventoryStackLimit() - stackInSlot.stackSize;
							}

							heldItem = this.itemStack;
							if(transferSize > heldItem.getItem().getItemStackLimit() - stackInSlot.stackSize) {
								heldItem = this.itemStack;
								transferSize = heldItem.getItem().getItemStackLimit() - stackInSlot.stackSize;
							}

							this.itemStack.splitStack(transferSize);
							if(this.itemStack.stackSize == 0) {
								this.itemStack = null;
							}

							stackInSlot.stackSize += transferSize;
						} else {
							if(button != 1) {
								return;
							}

							// Right click: move exactly one item from the held stack.
							int transferSize = 1;
							if(1 > slot.inventory.getInventoryStackLimit() - stackInSlot.stackSize) {
								transferSize = slot.inventory.getInventoryStackLimit() - stackInSlot.stackSize;
							}

							heldItem = this.itemStack;
							if(transferSize > heldItem.getItem().getItemStackLimit() - stackInSlot.stackSize) {
								heldItem = this.itemStack;
								transferSize = heldItem.getItem().getItemStackLimit() - stackInSlot.stackSize;
							}

							this.itemStack.splitStack(transferSize);
							if(this.itemStack.stackSize == 0) {
								this.itemStack = null;
							}

							stackInSlot.stackSize += transferSize;
						}
					}
				}
			} else if(this.itemStack != null) {
				// Clicking outside the container bounds drops the held stack (or one item) into the world.
				int guiLeft = (this.width - this.xSize) / 2;
				clickY = (this.height - this.ySize) / 2;
				if(mouseX < guiLeft || mouseY < clickY || mouseX >= guiLeft + this.xSize || mouseY >= clickY + this.xSize) {
					EntityPlayerSP player = this.mc.thePlayer;
					if(button == 0) {
						player.dropPlayerItem(this.itemStack);
						this.itemStack = null;
					}

					if(button == 1) {
						player.dropPlayerItem(this.itemStack.splitStack(1));
						if(this.itemStack.stackSize == 0) {
							this.itemStack = null;
						}
					}
				}
			}
		}

	}

	protected final void keyTyped(char typedChar, int keyCode) {
		if(keyCode == 1 || keyCode == this.mc.gameSettings.keyBindInventory.keyCode) {
			this.mc.displayGuiScreen((GuiScreen)null);
		}

	}

	public void onGuiClosed() {
		if(this.itemStack != null) {
			this.mc.thePlayer.dropPlayerItem(this.itemStack);
		}

	}

	public void guiCraftingItemsCheck() {
	}

	public final boolean doesGuiPauseGame() {
		return false;
	}
}
