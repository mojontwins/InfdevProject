package net.minecraft.client.render;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.game.world.block.tileentity.TileEntity;

/**
 * Base class for one special tile-entity renderer (today that means signs).
 * Each concrete subclass knows how to draw one {@link TileEntity} type and is
 * registered on the {@link TileEntityRenderer} singleton, which hands itself in
 * via {@link #setTileEntityRenderer} so textures, fonts and camera state can be
 * reached while rendering.
 */
public abstract class TileEntitySpecialRenderer {
	/** The owning registry; provides the render engine, font and camera state. */
	protected TileEntityRenderer tileEntityRenderer;

	/**
	 * Draws the given tile entity at the world-space offset (x, y, z),
	 * interpolating any animation by {@code partialTick}.
	 */
	public abstract void renderTileEntityAt(TileEntity tileEntity, double x, double y, double z, float partialTick);

	/** Binds the named texture through the owning render engine. */
	protected void bindTextureByName(String textureName) {
		RenderEngine renderEngine = this.tileEntityRenderer.renderEngine;
		RenderEngine.bindTexture(renderEngine.getTexture(textureName));
	}

	/** Records the owning registry so textures, fonts and camera state can be reached. */
	public void setTileEntityRenderer(TileEntityRenderer renderer) {
		this.tileEntityRenderer = renderer;
	}

	/** Returns the font renderer shared by the whole render pipeline. */
	public FontRenderer getFontRenderer() {
		return this.tileEntityRenderer.getFontRenderer();
	}
}