package net.minecraft.client.gui;

import java.io.File;
import net.minecraft.game.world.World;

/** The world deletion screen, wrapping the select-world layout but confirming deletion via a yes/no prompt. */
public final class GuiDeleteWorld extends GuiSelectWorld {
	public GuiDeleteWorld(GuiScreen parentScreen) {
		super(parentScreen);
		this.screenTitle = "Delete world";
	}

	/** Adds a Cancel button in place of the usual select-world actions. */
	public final void initGui2() {
		this.controlList.add(new GuiButton(6, this.width / 2 - 100, this.height / 6 + 168, "Cancel"));
	}

	/** Selecting a world asks for confirmation before deleting it. */
	public final void selectWorld(int slotNumber) {
		String worldName = this.getWorldName(slotNumber);
		if(worldName != null) {
			this.mc.displayGuiScreen(new GuiYesNo(this, "Are you sure you want to delete this world?", "\'" + worldName + "\' will be lost forever!", slotNumber));
		}

	}

	/** Performs the actual deletion if confirmed, then returns to the parent screen. */
	public final void deleteWorld(boolean confirmed, int slotNumber) {
		if(confirmed) {
			File worldDir = this.mc.getAppDir();
			World.deleteWorld(worldDir, this.getWorldName(slotNumber));
		}

		this.mc.displayGuiScreen(this.parentScreen);
	}
}
