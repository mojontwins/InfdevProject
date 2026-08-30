package net.minecraft.game.world;

/**
 * Represents a scheduled tick entry for a block in the world.
 * 
 * Stores the block's position (x, y, z), its block ID, and the time at which
 * it should be ticked next in the game's block tick scheduling system.
 */
public final class NextTickListEntry {
	public int xCoord;
	public int yCoord;
	public int zCoord;
	public int blockID;
	public int scheduledTime;

	public NextTickListEntry(int xCoord, int yCoord, int zCoord, int blockID) {
		this.xCoord = xCoord;
		this.yCoord = yCoord;
		this.zCoord = zCoord;
		this.blockID = blockID;
	}
}
