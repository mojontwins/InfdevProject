package net.minecraft.game.world.mcregion;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

/**
 * A single McRegion container file ({@code r.<regionX>.<regionZ>.mcr}). Each file holds up to
 * 32&times;32 = 1024 chunks belonging to one 512&times;512-block region of the world, sharing an
 * 8 KiB header (a 1024-entry location table then a 1024-entry timestamp table) followed by
 * 4 KiB-sector-aligned chunk payloads.
 *
 * <p>The header layout is the standard pre-Anvil one: entry {@code i = localX + localZ*32} holds,
 * in the location table, a big-endian int whose top 24 bits are the start sector (in 4 KiB units)
 * and low 8 bits the sector count; {@code 0} means the chunk is not present. A chunk payload is a
 * 4-byte length, a 1-byte compression marker (1 = gzip, 2 = zlib) and the NBT bytes, padded to a
 * whole number of sectors. New chunks are written zlib-compressed (marker 2); existing gzip
 * payloads (marker 1) are still read back.
 *
 * <p>The {@code chunkX}/{@code chunkZ} arguments to {@link #getChunkDataInputStream} and
 * {@link #getChunkDataOutputStream} are <em>local</em> coordinates in {@code 0..31} (the offset
 * into the region), already masked by the caller.
 */
final class RegionFile {
	private static final byte[] EMPTY_SECTOR = new byte[4096];
	private final RandomAccessFile dataFile;
	private final int[] offsets = new int[1024];
	private final int[] chunkTimestamps = new int[1024];
	private List<Boolean> sectorFree;
	private int sizeDelta;

	RegionFile(File file) {
		this.sizeDelta = 0;
		try {
			this.dataFile = new RandomAccessFile(file, "rw");
			if(this.dataFile.length() < 4096L) {
				for(int i = 0; i < 1024; ++i) {
					this.dataFile.writeInt(0);
				}
				for(int i = 0; i < 1024; ++i) {
					this.dataFile.writeInt(0);
				}
				this.sizeDelta += 8192;
			}

			if((this.dataFile.length() & 4095L) != 0L) {
				for(int i = 0; (long)i < (this.dataFile.length() & 4095L); ++i) {
					this.dataFile.write(0);
				}
			}

			int sectorCount = (int)this.dataFile.length() / 4096;
			this.sectorFree = new ArrayList<>(sectorCount);
			for(int i = 0; i < sectorCount; ++i) {
				this.sectorFree.add(Boolean.TRUE);
			}
			this.sectorFree.set(0, Boolean.FALSE);
			this.sectorFree.set(1, Boolean.FALSE);

			this.dataFile.seek(0L);
			for(int i = 0; i < 1024; ++i) {
				int offset = this.dataFile.readInt();
				this.offsets[i] = offset;
				if(offset != 0 && (offset >> 8) + (offset & 255) <= this.sectorFree.size()) {
					for(int j = 0; j < (offset & 255); ++j) {
						this.sectorFree.set((offset >> 8) + j, Boolean.FALSE);
					}
				}
			}
			for(int i = 0; i < 1024; ++i) {
				this.chunkTimestamps[i] = this.dataFile.readInt();
			}
		} catch(IOException e) {
			throw new RuntimeException("Failed to open McRegion file " + file, e);
		}
	}

	/** Returns the uncompressed byte delta this region added since the last query. */
	synchronized int getSizeDelta() {
		int delta = this.sizeDelta;
		this.sizeDelta = 0;
		return delta;
	}

	/** Returns a decompressed stream over a stored chunk, or null when absent/invalid. */
	synchronized DataInputStream getChunkDataInputStream(int chunkX, int chunkZ) {
		if(this.outOfBounds(chunkX, chunkZ)) {
			return null;
		}
		try {
			int offset = this.getOffset(chunkX, chunkZ);
			if(offset == 0) {
				return null;
			}
			int startSector = offset >> 8;
			int sectorCount = offset & 255;
			if(startSector + sectorCount > this.sectorFree.size()) {
				return null;
			}
			this.dataFile.seek(startSector * 4096L);
			int length = this.dataFile.readInt();
			if(length > 4096 * sectorCount) {
				return null;
			}
			byte compression = this.dataFile.readByte();
			byte[] payload = new byte[length - 1];
			this.dataFile.read(payload);
			if(compression == 1) {
				return new DataInputStream(new GZIPInputStream(new ByteArrayInputStream(payload)));
			}
			if(compression == 2) {
				return new DataInputStream(new InflaterInputStream(new ByteArrayInputStream(payload)));
			}
			return null;
		} catch(IOException e) {
			return null;
		}
	}

