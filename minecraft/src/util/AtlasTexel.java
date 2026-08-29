package util;

/**
 * Splits a packed tile index into the pixel coordinates of its top-left corner
 * inside its atlas. The result is written into the static {@link #u}/{@link #v}
 * scratch pair, which the other atlas helpers reuse without allocating — the
 * render loop calls these many thousands of times a frame.
 */
public final class AtlasTexel {
	public static int u;
	public static int v;

	private AtlasTexel() {
	}

	/**
	 * Column of {@code tileIndex} becomes the U pixel, row the V pixel, both in
	 * multiples of {@link TextureAtlas#TILE}.
	 */
	public static void calc(int tileIndex, TextureAtlas atlas) {
		u = (tileIndex & (atlas.widthInTiles - 1)) << 4;
		v = tileIndex / atlas.widthInTiles << 4;
	}
}