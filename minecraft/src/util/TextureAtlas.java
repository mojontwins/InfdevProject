package util;

/**
 * The texture atlases the client stitches tiles from. Resizing an atlas is a
 * two-number change: edit the {@link #width} and {@link #height} of the
 * matching constant below — every UV calculation in the renderers reads its
 * scale and tile span from here, so nothing else needs to touch the atlas.
 *
 * <p>The tile index layout assumed by {@link AtlasTexel} packs one tile per
 * row (16 texels per tile); wider atlases can't be expressed by this layout
 * without re-packing tiles, but taller ones (the common case, e.g. a 256x512
 * terrain) work without any other change.
 */
public enum TextureAtlas {
	/** The block/terrain atlas ({@code terrain.png}). */
	TERRAIN(256, 256),
	/** The item icon atlas ({@code gui/items.png}). */
	ITEMS(256, 256);

	/** Edge length of a single tile, in texels. */
	public static final int TILE = 16;
	/** Inset per tile edge so neighbouring tiles never bleed into each other. */
	public static final float TILE_INSET = 0.01F;

	public final int width;
	public final int height;
	public final int widthInTiles;
	public final int heightInTiles;
	/** Effective tile span in texels ({@link #TILE} - {@link #TILE_INSET}). */
	public final float tileSpan;

	private TextureAtlas(int width, int height) {
		this.width = width;
		this.height = height;
		this.widthInTiles = width / TILE;
		this.heightInTiles = height / TILE;
		this.tileSpan = TILE - TILE_INSET;
	}
}