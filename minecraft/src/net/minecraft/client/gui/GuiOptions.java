package net.minecraft.client.gui;

import net.minecraft.client.GameSettings;

/** The options menu: adjusts game settings via small buttons, with links to controls and Done. */
public final class GuiOptions extends GuiScreen {
	private GuiScreen parentScreen;
	private String screenTitle = "Options";
	private GameSettings options;

	public GuiOptions(GuiScreen parentScreen, GameSettings settings) {
		this.parentScreen = parentScreen;
		this.options = settings;
	}

	/** Creates a small button for each option plus Controls and Done buttons. */
	public final void initGui() {
		for(int index = 0; index < this.options.numberOfOptions; ++index) {
			this.controlList.add(new GuiSmallButton(index, this.width / 2 - 155 + index % 2 * 160, this.height / 6 + 24 * (index >> 1), this.options.getKeyBinding(index)));
		}

		this.controlList.add(new GuiButton(100, this.width / 2 - 100, this.height / 6 + 120 + 12, "Controls..."));
		this.controlList.add(new GuiButton(200, this.width / 2 - 100, this.height / 6 + 168, "Done"));
	}

	/** Handles option toggles, opening the controls screen, or returning to the parent. */
	protected final void actionPerformed(GuiButton button) {
		if(button.enabled) {
			// Option buttons have id < 100; cycle the setting's value.
			if(button.id < 100) {
				this.options.setOptionFloatValue(button.id, 1);
				button.displayString = this.options.getKeyBinding(button.id);
			}

			if(button.id == 100) {
				this.mc.displayGuiScreen(new GuiControls(this, this.options));
			}

			if(button.id == 200) {
				this.mc.displayGuiScreen(this.parentScreen);
			}

		}
	}

	/** Draws the background, the options title and the buttons. */
	public final void drawScreen(int mouseX, int mouseY, float partialTicks) {
		this.drawDefaultBackground();
		drawCenteredString(this.fontRenderer, this.screenTitle, this.width / 2, 20, 16777215);
		super.drawScreen(mouseX, mouseY, partialTicks);
	}
}
