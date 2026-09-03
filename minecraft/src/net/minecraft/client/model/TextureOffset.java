package net.minecraft.client.model;

/**
 * A named offset into a model's texture atlas. Used by {@link ModelBase} to
 * remember texture coordinates for sub-parts (e.g. a head overlay on a
 * head), and looked up by {@link ModelRenderer#addBox} when the caller asks
 * for a sub-box by name rather than by raw {@code (u, v)} pair.
 *
 * <p>Ported from r1.2.5; in the original the two fields are named
 * {@code field_40734_a} and {@code field_40733_b} under MCP's mid-2010s
 * obfuscation. The descriptive names {@code textureOffsetX/Y} match the
 * equivalent pair on {@link ModelRenderer}.
 */
public class TextureOffset {
	public final int textureOffsetX;
	public final int textureOffsetY;

	public TextureOffset(int textureOffsetX, int textureOffsetY) {
		this.textureOffsetX = textureOffsetX;
		this.textureOffsetY = textureOffsetY;
	}
}
