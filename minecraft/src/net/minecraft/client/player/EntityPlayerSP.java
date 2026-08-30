package net.minecraft.client.player;

import com.mojang.nbt.NBTTagCompound;
import com.mojang.nbt.NBTTagList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Session;
import net.minecraft.client.effect.EntityPickupFX;
import net.minecraft.client.gui.container.GuiChest;
import net.minecraft.client.gui.container.GuiCrafting;
import net.minecraft.client.gui.container.GuiFurnace;
import net.minecraft.game.IInventory;
import net.minecraft.game.entity.Entity;
import net.minecraft.game.entity.player.EntityPlayer;
import net.minecraft.game.item.ItemStack;
import net.minecraft.game.world.World;
import net.minecraft.game.world.block.tileentity.TileEntityFurnace;

/**
 * The client's own player: wired to the keyboard and mouse through
 * {@link MovementInput}, able to open chests, workbenches and furnaces, and —
 * being the only player class with an inventory to save — the one that
 * serializes items into the world data.
 */
public class EntityPlayerSP extends EntityPlayer {
	public MovementInput movementInput;
	private Minecraft mc;

	public EntityPlayerSP(Minecraft mc, World world, Session session) {
		super(world);
		this.mc = mc;
		if(world != null) {
			if(world.playerEntity != null) {
				World.setEntityDead(world.playerEntity);
			}

			world.playerEntity = this;
		}

		if(session != null) {
			this.skinUrl = "http://www.minecraft.net/skin/" + session.name + ".png";
		}

		this.username = session.name;
	}

	/** Takes the movement values straight off the keyboard/mouse input. */
	public final void updatePlayerActionState() {
		this.moveStrafing = this.movementInput.moveStrafe;
		this.moveForward = this.movementInput.moveForward;
		this.isJumping = this.movementInput.jump;
	}

	/** The player sneaks while the sneak key is held — feeds the ledge guard and the mob sight rules. */
	@Override
	public final boolean isSneaking() {
		return this.movementInput.sneak;
	}

	public final void onLivingUpdate() {
		this.movementInput.updatePlayerMoveState();
		super.onLivingUpdate();
	}

	public final void writeEntityToNBT(NBTTagCompound tag) {
		super.writeEntityToNBT(tag);
		tag.setInteger("Score", this.score);
		NBTTagList inventoryTag = new NBTTagList();

		for(int slot = 0; slot < this.inventory.mainInventory.length; ++slot) {
			ItemStack stack = this.inventory.mainInventory[slot];
			if(stack != null) {
				NBTTagCompound itemTag = new NBTTagCompound();
				itemTag.setByte("Slot", (byte)slot);
				stack.writeToNBT(itemTag);
				inventoryTag.setTag(itemTag);
			}
		}

		for(int slot = 0; slot < this.inventory.armorInventory.length; ++slot) {
			ItemStack stack = this.inventory.armorInventory[slot];
			if(stack != null) {
				NBTTagCompound itemTag = new NBTTagCompound();
				itemTag.setByte("Slot", (byte)(slot + 100));
				stack.writeToNBT(itemTag);
				inventoryTag.setTag(itemTag);
			}
		}

		tag.setTag("Inventory", inventoryTag);
	}

	public final void readEntityFromNBT(NBTTagCompound tag) {
		super.readEntityFromNBT(tag);
		this.score = tag.getInteger("Score");
		NBTTagList inventoryTag = tag.getTagList("Inventory");
		this.inventory.mainInventory = new ItemStack[36];
		this.inventory.armorInventory = new ItemStack[4];

		for(int index = 0; index < inventoryTag.tagCount(); ++index) {
			NBTTagCompound itemTag = (NBTTagCompound)inventoryTag.tagAt(index);
			int slot = itemTag.getByte("Slot") & 255;
			if(slot >= 0 && slot < this.inventory.mainInventory.length) {
				this.inventory.mainInventory[slot] = new ItemStack(itemTag);
			}

			if(slot >= 100 && slot < this.inventory.armorInventory.length + 100) {
				this.inventory.armorInventory[slot - 100] = new ItemStack(itemTag);
			}
		}

	}

	public final void displayChestGUI(IInventory inventory) {
		this.mc.displayGuiScreen(new GuiChest(this.inventory, inventory));
	}

	public final void displayWorkbenchGUI() {
		this.mc.displayGuiScreen(new GuiCrafting(this.inventory));
	}

	public final void displayFurnaceGUI(TileEntityFurnace tileEntityFurnace) {
		this.mc.displayGuiScreen(new GuiFurnace(this.inventory, tileEntityFurnace));
	}

	public final void displayInventoryGUI() {
		this.inventory.setInventorySlotContents(this.inventory.currentItem, (ItemStack)null);
	}

	public final void onItemPickup(Entity item) {
		this.mc.effectRenderer.addEffect(new EntityPickupFX(this.mc.theWorld, item, this, -0.5F));
	}
}