	/** Returns a zlib-compressing output stream that commits the chunk on close. */
	DataOutputStream getChunkDataOutputStream(int chunkX, int chunkZ) {
		if(this.outOfBounds(chunkX, chunkZ)) {
			return null;
		}
		return new DataOutputStream(new DeflaterOutputStream(new RegionFileChunkBuffer(this, chunkX, chunkZ)));
	}

	/** Stores a chunk's deflated NBT payload (without the length/compression wrapper) into the file. */
	protected synchronized void write(int chunkX, int chunkZ, byte[] payload, int length) {
		try {
			int offset = this.getOffset(chunkX, chunkZ);
			int startSector = offset >> 8;
			int sectorCount = offset & 255;
			int neededSectors = (length + 5) / 4096 + 1;
			if(neededSectors >= 256) {
				return;
			}

			if(startSector != 0 && sectorCount == neededSectors) {
				this.writeSectors(startSector, payload, length);
			} else {
				for(int i = 0; i < sectorCount; ++i) {
					this.sectorFree.set(startSector + i, Boolean.TRUE);
				}

				int freeStart = this.sectorFree.indexOf(Boolean.TRUE);
				int run = 0;
				if(freeStart != -1) {
					for(int i = freeStart; i < this.sectorFree.size(); ++i) {
						if(run != 0) {
							if(this.sectorFree.get(i)) {
								++run;
							} else {
								run = 0;
							}
						} else if(this.sectorFree.get(i)) {
							freeStart = i;
							run = 1;
						}
						if(run >= neededSectors) {
							break;
						}
					}
				}

				if(run >= neededSectors) {
					startSector = freeStart;
					this.setOffset(chunkX, chunkZ, startSector << 8 | neededSectors);
					for(int i = 0; i < neededSectors; ++i) {
						this.sectorFree.set(startSector + i, Boolean.FALSE);
					}
					this.writeSectors(startSector, payload, length);
				} else {
					this.dataFile.seek(this.dataFile.length());
					startSector = this.sectorFree.size();
					for(int i = 0; i < neededSectors; ++i) {
						this.dataFile.write(EMPTY_SECTOR);
						this.sectorFree.add(Boolean.FALSE);
					}
					this.sizeDelta += 4096 * neededSectors;
					this.writeSectors(startSector, payload, length);
					this.setOffset(chunkX, chunkZ, startSector << 8 | neededSectors);
				}
			}

			this.setChunkTimestamp(chunkX, chunkZ, (int)(System.currentTimeMillis() / 1000L));
		} catch(IOException e) {
			e.printStackTrace();
		}
	}

	/** Writes the length/compression wrapper + payload at the given sector. Always zlib (marker 2). */
	private void writeSectors(int startSector, byte[] payload, int length) throws IOException {
		this.dataFile.seek(startSector * 4096L);
		this.dataFile.writeInt(length + 1);
		this.dataFile.writeByte(2);
		this.dataFile.write(payload, 0, length);
	}

	private boolean outOfBounds(int chunkX, int chunkZ) {
		return chunkX < 0 || chunkX >= 32 || chunkZ < 0 || chunkZ >= 32;
	}

	private int getOffset(int chunkX, int chunkZ) {
		return this.offsets[chunkX + chunkZ * 32];
	}

	boolean isChunkSaved(int chunkX, int chunkZ) {
		return this.getOffset(chunkX, chunkZ) != 0;
	}

	private void setOffset(int chunkX, int chunkZ, int offset) throws IOException {
		this.offsets[chunkX + chunkZ * 32] = offset;
		this.dataFile.seek((long)((chunkX + chunkZ * 32) * 4));
		this.dataFile.writeInt(offset);
	}

	private void setChunkTimestamp(int chunkX, int chunkZ, int timestamp) throws IOException {
		this.chunkTimestamps[chunkX + chunkZ * 32] = timestamp;
		this.dataFile.seek((long)(4096 + (chunkX + chunkZ * 32) * 4));
		this.dataFile.writeInt(timestamp);
	}

	synchronized void close() throws IOException {
		this.dataFile.close();
	}
}
