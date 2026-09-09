package net.minecraft.game.world.mcregion;

import java.io.ByteArrayOutputStream;

/**
 * Accumulates one chunk's zlib-deflated NBT in memory and, on {@link #close}, hands the raw
 * payload bytes to its {@link RegionFile} so it can be stamped with the length/compression
 * wrapper and written into the region, reusing old sectors or growing the file as needed.
 */
final class RegionFileChunkBuffer extends ByteArrayOutputStream {
	private final RegionFile regionFile;
	private final int chunkX;
	private final int chunkZ;

	RegionFileChunkBuffer(RegionFile regionFile, int chunkX, int chunkZ) {
		super(8096);
		this.regionFile = regionFile;
		this.chunkX = chunkX;
		this.chunkZ = chunkZ;
	}

	@Override
	public void close() {
		this.regionFile.write(this.chunkX, this.chunkZ, this.buf, this.count);
	}
}
