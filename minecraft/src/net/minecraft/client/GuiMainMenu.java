package net.minecraft.client;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSelectWorld;
import net.minecraft.client.render.Tessellator;
import org.lwjgl.opengl.GL11;
import util.MathHelper;

/**
 * The title screen shown at startup: renders the logo, a randomly chosen
 * splash message, the main menu buttons, and memory-usage statistics.
 */
public final class GuiMainMenu extends GuiScreen {
	private String[] splashes = new String[]{"Pre-beta!", "As seen on TV!", "Awesome!", "100% pure!", "May contain nuts!", "Better than Prey!", "More polygons!", "Sexy!", "Limited edition!", "Flashing letters!", "Made by Notch!", "Coming soon!", "Best in class!", "When it\'s finished!", "Absolutely dragon free!", "Excitement!", "More than 5000 sold!", "One of a kind!", "700+ hits on YouTube!", "Indev!", "Spiders everywhere!", "Check it out!", "Holy cow, man!", "It\'s a game!", "Made in Sweden!", "Uses LWJGL!", "Reticulating splines!", "Minecraft!", "Yaaay!", "Alpha version!", "Singleplayer!", "Keyboard compatible!", "Undocumented!", "Ingots!", "Exploding creepers!", "That\'s not a moon!", "l33t!", "Create!", "Survive!", "Dungeon!", "Exclusive!", "The bee\'s knees!", "Down with O.P.P.!", "Closed source!", "Classy!", "Wow!", "Not on steam!", "9.95 euro!", "Half price!", "Oh man!", "Check it out!", "Awesome community!", "Pixels!", "Teetsuuuuoooo!", "Kaaneeeedaaaa!", "Now with difficulty!", "Enhanced!", "90% bug free!", "Pretty!", "12 herbs and spices!", "Fat free!", "Absolutely no memes!", "Free dental!", "Ask your doctor!", "Minors welcome!", "Cloud computing!", "Legal in Finland!", "Hard to label!", "Technically good!", "Bringing home the bacon!", "Indie!", "GOTY!", "Ceci n\'est pas une title screen!", "Euclidian!", "Now in 3D!", "Inspirational!", "Herregud!", "Complex cellular automata!", "Yes, sir!", "Played by cowboys!", "OpenGL 1.1!", "Thousands of colors!", "Try it!", "Age of Wonders is better!", "Try the mushroom stew!", "Sensational!", "Hot tamale, hot hot tamale!", "Play him off, keyboard cat!", "Guaranteed!", "Macroscopic!", "Bring it on!", "Random splash!", "Call your mother!", "Monster infighting!", "Loved by millions!", "Ultimate edition!", "Freaky!", "You\'ve got a brand new key!", "Water proof!", "Uninflammable!", "Whoa, dude!", "All inclusive!", "Tell your friends!", "NP is not in P!", "Notch <3 Ez!", "Music by C418!"};
	private String currentSplash = this.splashes[(int)(Math.random() * (double)this.splashes.length)];

	public final void updateScreen() {
	}

	protected final void keyTyped(char typedChar, int keyCode) {
	}

	public final void initGui() {
		this.controlList.clear();
		this.controlList.add(new GuiButton(1, this.width / 2 - 100, this.height / 4 + 48, "Single player"));
		this.controlList.add(new GuiButton(2, this.width / 2 - 100, this.height / 4 + 72, "Multi player"));
		this.controlList.add(new GuiButton(3, this.width / 2 - 100, this.height / 4 + 96, "Play tutorial level"));
		this.controlList.add(new GuiButton(0, this.width / 2 - 100, this.height / 4 + 120 + 12, "Options..."));
		this.controlList.get(1).enabled = false;
		this.controlList.get(2).enabled = false;
		if(this.mc.session == null) {
			this.controlList.get(1).enabled = false;
		}

	}

	protected final void actionPerformed(GuiButton button) {
		if(button.id == 0) {
			this.mc.displayGuiScreen(new GuiOptions(this, this.mc.gameSettings));
		}

		if(button.id == 1) {
			this.mc.displayGuiScreen(new GuiSelectWorld(this));
		}

	}

	public final void drawScreen(int mouseX, int mouseY, float partialTicks) {
		this.drawDefaultBackground();
		Tessellator tessellator = Tessellator.instance;
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, this.mc.renderEngine.getTexture("/gui/logo.png"));
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		tessellator.setColorOpaque_I(16777215);
		this.drawTexturedModalRect((this.width - 256) / 2, 30, 0, 0, 256, 49);
		// Draw the splash text, tilted and gently pulsing in size.
		GL11.glPushMatrix();
		GL11.glTranslatef((float)(this.width / 2 + 90), 70.0F, 0.0F);
		GL11.glRotatef(-20.0F, 0.0F, 0.0F, 1.0F);
		float splashScale = 1.8F - MathHelper.abs(MathHelper.sin((float)(System.currentTimeMillis() % 1000L) / 1000.0F * (float)Math.PI * 2.0F) * 0.1F);
		splashScale = splashScale * 100.0F / (float)(this.fontRenderer.getStringWidth(this.currentSplash) + 32);
		GL11.glScalef(splashScale, splashScale, splashScale);
		drawCenteredString(this.fontRenderer, this.currentSplash, 0, -8, 16776960);
		GL11.glPopMatrix();
		String text = "Copyright Mojang Specifications. Do not distribute.";
		drawString(this.fontRenderer, text, this.width - this.fontRenderer.getStringWidth(text) - 2, this.height - 10, 16777215);
		long maxMemory = Runtime.getRuntime().maxMemory();
		long totalMemory = Runtime.getRuntime().totalMemory();
		long freeMemory = Runtime.getRuntime().freeMemory();
		long usedMemory = maxMemory - freeMemory;
		text = "Free memory: " + usedMemory * 100L / maxMemory + "% of " + maxMemory / 1024L / 1024L + "MB";
		drawString(this.fontRenderer, text, this.width - this.fontRenderer.getStringWidth(text) - 2, 2, 8421504);
		text = "Allocated memory: " + totalMemory * 100L / maxMemory + "% (" + totalMemory / 1024L / 1024L + "MB)";
		drawString(this.fontRenderer, text, this.width - this.fontRenderer.getStringWidth(text) - 2, 12, 8421504);
		super.drawScreen(mouseX, mouseY, partialTicks);
	}
}
