package util;

import net.minecraft.game.world.block.Block;

/**
 * Normalised quad corners for the vertical side faces of a block, driven by the
 * block's current bounds. U spans the whole tile (with the bleed-guard inset);
 * V spans only the block's visible height, so thin blocks such as snow layers,
 * water, doors and stairs stretch the tile over exactly what is seen, falling
 * back to the full tile on full-cube blocks. This is the rule the four side
 * face emitters in {@code RenderBlocks} share.
 */
public final class AtlasUVBounds {
	public static double u1;
	public static double v1;
	public static double u2;
	public static double v2;

	private AtlasUVBounds() {
	}

	public static void calc(Block block, int tileIndex) {
		TextureAtlas terrain = TextureAtlas.TERRAIN;
		AtlasTexel.calc(tileIndex, terrain);
		double uPx = AtlasTexel.u;
		double vPx = AtlasTexel.v;
		u1 = TexelScale.ud(terrain, uPx);
		u2 = TexelScale.ud(terrain, uPx + terrain.tileSpan);
		if(block.minY >= 0.0D && block.maxY <= 1.0D) {
			v1 = TexelScale.vd(terrain, vPx + block.minY * terrain.tileSpan);
			v2 = TexelScale.vd(terrain, vPx + block.maxY * terrain.tileSpan);
		} else {
			v1 = TexelScale.vd(terrain, vPx);
			v2 = TexelScale.vd(terrain, vPx + terrain.tileSpan);
		}
	}
}