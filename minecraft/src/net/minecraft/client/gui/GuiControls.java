package net.minecraft.client.gui;

import net.minecraft.client.GameSettings;

/** The controls/key-bindings screen, letting the player remap keys and displaying the current bindings. */
public final class GuiControls extends GuiScreen {
	private GuiScreen parentScreen;
	private String screenTitle = "Controls";
	private GameSettings optiond;
	private int buttonId = -1;

	public GuiControls(GuiScreen parentScreen, GameSettings settings) {
		this.parentScreen = parentScreen;
		this.optiond = settings;
	}

	/** Lays out one small button per key binding in two columns, plus a Done button. */
	public final void initGui() {
		for(int index = 0; index < this.optiond.keyBindings.length; ++index) {
			this.controlList.add(new GuiSmallButton(index, this.width / 2 - 155 + index % 2 * 160, this.height / 6 + 24 * (index >> 1), this.optiond.getOptionDisplayString(index)));
		}

		this.controlList.add(new GuiButton(200, this.width / 2 - 100, this.height / 6 + 168, "Done"));
	}

	/** Handles button clicks: refreshes labels, closes on Done, or starts rebinding the selected key. */
	protected final void actionPerformed(GuiButton button) {
		for(int index = 0; index < this.optiond.keyBindings.length; ++index) {
			this.controlList.get(index).displayString = this.optiond.getOptionDisplayString(index);
		}

		if(button.id == 200) {
			this.mc.displayGuiScreen(this.parentScreen);
		} else {
			this.buttonId = button.id;
			button.displayString = "> " + this.optiond.getOptionDisplayString(button.id) + " <";
		}
	}

	/** When a key is pressed while a binding is being captured, assign it and refresh the label. */
	protected final void keyTyped(char typedChar, int keyCode) {
		if(this.buttonId >= 0) {
			this.optiond.setKeyBinding(this.buttonId, keyCode);
			this.controlList.get(this.buttonId).displayString = this.optiond.getOptionDisplayString(this.buttonId);
			this.buttonId = -1;
		} else {
			super.keyTyped(typedChar, keyCode);
		}
	}

	/** Draws the title and background, then the buttons. */
	public final void drawScreen(int mouseX, int mouseY, float partialTicks) {
		this.drawDefaultBackground();
		drawCenteredString(this.fontRenderer, this.screenTitle, this.width / 2, 20, 16777215);
		super.drawScreen(mouseX, mouseY, partialTicks);
	}
}
