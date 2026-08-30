package net.minecraft.client.gui;

/** A 150px-wide button, used for the compact option and control rows. */
public final class GuiSmallButton extends GuiButton {
	public GuiSmallButton(int id, int x, int y, String buttonText) {
		super(id, x, y, 150, 20, buttonText);
	}
}
