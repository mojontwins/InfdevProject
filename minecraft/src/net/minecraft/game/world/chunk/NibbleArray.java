package net.minecraft.game.world.chunk;

/**
 * A 2-bits-per-fourth storage array: the metadata, skylight and blocklight planes of a
 * {@link Chunk}. Each 16&times;16&times;128 block column of a chunk needs 49152 values, but a 4-bit
 * nibble per block packs them into half the space (24576 bytes), at the cost of a little
 * bit-twiddling on every {@link #get} / {@link #set}.
 *
 * <p>The 49152 block cells are indexed by the same packed {@code (x << 11 | z << 7 | y)} scheme
 * the block-byte array uses; two nibbles share each backing byte, so the byte index is the cell
 * index &gt;&gt; 1 and the nibble-in-byte is cell index &#38; 1 (low nibble = even cells).
 */
final class NibbleArray {
	/** Bits per nibble stored. */
	private static final int NIBBLE_BITS = 4;
	/** Mask isolating one 4-bit nibble. */
	private static final int NIBBLE_MASK = 15;

	/** The packed nibble data; each byte holds two cell nibbles (low cell on the low nibble). */
	public final byte[] data;

	public NibbleArray(int cellCount) {
		this.data = new byte[cellCount >> 1];
	}

	public NibbleArray(byte[] data) {
		this.data = data;
	}

	/** Reads the 4-bit value at the given chunk-local block cell. */
	public final int get(int x, int y, int z) {
		int cellIndex = x << 11 | z << 7 | y;
		int byteIndex = cellIndex >> 1;
		int nibbleOffset = cellIndex & 1;
		return nibbleOffset == 0 ? this.data[byteIndex] & NIBBLE_MASK : this.data[byteIndex] >> NIBBLE_BITS & NIBBLE_MASK;
	}

	/** Writes a 4-bit value into the given chunk-local block cell, preserving the sibling nibble. */
	public final void set(int x, int y, int z, int value) {
		int cellIndex = x << 11 | z << 7 | y;
		int byteIndex = cellIndex >> 1;
		int nibbleOffset = cellIndex & 1;
		if(nibbleOffset == 0) {
			this.data[byteIndex] = (byte)(this.data[byteIndex] & ~NIBBLE_MASK | value & NIBBLE_MASK);
		} else {
			this.data[byteIndex] = (byte)(this.data[byteIndex] & NIBBLE_MASK | (value & NIBBLE_MASK) << NIBBLE_BITS);
		}
	}

	/**
	 * Whether the backing buffer is present. A {@code null} (missing) plane means "not yet
	 * initialized" — {@link Chunk#readChunkNBTData} uses this to spot and regenerate light.
	 */
	public final boolean isValid() {
		return this.data != null;
	}
}