package net.minecraft.client.gui;

import net.minecraft.client.GuiMainMenu;
import net.minecraft.game.world.World;

/** The pause/options menu shown while in-game, giving access to options, world selection and quitting. */
public final class GuiIngameMenu extends GuiScreen {
	public final void initGui() {
		this.controlList.clear();
		this.controlList.add(new GuiButton(0, this.width / 2 - 100, this.height / 4, "Options..."));
		this.controlList.add(new GuiButton(1, this.width / 2 - 100, this.height / 4 + 24, "Change world..."));
		this.controlList.add(new GuiButton(2, this.width / 2 - 100, this.height / 4 + 48, "Quit game"));
		this.controlList.add(new GuiButton(4, this.width / 2 - 100, this.height / 4 + 120, "Back to game"));
	}

	/** Performs the action associated with the clicked button. */
	protected final void actionPerformed(GuiButton button) {
		if(button.id == 0) {
			this.mc.displayGuiScreen(new GuiOptions(this, this.mc.gameSettings));
		}

		if(button.id == 1) {
			this.mc.displayGuiScreen(new GuiSelectWorld(this));
		}

		if(button.id == 2) {
			this.mc.closeWorld((World)null);
			this.mc.displayGuiScreen(new GuiMainMenu());
		}

		if(button.id == 4) {
			this.mc.displayGuiScreen((GuiScreen)null);
			this.mc.setIngameFocus();
		}

	}

	/** Draws the background, the menu title and the buttons. */
	public final void drawScreen(int mouseX, int mouseY, float partialTicks) {
		this.drawDefaultBackground();
		drawCenteredString(this.fontRenderer, "Game menu", this.width / 2, 40, 16777215);
		super.drawScreen(mouseX, mouseY, partialTicks);
	}
}
