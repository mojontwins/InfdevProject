package net.minecraft.game.world;

/** Immutable 3D position of a chunk in the world, identified by its x/z horizontal
 *  coordinates and a y coordinate (typically the block height). */
public final class ChunkPosition {
	public final int x;
	public final int y;
	public final int z;

	public ChunkPosition(int x, int y, int z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	/** Two chunk positions are equal when all three coordinates match exactly. */
	@Override
	public final boolean equals(Object obj) {
		if(obj instanceof ChunkPosition) {
			ChunkPosition other = (ChunkPosition)obj;
			return other.x == this.x && other.y == this.y && other.z == this.z;
		} else {
			return false;
		}
	}

	/** Hash combines the three coordinates with large prime-like multipliers so that
	 *  positions with different x/y/z values spread out across the int range, reducing
	 *  collisions when ChunkPositions are used as keys in hash structures. */
	@Override
	public final int hashCode() {
		return this.x * 8976890 + this.y * 981131 + this.z;
	}
}
