package net.minecraft.game.world.mcregion;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Manages the set of open {@link RegionFile}s for a world: maps a {@code (regionX, regionZ)}
 * to its soft-referenced {@code RegionFile}, opening files on demand under the world's
 * {@code region/} folder and closing them all on flush. Chunk coordinates passed to the
 * stream accessors are <em>world</em> chunk coordinates; the region file and the 0..31 local
 * offset are derived here (see the pre-Anvil {@code r.x.z.mcr} layout and the McRegion local
 * index {@code (chunkZ & 31) << 5 | (chunkX & 31)}).
 */
public final class RegionFileCache {
	private static final Map<File, SoftReference<RegionFile>> cache = new HashMap<>();

	private RegionFileCache() {
	}

	/** Returns (opening if needed) the region file holding the given world chunk coordinates. */
	public static synchronized RegionFile getRegionFile(File saveDirectory, int chunkX, int chunkZ) {
		File regionFolder = new File(saveDirectory, "region");
		File regionFile = new File(regionFolder, "r." + (chunkX >> 5) + "." + (chunkZ >> 5) + ".mcr");
		SoftReference<RegionFile> reference = cache.get(regionFile);
		if(reference != null) {
			RegionFile existing = reference.get();
			if(existing != null) {
				return existing;
			}
		}

		if(!regionFolder.exists()) {
			regionFolder.mkdirs();
		}

		if(cache.size() >= 256) {
			closeRegionFiles();
		}

		RegionFile opened = new RegionFile(regionFile);
		cache.put(regionFile, new SoftReference<>(opened));
		return opened;
	}

	/** Closes and forgets every open region file (flush before world save/unload). */
	public static synchronized void closeRegionFiles() {
		Iterator<SoftReference<RegionFile>> iterator = cache.values().iterator();
		while(iterator.hasNext()) {
			SoftReference<RegionFile> reference = iterator.next();
			try {
				RegionFile regionFile = reference.get();
				if(regionFile != null) {
					regionFile.close();
				}
			} catch(IOException e) {
				e.printStackTrace();
			}
		}
		cache.clear();
	}

	/** Returns a decompressed stream over a stored chunk, or null when the region lacks it. */
	public static DataInputStream getChunkInputStream(File saveDirectory, int chunkX, int chunkZ) {
		return getRegionFile(saveDirectory, chunkX, chunkZ).getChunkDataInputStream(chunkX & 31, chunkZ & 31);
	}

	/** Returns a zlib-compressing stream for storing a chunk; commit happens on stream close. */
	public static DataOutputStream getChunkOutputStream(File saveDirectory, int chunkX, int chunkZ) {
		return getRegionFile(saveDirectory, chunkX, chunkZ).getChunkDataOutputStream(chunkX & 31, chunkZ & 31);
	}

	/** Whether the region file that would hold this chunk actually exists on disk. */
	public static boolean regionFileExists(File saveDirectory, int chunkX, int chunkZ) {
		return new File(new File(saveDirectory, "region"), "r." + (chunkX >> 5) + "." + (chunkZ >> 5) + ".mcr").exists();
	}
}
