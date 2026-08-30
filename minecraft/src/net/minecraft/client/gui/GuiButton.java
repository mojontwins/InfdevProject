package net.minecraft.client.gui;

import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;

/** A clickable button rendered from the Minecraft GUI texture, with enabled/disabled visual states. */
public class GuiButton extends Gui {
	private int width;
	private int height;
	private int xPosition;
	private int yPosition;
	public String displayString;
	public int id;
	public boolean enabled;
	private boolean visible;

	public GuiButton(int id, int x, int y, String buttonText) {
		this(id, x, y, 200, 20, buttonText);
	}

	protected GuiButton(int id, int x, int y, int width, int height, String buttonText) {
		this.width = 200;
		this.height = 20;
		this.enabled = true;
		this.visible = true;
		this.id = id;
		this.xPosition = x;
		this.yPosition = y;
		this.width = width;
		this.height = 20;
		this.displayString = buttonText;
	}

	/** Draws the button in its current state, choosing between disabled/normal/hovered textures and text colours. */
	public final void drawButton(Minecraft minecraft, int mouseX, int mouseY) {
		if(this.visible) {
			FontRenderer fontRenderer = minecraft.fontRenderer;
			GL11.glBindTexture(GL11.GL_TEXTURE_2D, minecraft.renderEngine.getTexture("/gui/gui.png"));
			GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
			byte state = 1;
			boolean hovered = mouseX >= this.xPosition && mouseY >= this.yPosition && mouseX < this.xPosition + this.width && mouseY < this.yPosition + this.height;
			if(!this.enabled) {
				state = 0;
			} else if(hovered) {
				state = 2;
			}

			// Draw the left and right halves of the button, centred on the state's
			// graphic row (state * 20 tall) in the GUI texture.
			this.drawTexturedModalRect(this.xPosition, this.yPosition, 0, 46 + state * 20, this.width / 2, this.height);
			this.drawTexturedModalRect(this.xPosition + this.width / 2, this.yPosition, 200 - this.width / 2, 46 + state * 20, this.width / 2, this.height);
			if(!this.enabled) {
				drawCenteredString(fontRenderer, this.displayString, this.xPosition + this.width / 2, this.yPosition + (this.height - 8) / 2, -6250336);
			} else if(hovered) {
				drawCenteredString(fontRenderer, this.displayString, this.xPosition + this.width / 2, this.yPosition + (this.height - 8) / 2, 16777120);
			} else {
				drawCenteredString(fontRenderer, this.displayString, this.xPosition + this.width / 2, this.yPosition + (this.height - 8) / 2, 14737632);
			}
		}
	}

	/** Returns true if the given mouse position lies within the button bounds. */
	public final boolean mousePressed(int mouseX, int mouseY) {
		return this.enabled && mouseX >= this.xPosition && mouseY >= this.yPosition && mouseX < this.xPosition + this.width && mouseY < this.yPosition + this.height;
	}
}
