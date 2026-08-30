package net.minecraft.client.gui;

/** A generic yes/no confirmation prompt, used e.g. before deleting a world. */
public final class GuiYesNo extends GuiScreen {
	private GuiScreen parentScreen;
	private String message1;
	private String message2;
	private int worldNumber;

	public GuiYesNo(GuiScreen parentScreen, String message1, String message2, int worldNumber) {
		this.parentScreen = parentScreen;
		this.message1 = message1;
		this.message2 = message2;
		this.worldNumber = worldNumber;
	}

	/** Adds the Yes and No buttons side by side. */
	public final void initGui() {
		this.controlList.add(new GuiSmallButton(0, this.width / 2 - 155, this.height / 6 + 96, "Yes"));
		this.controlList.add(new GuiSmallButton(1, this.width / 2 - 155 + 160, this.height / 6 + 96, "No"));
	}

	/** Forwards the yes/no choice back to the parent screen (e.g. the delete confirmation). */
	protected final void actionPerformed(GuiButton button) {
		this.parentScreen.deleteWorld(button.id == 0, this.worldNumber);
	}

	/** Draws the background and the two confirmation messages. */
	public final void drawScreen(int mouseX, int mouseY, float partialTicks) {
		this.drawDefaultBackground();
		drawCenteredString(this.fontRenderer, this.message1, this.width / 2, 70, 16777215);
		drawCenteredString(this.fontRenderer, this.message2, this.width / 2, 90, 16777215);
		super.drawScreen(mouseX, mouseY, partialTicks);
	}
}
