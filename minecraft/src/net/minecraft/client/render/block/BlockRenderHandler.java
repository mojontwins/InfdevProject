package net.minecraft.client.render.block;

import net.minecraft.client.render.RenderBlocks;
import net.minecraft.game.world.block.Block;

/**
 * Strategy for one block render type (see {@link Block#getRenderType()}).
 *
 * Each render type has its own dedicated handler class that owns all of that
 * type's geometry, so adding a new render type only means registering one more
 * handler in {@link BlockRenderType} — no change to {@link RenderBlocks}.
 */
public interface BlockRenderHandler {
	/** Renders the block in the world. Returns true when at least one face was emitted. */
	boolean renderBlock(RenderBlocks renderBlocks, Block block, int x, int y, int z);

	/**
	 * Renders the block as a 3D preview inside an inventory slot. The
	 * {@code metadata} value lets colour-aware blocks (cloth) pick a swatch, and
	 * {@code brightness} (in [0, 1]) scales the result to the surrounding light,
	 * so a held or dropped block dims with the ambient light level. Defaults to
	 * nothing.
	 */
	default void renderBlockOnInventory(RenderBlocks renderBlocks, Block block, int metadata, float brightness) {
	}
}