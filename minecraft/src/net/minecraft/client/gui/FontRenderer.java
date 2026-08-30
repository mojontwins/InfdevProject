package net.minecraft.client.gui;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.IntBuffer;
import javax.imageio.ImageIO;
import net.minecraft.client.GameSettings;
import net.minecraft.client.render.RenderEngine;
import net.minecraft.client.render.Tessellator;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

/**
 * Renders text to the screen using an OpenGL display-list cache of the
 * Minecraft font texture. Each character is stored as a display list and
 * drawn via GL call lists for efficiency. This is the class behind all
 * in-game and menu text.
 */
public final class FontRenderer {
	private int[] charWidth = new int[256];
	private int fontTextureInt = 0;
	private int fontDisplayLists;
	private IntBuffer buffer = BufferUtils.createIntBuffer(1024);

	/**
	 * Loads the font texture, measures each glyph's advance width, and builds the
	 * display lists used to render each character.
	 */
	public FontRenderer(GameSettings settings, String fontPath, RenderEngine renderEngine) {
		BufferedImage fontImage;
		try {
			fontImage = ImageIO.read(RenderEngine.class.getResourceAsStream(fontPath));
		} catch (IOException exception) {
			throw new RuntimeException(exception);
		}

		int imageWidth = fontImage.getWidth();
		int imageHeight = fontImage.getHeight();
		int[] pixels = new int[imageWidth * imageHeight];
		fontImage.getRGB(0, 0, imageWidth, imageHeight, pixels, 0, imageWidth);

		int cellX;
		int width;
		int pixelIndex;
		int pixelRow;
		int alpha;
		// Measure the visible width of the first 128 glyphs, scanning the 8x8
		// cell of each character in the 16x8 font atlas to find its right edge.
		for(int character = 0; character < 128; ++character) {
			imageHeight = character % 16;
			cellX = character / 16;
			width = 0;

			for(boolean foundAlpha = false; width < 8 && !foundAlpha; ++width) {
				pixelIndex = (imageHeight << 3) + width;
				foundAlpha = true;

				for(int pixelCol = 0; pixelCol < 8 && foundAlpha; ++pixelCol) {
					pixelRow = ((cellX << 3) + pixelCol) * imageWidth;
					alpha = pixels[pixelIndex + pixelRow] & 255;
					if(alpha > 128) {
						foundAlpha = false;
					}
				}
			}

			// The space character has no visible pixels, so give it a manual width of 4.
			if(character == 32) {
				width = 4;
			}

			this.charWidth[character] = width;
		}

		this.fontTextureInt = renderEngine.getTexture(fontPath);
		this.fontDisplayLists = GL11.glGenLists(288);
		Tessellator tessellator = Tessellator.instance;

		// Build one display list per character (256) plus the colour variants used
		// for the grey/colour shadow digits.
		for(imageHeight = 0; imageHeight < 256; ++imageHeight) {
			GL11.glNewList(this.fontDisplayLists + imageHeight, GL11.GL_COMPILE);
			tessellator.startDrawingQuads();
			cellX = imageHeight % 16 << 3;
			width = imageHeight / 16 << 3;
			tessellator.addVertexWithUV(0.0D, (double)7.99F, 0.0D, (double)((float)cellX / 128.0F), (double)(((float)width + 7.99F) / 128.0F));
			tessellator.addVertexWithUV((double)7.99F, (double)7.99F, 0.0D, (double)(((float)cellX + 7.99F) / 128.0F), (double)(((float)width + 7.99F) / 128.0F));
			tessellator.addVertexWithUV((double)7.99F, 0.0D, 0.0D, (double)(((float)cellX + 7.99F) / 128.0F), (double)((float)width / 128.0F));
			tessellator.addVertexWithUV(0.0D, 0.0D, 0.0D, (double)((float)cellX / 128.0F), (double)((float)width / 128.0F));
			tessellator.draw();
			// Advance the "pen" by this character's measured width after drawing.
			GL11.glTranslatef((float)this.charWidth[imageHeight], 0.0F, 0.0F);
			GL11.glEndList();
		}

		// Build the 32 shadow/colour display lists used by the "&x" colour codes.
		// Each list uses a fixed RGB colour derived from the loop index.
		for(imageHeight = 0; imageHeight < 32; ++imageHeight) {
			cellX = (imageHeight & 8) << 3;
			width = (imageHeight & 1) * 191 + cellX;
			int green = ((imageHeight & 2) >> 1) * 191 + cellX;
			pixelIndex = ((imageHeight & 4) >> 2) * 191 + cellX;
			boolean shadowVariant = imageHeight >= 16;
			if(settings.anaglyph) {
				// Anaglyph (3D glasses) mode: combine the RGB channels via the
				// standard luminance weights into red/cyan channels.
				pixelRow = (pixelIndex * 30 + green * 59 + width * 11) / 100;
				alpha = (pixelIndex * 30 + green * 70) / 100;
				int blue = (pixelIndex * 30 + width * 70) / 100;
				pixelIndex = pixelRow;
				green = alpha;
				width = blue;
			}

			imageHeight += 2;
			if(shadowVariant) {
				// Shadow variants are darkened (quarter brightness).
				pixelIndex /= 4;
				green /= 4;
				width /= 4;
			}

			GL11.glColor4f((float)pixelIndex / 255.0F, (float)green / 255.0F, (float)width / 255.0F, 1.0F);
		}

	}

