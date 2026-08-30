package net.minecraft.client.gui;

import net.minecraft.client.render.Tessellator;
import org.lwjgl.opengl.GL11;

public class Gui {
	protected float zLevel = 0.0F;

	/** Draws a vertical colour gradient rectangle, blending between the top and bottom ARGB colours. */
	protected static void drawGradientRect(int x1, int y1, int x2, int y2, int topColour, int bottomColour) {
		float topAlpha = (float)(topColour >>> 24) / 255.0F;
		float topRed = (float)(topColour >> 16 & 255) / 255.0F;
		float topGreen = (float)(topColour >> 8 & 255) / 255.0F;
		float topBlue = (float)(topColour & 255) / 255.0F;
		float bottomAlpha = (float)(bottomColour >>> 24) / 255.0F;
		float bottomRed = (float)(bottomColour >> 16 & 255) / 255.0F;
		float bottomGreen = (float)(bottomColour >> 8 & 255) / 255.0F;
		float bottomBlue = (float)(bottomColour & 255) / 255.0F;
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glDisable(GL11.GL_ALPHA_TEST);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		Tessellator tessellator = Tessellator.instance;
		tessellator.startDrawingQuads();
		tessellator.setColorRGBA_F(topRed, topGreen, topBlue, topAlpha);
		tessellator.addVertex((double)x2, (double)y1, 0.0D);
		tessellator.addVertex((double)x1, (double)y1, 0.0D);
		tessellator.setColorRGBA_F(bottomRed, bottomGreen, bottomBlue, bottomAlpha);
		tessellator.addVertex((double)x1, (double)y2, 0.0D);
		tessellator.addVertex((double)x2, (double)y2, 0.0D);
		tessellator.draw();
		GL11.glDisable(GL11.GL_BLEND);
		GL11.glEnable(GL11.GL_ALPHA_TEST);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
	}

	/** Draws text centred horizontally; x is the centre point. */
	public static void drawCenteredString(FontRenderer fontRenderer, String text, int x, int y, int colour) {
		fontRenderer.drawStringWithShadow(text, x - fontRenderer.getStringWidth(text) / 2, y, colour);
	}

	/** Draws text with a shadow at the given position. */
	public static void drawString(FontRenderer fontRenderer, String text, int x, int y, int colour) {
		fontRenderer.drawStringWithShadow(text, x, y, colour);
	}

	/**
	 * Draws a region of a texture onto the screen. The source coordinates are
	 * given in texels and scaled by 1/256 (0.00390625) to UV coordinates.
	 */
	public final void drawTexturedModalRect(int x, int y, int srcX, int srcY, int dstWidth, int dstHeight) {
		Tessellator tessellator = Tessellator.instance;
		tessellator.startDrawingQuads();
		tessellator.addVertexWithUV((double)x, (double)(y + dstHeight), (double)this.zLevel, (double)((float)srcX * 0.00390625F), (double)((float)(srcY + dstHeight) * 0.00390625F));
		tessellator.addVertexWithUV((double)(x + dstWidth), (double)(y + dstHeight), (double)this.zLevel, (double)((float)(srcX + dstWidth) * 0.00390625F), (double)((float)(srcY + dstHeight) * 0.00390625F));
		tessellator.addVertexWithUV((double)(x + dstWidth), (double)y, (double)this.zLevel, (double)((float)(srcX + dstWidth) * 0.00390625F), (double)((float)srcY * 0.00390625F));
		tessellator.addVertexWithUV((double)x, (double)y, (double)this.zLevel, (double)((float)srcX * 0.00390625F), (double)((float)srcY * 0.00390625F));
		tessellator.draw();
	}
}
