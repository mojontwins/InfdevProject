package net.minecraft.game.world.chunk;

/**
 * A 2-bits-per-fourth storage array: the metadata, skylight and blocklight planes of a single
 * 16&times;16&times;16 {@link Chunk} subchunk. Each subchunk needs 4096 values, but a 4-bit nibble
 * per block packs them into half the space (2048 bytes), at the cost of a little bit-twiddling
 * on every {@link #get} / {@link #set}.
 *
 * <p>The 4096 block cells are indexed by the same packed {@code (x << 8 | z << 4 | y)} scheme the
 * subchunk's block-byte array uses, with y local to the subchunk (0–15). Two nibbles share each
 * backing byte, so the byte index is the cell index &gt;&gt; 1 and the nibble-in-byte is the cell
 * index &#38; 1 (low nibble = even cells).
 */
final class NibbleArray {
	/** Bits per nibble stored. */
	private static final int NIBBLE_BITS = 4;
	/** Mask isolating one 4-bit nibble. */
	private static final int NIBBLE_MASK = 15;
	/** Bits to shift X within a 256-cell subchunk row (x &times; 16 cells wide). */
	private static final int LOCAL_X_SHIFT = 8;
	/** Bits to shift Z within a 16-cell row of a subchunk's planes. */
	private static final int LOCAL_Z_SHIFT = 4;

	/** The packed nibble data; each byte holds two cell nibbles (low cell on the low nibble). */
	public final byte[] data;

	public NibbleArray(int cellCount) {
		this.data = new byte[cellCount >> 1];
	}

	public NibbleArray(byte[] data) {
		this.data = data;
	}

	/** Reads the 4-bit value at the given subchunk-local block cell. */
	public final int get(int x, int y, int z) {
		int cellIndex = x << LOCAL_X_SHIFT | z << LOCAL_Z_SHIFT | y;
		int byteIndex = cellIndex >> 1;
		int nibbleOffset = cellIndex & 1;
		return nibbleOffset == 0 ? this.data[byteIndex] & NIBBLE_MASK : this.data[byteIndex] >> NIBBLE_BITS & NIBBLE_MASK;
	}

	/** Writes a 4-bit value into the given subchunk-local block cell, preserving the sibling nibble. */
	public final void set(int x, int y, int z, int value) {
		int cellIndex = x << LOCAL_X_SHIFT | z << LOCAL_Z_SHIFT | y;
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