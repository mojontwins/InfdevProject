package net.minecraft.client.gui;

import net.minecraft.client.GuiMainMenu;
import net.minecraft.game.world.World;
import org.lwjgl.opengl.GL11;

/** The death screen shown when the player dies, offering to respawn or return to the title menu. */
public final class GuiGameOver extends GuiScreen {
	public final void initGui() {
		this.controlList.clear();
		this.controlList.add(new GuiButton(1, this.width / 2 - 100, this.height / 4 + 72, "Respawn"));
		this.controlList.add(new GuiButton(2, this.width / 2 - 100, this.height / 4 + 96, "Title menu"));
		if(this.mc.session == null) {
			// Without a session there is no save to return to, so disable respawn.
			this.controlList.get(1).enabled = false;
		}

	}

	/** Suppresses all keyboard input on the death screen. */
	protected final void keyTyped(char typedChar, int keyCode) {
	}

	/** Handles the button choices: respawn or return to the title menu. */
	protected final void actionPerformed(GuiButton button) {
		if(button.id == 1) {
			this.mc.respawn();
			this.mc.displayGuiScreen((GuiScreen)null);
		}

		if(button.id == 2) {
			this.mc.closeWorld((World)null);
			this.mc.displayGuiScreen(new GuiMainMenu());
		}

	}

	/** Draws the red-tinted death gradient, a scaled "Game over!" title and the player's score. */
	public final void drawScreen(int mouseX, int mouseY, float partialTicks) {
		drawGradientRect(0, 0, this.width, this.height, 1615855616, -1602211792);
		GL11.glPushMatrix();
		GL11.glScalef(2.0F, 2.0F, 2.0F);
		drawCenteredString(this.fontRenderer, "Game over!", this.width / 2 / 2, 30, 16777215);
		GL11.glPopMatrix();
		String scoreText = "Score: &e" + this.mc.thePlayer.score;
		drawCenteredString(this.fontRenderer, scoreText, this.width / 2, 100, 16777215);
		super.drawScreen(mouseX, mouseY, partialTicks);
	}

	public final boolean doesGuiPauseGame() {
		return false;
	}
}