	/** Draws the given text with a dark shadow offset by one pixel down-right. */
	public final void drawStringWithShadow(String text, int x, int y, int colour) {
		this.renderString(text, x + 1, y + 1, colour, true);
		this.drawString(text, x, y, colour);
	}

	/** Draws the given text in the specified ARGB colour. */
	public final void drawString(String text, int x, int y, int colour) {
		this.renderString(text, x, y, colour, false);
	}

	/**
	 * Renders the string, handling the "&x" colour codes (following the formatter
	 * character '&') by inserting the appropriate shadow/colour display list.
	 */
	private void renderString(String text, int x, int y, int colour, boolean shadow) {
		if(text != null) {
			char[] chars = text.toCharArray();
			if(shadow) {
				// Darken the colour to approximate a shadow behind the glyph.
				colour = (colour & 16579836) >> 2;
			}

			GL11.glBindTexture(GL11.GL_TEXTURE_2D, this.fontTextureInt);
			float red = (float)(colour >> 16 & 255) / 255.0F;
			float green = (float)(colour >> 8 & 255) / 255.0F;
			float blue = (float)(colour & 255) / 255.0F;
			GL11.glColor4f(red, green, blue, 1.0F);
			this.buffer.clear();
			GL11.glPushMatrix();
			GL11.glTranslatef((float)x, (float)y, 0.0F);

			for(int index = 0; index < chars.length; ++index) {
				// Scan the "&x" colour-code pairs, skipping the '&' and replacing
				// them with a colour-list id; 256 + digit selects the colour list.
				for(; chars[index] == 38 && chars.length > index + 1; index += 2) {
					int colourIndex = "0123456789abcdef".indexOf(chars[index + 1]);
					if(colourIndex < 0 || colourIndex > 15) {
						colourIndex = 15;
					}

					this.buffer.put(this.fontDisplayLists + 256 + colourIndex + (shadow ? 16 : 0));
					if(this.buffer.remaining() == 0) {
						this.buffer.flip();
						GL11.glCallLists(this.buffer);
						this.buffer.clear();
					}
				}

				this.buffer.put(this.fontDisplayLists + chars[index]);
				if(this.buffer.remaining() == 0) {
					this.buffer.flip();
					GL11.glCallLists(this.buffer);
					this.buffer.clear();
				}
			}

			this.buffer.flip();
			GL11.glCallLists(this.buffer);
			GL11.glPopMatrix();
		}
	}

	/** Returns the total pixel width of the string using each glyph's advance width. */
	public final int getStringWidth(String text) {
		if(text == null) {
			return 0;
		} else {
			char[] chars = text.toCharArray();
			int totalWidth = 0;

			for(int index = 0; index < chars.length; ++index) {
				if(chars[index] == 38) {
					// '&' introduces a colour code for the next character; skip it.
					++index;
				} else {
					totalWidth += this.charWidth[chars[index]];
				}
			}

			return totalWidth;
		}
	}
}
