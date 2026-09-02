package net.minecraft.client.gui;

import com.mojang.nbt.NBTTagCompound;
import java.io.File;
import net.minecraft.game.world.World;
import net.minecraft.game.world.WorldOptions;
import net.minecraft.game.world.WorldType;

/** The world selection screen, listing the five save slots and letting the player load or delete worlds. */
public class GuiSelectWorld extends GuiScreen {
	protected GuiScreen parentScreen;
	protected String screenTitle = "Select world";
	private boolean selected = false;

	public GuiSelectWorld(GuiScreen parentScreen) {
		this.parentScreen = parentScreen;
	}

	/** Creates a button per save slot, showing "empty" or the world name with its real size on disk. */
	public final void initGui() {
		File appDir = this.mc.getAppDir();

		for(int slotNumber = 0; slotNumber < 5; ++slotNumber) {
			NBTTagCompound worldTag = World.getWorldNBTTag(appDir, "World" + (slotNumber + 1));
			if(worldTag == null) {
				this.controlList.add(new GuiButton(slotNumber, this.width / 2 - 100, this.height / 6 + slotNumber * 24, "- empty -"));
			} else {
				String buttonText = "World " + (slotNumber + 1);
				long sizeOnDisk = World.getWorldSize(appDir, "World" + (slotNumber + 1));
				// Show the real disk usage in MB (divide by 1024 twice, preserving two decimals).
				buttonText = buttonText + " (" + (float)(sizeOnDisk / 1024L * 100L / 1024L) / 100.0F + " MB)";
				if(World.isLegacyWorldFormat(appDir, "World" + (slotNumber + 1))) {
					// Keep the pre-256-height saves clearly labelled so the new format is easy to spot.
					buttonText = buttonText + " (OLD)";
				}
				this.controlList.add(new GuiButton(slotNumber, this.width / 2 - 100, this.height / 6 + slotNumber * 24, buttonText));
			}
		}

		this.initGui2();
	}

	/** Returns the save-name string for the given slot, or null if that slot is empty. */
	protected final String getWorldName(int slotNumber) {
		File appDir = this.mc.getAppDir();
		return World.getWorldNBTTag(appDir, "World" + slotNumber) != null ? "World" + slotNumber : null;
	}

	public void initGui2() {
		this.controlList.add(new GuiButton(5, this.width / 2 - 100, this.height / 6 + 120 + 12, "Delete world..."));
		this.controlList.add(new GuiButton(6, this.width / 2 - 100, this.height / 6 + 168, "Cancel"));
	}

	/** Routes button clicks: load a world, open the delete screen, or go back. */
	protected final void actionPerformed(GuiButton button) {
		if(button.enabled) {
			if(button.id < 5) {
				this.selectWorld(button.id + 1);
			} else if(button.id == 5) {
				this.mc.displayGuiScreen(new GuiDeleteWorld(this));
			} else {
				if(button.id == 6) {
					this.mc.displayGuiScreen(this.parentScreen);
				}

			}
		}
	}

	/**
	 * Starts the given world, guarding against double selection. A fresh
	 * {@link WorldOptions} (every flag at its default, all false for now) is
	 * handed to the new world; when the slot already holds a world the
	 * constructor later re-reads the saved options and {@link WorldType} from
	 * level.dat. The default {@code WORLDTYPE_420} is the creation-time world
	 * type until a selection screen exists.
	 */
	public void selectWorld(int slotNumber) {
		this.mc.displayGuiScreen((GuiScreen)null);
		if(!this.selected) {
			this.selected = true;
			this.mc.startWorld("World" + slotNumber, new WorldOptions(), WorldType.WORLDTYPE_420);
			this.mc.displayGuiScreen((GuiScreen)null);
		}
	}

	/** Draws the background, the title and the buttons. */
	public final void drawScreen(int mouseX, int mouseY, float partialTicks) {
		this.drawDefaultBackground();
		drawCenteredString(this.fontRenderer, this.screenTitle, this.width / 2, 20, 16777215);
		super.drawScreen(mouseX, mouseY, partialTicks);
	}
}
