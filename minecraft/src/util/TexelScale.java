package util;

/**
 * Converts texel-pixel coordinates into the normalised 0..1 UV space the
 * tessellator samples the atlas with. Per-atlas, because a taller atlas packs
 * the same texels into a smaller vertical fraction.
 */
public final class TexelScale {
	private TexelScale() {
	}

	public static float u(TextureAtlas atlas, float texels) {
		return texels / atlas.width;
	}

	public static float v(TextureAtlas atlas, float texels) {
		return texels / atlas.height;
	}

	public static double ud(TextureAtlas atlas, double texels) {
		return texels / atlas.width;
	}

	public static double vd(TextureAtlas atlas, double texels) {
		return texels / atlas.height;
	}
}