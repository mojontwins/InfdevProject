package net.minecraft.game.world.block;

/**
 * An immutable pairing of a {@link Block} and its metadata. Captures the two
 * values {@link World#getBlockId} and {@link World#getBlockMetadata} return for
 * a single cell, so code can carry both around together instead of two ints.
 * {@link #air} is the well-known empty state (block 0, metadata 0).
 */
public final class BlockState {
	/** The empty cell: block id 0, metadata 0. */
	public static final BlockState air = new BlockState(0, 0);

	private final Block block;
	private final int metadata;

	public BlockState(Block block, int metadata) {
		this.block = block;
		this.metadata = metadata;
	}

	public BlockState(int blockID, int metadata) {
		this(Block.blocksList[blockID], metadata);
	}

	public final Block getBlock() {
		return this.block;
	}

	public final int getMetadata() {
		return this.metadata;
	}

	/** The block id, or 0 when the block is absent (as with {@link #air}). */
	public final int getBlockID() {
		return this.block == null ? 0 : this.block.blockID;
	}

	@Override
	public final boolean equals(Object obj) {
		if (this == obj) {
			return true;
		} else if (!(obj instanceof BlockState)) {
			return false;
		}

		BlockState other = (BlockState) obj;
		int thisID = this.block == null ? 0 : this.block.blockID;
		int otherID = other.block == null ? 0 : other.block.blockID;
		return thisID == otherID && this.metadata == other.metadata;
	}

	@Override
	public final int hashCode() {
		return 31 * this.getBlockID() + this.metadata;
	}

	@Override
	public String toString() {
		return "[" + this.getBlockID() + ":" + this.metadata + "]";
	}
}
