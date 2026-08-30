package net.minecraft.client.gui;

/** A fatal error screen showing a title and message; it ignores all input. */
public final class GuiErrorScreen extends GuiScreen {
	private String title;
	private String text;

	public GuiErrorScreen(String title, String text) {
		this.title = title;
		this.text = text;
	}

	/** No buttons are created for this minimal screen. */
	public final void initGui() {
	}

	/** Draws a red-tinted gradient background with the title and message text. */
	public final void drawScreen(int mouseX, int mouseY, float partialTicks) {
		drawGradientRect(0, 0, this.width, this.height, -12574688, -11530224);
		drawCenteredString(this.fontRenderer, this.title, this.width / 2, 90, 16777215);
		drawCenteredString(this.fontRenderer, this.text, this.width / 2, 110, 16777215);
		super.drawScreen(mouseX, mouseY, partialTicks);
	}

	/** Suppresses all keyboard input so the error cannot be dismissed accidentally. */
	protected final void keyTyped(char typedChar, int keyCode) {
	}
}
