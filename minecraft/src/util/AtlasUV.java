package util;

/**
 * Normalised quad corners of a tile for the two texture atlases, written into
 * a static scratch quad ({@link #u1 v1 u2 v2}) to stay allocation-free in the
 * render loop. {@code calc} covers a whole tile with the bleed-guard inset
 * ({@link TextureAtlas#tileSpan}); {@code calcPixels} covers an arbitrary
 * pixel rectangle — the thin slabs and strips of torch heads, lever handles,
 * ladder rails and next-row fire frames.
 */
public final class AtlasUV {
	public static double u1;
	public static double v1;
	public static double u2;
	public static double v2;

	private AtlasUV() {
	}

	/** Full-tile quad for a plain tile index. Also leaves {@link AtlasTexel#u v} set. */
	public static void calc(int tileIndex, TextureAtlas atlas) {
		AtlasTexel.calc(tileIndex, atlas);
		u1 = TexelScale.ud(atlas, AtlasTexel.u);
		v1 = TexelScale.vd(atlas, AtlasTexel.v);
		u2 = TexelScale.ud(atlas, AtlasTexel.u + atlas.tileSpan);
		v2 = TexelScale.vd(atlas, AtlasTexel.v + atlas.tileSpan);
	}

	/** Quad covering the pixel rectangle [{@code uPx}, {@code uPx+spanUPx}) x [{@code vPx}, {@code vPx+spanVPx}). */
	public static void calcPixels(int uPx, int vPx, float spanUPx, float spanVPx, TextureAtlas atlas) {
		u1 = TexelScale.ud(atlas, (double)uPx);
		v1 = TexelScale.vd(atlas, (double)vPx);
		u2 = TexelScale.ud(atlas, (double)uPx + spanUPx);
		v2 = TexelScale.vd(atlas, (double)vPx + spanVPx);
	}
}