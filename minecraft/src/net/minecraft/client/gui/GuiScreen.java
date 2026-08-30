package net.minecraft.client.gui;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.render.Tessellator;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

/** Base class for all in-game and menu screens: manages the button list and routes input events. */
public class GuiScreen extends Gui {
	protected Minecraft mc;
	public int width;
	public int height;
	protected List<GuiButton> controlList = new ArrayList<>();
	public boolean allowUserInput = false;
	protected FontRenderer fontRenderer;

	/** Draws the background (handled by subclasses) and every button in the control list. */
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		for(int index = 0; index < this.controlList.size(); ++index) {
			GuiButton button = this.controlList.get(index);
			button.drawButton(this.mc, mouseX, mouseY);
		}

	}

	/** Default keyboard handler: pressing Esc (key 1) closes the screen. */
	protected void keyTyped(char typedChar, int keyCode) {
		if(keyCode == 1) {
			this.mc.displayGuiScreen((GuiScreen)null);
			this.mc.setIngameFocus();
		}

	}

	/** Routes a mouse click to the matching button and fires its action. */
	protected void mouseClicked(int mouseX, int mouseY, int button) {
		if(button == 0) {
			for(button = 0; button < this.controlList.size(); ++button) {
				GuiButton control = this.controlList.get(button);
				if(control.mousePressed(mouseX, mouseY)) {
					this.mc.sndManager.playSoundFX("random.click", 1.0F, 1.0F);
					this.actionPerformed(control);
				}
			}
		}

	}

	protected void actionPerformed(GuiButton button) {
	}

	/** Stores the Minecraft instance and resolution, then (re)initialises the screen. */
	public final void setWorldAndResolution(Minecraft minecraft, int screenWidth, int screenHeight) {
		this.mc = minecraft;
		this.fontRenderer = minecraft.fontRenderer;
		this.width = screenWidth;
		this.height = screenHeight;
		this.initGui();
	}

	public void initGui() {
	}

	/** Polls the mouse events and converts raw window coordinates to scaled screen coordinates. */
	public final void handleMouseInput() {
		if(Mouse.getEventButtonState()) {
			int mouseX = Mouse.getEventX() * this.width / this.mc.displayWidth;
			int mouseY = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;
			this.mouseClicked(mouseX, mouseY, Mouse.getEventButton());
		} else {
			Mouse.getEventX();
			Mouse.getEventY();
			Mouse.getEventButton();
		}
	}

	/** Polls keyboard events, handling fullscreen toggle and forwarding typed keys. */
	public final void handleKeyboardInput() {
		if(Keyboard.getEventKeyState()) {
			if(Keyboard.getEventKey() == Keyboard.KEY_F11) {
				this.mc.toggleFullscreen();
				return;
			}

			this.keyTyped(Keyboard.getEventCharacter(), Keyboard.getEventKey());
		}

	}

	public void updateScreen() {
	}

	public void onGuiClosed() {
	}

	/** Fills the background: a gradient in-game, or the tiled dirt background on menus. */
	public final void drawDefaultBackground() {
		if(this.mc.theWorld != null) {
			drawGradientRect(0, 0, this.width, this.height, 1610941696, -1607454624);
		} else {
			GL11.glDisable(GL11.GL_LIGHTING);
			GL11.glDisable(GL11.GL_FOG);
			Tessellator tessellator = Tessellator.instance;
			GL11.glBindTexture(GL11.GL_TEXTURE_2D, this.mc.renderEngine.getTexture("/dirt.png"));
			GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
			// Tile the 32x32 dirt texture across the whole screen.
			tessellator.startDrawingQuads();
			tessellator.setColorOpaque_I(4210752);
			tessellator.addVertexWithUV(0.0D, (double)this.height, 0.0D, 0.0D, (double)((float)this.height / 32.0F));
			tessellator.addVertexWithUV((double)this.width, (double)this.height, 0.0D, (double)((float)this.width / 32.0F), (double)((float)this.height / 32.0F));
			tessellator.addVertexWithUV((double)this.width, 0.0D, 0.0D, (double)((float)this.width / 32.0F), 0.0D);
			tessellator.addVertexWithUV(0.0D, 0.0D, 0.0D, 0.0D, 0.0D);
			tessellator.draw();
		}
	}

	public boolean doesGuiPauseGame() {
		return true;
	}

	public void deleteWorld(boolean confirmed, int slotNumber) {
	}
}
