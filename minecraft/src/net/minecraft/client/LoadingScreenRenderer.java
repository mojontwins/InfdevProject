package net.minecraft.client;

import com.mojang.nbt.NBTBase;
import com.mojang.nbt.NBTTagCompound;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.render.Tessellator;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import util.IProgressUpdate;

/**
 * Draws the in-game loading screen (fading dirt background and a progress
 * bar) while a world or game state is being prepared, and drives it via the
 * {@link IProgressUpdate} interface.
 *
 * This class also hosts static NBT {@link #read}/{@link #write} helpers used
 * to load and save gzip-compressed compound data (e.g. level metadata), which
 * have nothing to do with the loading screen itself.
 */
public class LoadingScreenRenderer implements IProgressUpdate {
	private String text;
	private Minecraft mc;
	private String title;
	private long start;

	public LoadingScreenRenderer(Minecraft mc) {
		this.text = "";
		this.title = "";
		this.start = System.currentTimeMillis();
		this.mc = mc;
	}

	public final void setTitle(String title) {
		if(!this.mc.running) {
			throw new MinecraftError();
		} else {
			this.title = title;
			ScaledResolution resolution = new ScaledResolution(this.mc.displayWidth, this.mc.displayHeight);
			int scaledWidth = resolution.getScaledWidth();
			int scaledHeight = resolution.getScaledHeight();
			GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
			// Reset to a 2D orthographic projection for screen-space drawing.
			GL11.glMatrixMode(GL11.GL_PROJECTION);
			GL11.glLoadIdentity();
			GL11.glOrtho(0.0D, (double)scaledWidth, (double)scaledHeight, 0.0D, 100.0D, 300.0D);
			GL11.glMatrixMode(GL11.GL_MODELVIEW);
			GL11.glLoadIdentity();
			GL11.glTranslatef(0.0F, 0.0F, -200.0F);
		}
	}

	public final void setText(String text) {
		if(!this.mc.running) {
			throw new MinecraftError();
		} else {
			this.start = 0L;
			this.text = text;
			this.setProgress(-1);
			this.start = 0L;
		}
	}

	public final void setProgress(int progress) {
		if(!this.mc.running) {
			throw new MinecraftError();
		} else {
			long now = System.currentTimeMillis();
			// Only redraw at most every 20 ms to avoid hammering the display.
			if(now - this.start >= 20L) {
				this.start = now;
				ScaledResolution resolution = new ScaledResolution(this.mc.displayWidth, this.mc.displayHeight);
				int scaledWidth = resolution.getScaledWidth();
				int scaledHeight = resolution.getScaledHeight();
				GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
				GL11.glMatrixMode(GL11.GL_PROJECTION);
				GL11.glLoadIdentity();
				GL11.glOrtho(0.0D, (double)scaledWidth, (double)scaledHeight, 0.0D, 100.0D, 300.0D);
				GL11.glMatrixMode(GL11.GL_MODELVIEW);
				GL11.glLoadIdentity();
				GL11.glTranslatef(0.0F, 0.0F, -200.0F);
				GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_COLOR_BUFFER_BIT);
				Tessellator tessellator = Tessellator.instance;
				int textureId = this.mc.renderEngine.getTexture("/dirt.png");
				GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
				// Draw the dirt texture as the background, tiled every 32 px.
				tessellator.startDrawingQuads();
				tessellator.setColorOpaque_I(4210752);
				tessellator.addVertexWithUV(0.0D, (double)scaledHeight, 0.0D, 0.0D, (double)((float)scaledHeight / 32.0F));
				tessellator.addVertexWithUV((double)scaledWidth, (double)scaledHeight, 0.0D, (double)((float)scaledWidth / 32.0F), (double)((float)scaledHeight / 32.0F));
				tessellator.addVertexWithUV((double)scaledWidth, 0.0D, 0.0D, (double)((float)scaledWidth / 32.0F), 0.0D);
				tessellator.addVertexWithUV(0.0D, 0.0D, 0.0D, 0.0D, 0.0D);
				tessellator.draw();
				if(progress >= 0) {
					// Draw the progress bar: grey track plus green fill proportional to progress.
					textureId = scaledWidth / 2 - 50;
					int barY = scaledHeight / 2 + 16;
					GL11.glDisable(GL11.GL_TEXTURE_2D);
					tessellator.startDrawingQuads();
					tessellator.setColorOpaque_I(8421504);
					tessellator.addVertex((double)textureId, (double)barY, 0.0D);
					tessellator.addVertex((double)textureId, (double)(barY + 2), 0.0D);
					tessellator.addVertex((double)(textureId + 100), (double)(barY + 2), 0.0D);
					tessellator.addVertex((double)(textureId + 100), (double)barY, 0.0D);
					tessellator.setColorOpaque_I(8454016);
					tessellator.addVertex((double)textureId, (double)barY, 0.0D);
					tessellator.addVertex((double)textureId, (double)(barY + 2), 0.0D);
					tessellator.addVertex((double)(textureId + progress), (double)(barY + 2), 0.0D);
					tessellator.addVertex((double)(textureId + progress), (double)barY, 0.0D);
					tessellator.draw();
					GL11.glEnable(GL11.GL_TEXTURE_2D);
				}

				this.mc.fontRenderer.drawStringWithShadow(this.title, (scaledWidth - this.mc.fontRenderer.getStringWidth(this.title)) / 2, scaledHeight / 2 - 4 - 16, 16777215);
				this.mc.fontRenderer.drawStringWithShadow(this.text, (scaledWidth - this.mc.fontRenderer.getStringWidth(this.text)) / 2, scaledHeight / 2 - 4 + 8, 16777215);
				Display.update();

				try {
					Thread.yield();
				} catch (Exception ignored) {
				}
			}
		}
	}

	public LoadingScreenRenderer() {
	}

	public static NBTTagCompound read(InputStream in) throws IOException {
		DataInputStream dataIn = new DataInputStream(new GZIPInputStream(in));

		NBTTagCompound rootTag;
		try {
			NBTBase tag = NBTBase.readNamedTag(dataIn);
			if(!(tag instanceof NBTTagCompound)) {
				throw new IOException("Root tag must be a named compound tag");
			}

			rootTag = (NBTTagCompound)tag;
		} finally {
			dataIn.close();
		}

		return rootTag;
	}

	public static void write(NBTTagCompound tag, OutputStream out) throws IOException {
		DataOutputStream dataOut = new DataOutputStream(new GZIPOutputStream(out));

		try {
			NBTBase.writeNamedTag(tag, dataOut);
		} finally {
			dataOut.close();
		}

	}
}
