package net.minecraft.game.world.chunk;

import com.mojang.nbt.NBTTagCompound;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import net.minecraft.client.LoadingScreenRenderer;
import net.minecraft.game.world.World;

/**
 * The disk-backed layer of the chunk pipeline: a fixed-size cache of loaded chunks (1024 slots,
 * 32&times;32) that hands requests down to the generating provider underneath and serializes
 * chunks to the save folder when they fall out of the cache or the world is saved.
 *
 * <p>The cache is an open-addressed ring over the low 5 bits of each chunk coordinate: slot
 * {@code (chunkX & 31) | (chunkZ & 31) << 5}. Loading a new chunk evicts whatever currently
 * occupies its slot (saving it if modified). Because several coordinate pairs hash to the same
 * slot, the provider walks the world generation chain after a chunk is produced so that a newly
 * generated chunk's neighbours (and their great-grandparents) are populated only once their full
 * neighbourhood exists on disk.
 */
public final class ChunkProviderLoadOrGenerate implements IChunkProvider {
	/** Cache width in chunks — the 32&times;32 ring (1024 slots). */
	private static final int CACHE_MASK = 31;
	private static final int CACHE_BITS = 5;
	/** Coordinates of a fresh unloaded chunk, expressed as a {@code File} per lower-6-bit bucket. */
	private static final int FILE_BUCKET_MASK = 63;

	private IChunkProvider chunkProvider;
	private Chunk[] chunks = new Chunk[(CACHE_MASK + 1) * (CACHE_MASK + 1)];
	private File saveDirectory;
	private World worldObj;

	public ChunkProviderLoadOrGenerate(World world, File saveDirectory, IChunkProvider chunkProvider) {
		this.worldObj = world;
		this.chunkProvider = chunkProvider;
		this.saveDirectory = saveDirectory;
	}

	/**
	 * Whether the requested chunk is present in the cache <em>and</em> really is the chunk for
	 * these coordinates (the hash ring can hold a different chunk that maps to the same slot).
	 */
	@Override
	public final boolean chunkExists(int chunkX, int chunkZ) {
		int slot = chunkSlot(chunkX, chunkZ);
		Chunk cached = this.chunks[slot];
		return cached != null && cached.xPosition == chunkX && cached.zPosition == chunkZ;
	}

	/**
	 * Loads (from disk) or generates (from the underlying provider) the chunk at these
	 * coordinates and caches it, evicting the previous occupant of its slot. When a chunk is
	 * created fresh it is then handed to {@link #populate} once all four neighbours that form
	 * its decoration square are present, and the same check is re-run for the neighbouring
	 * coordinates so a frontier of generated chunks populates in the correct order.
	 */
	@Override
	public final Chunk provideChunk(int chunkX, int chunkZ) {
		int slot = chunkSlot(chunkX, chunkZ);
		if(!this.chunkExists(chunkX, chunkZ)) {
			if(this.chunks[slot] != null) {
				this.chunks[slot].unloadEntities();
				this.saveChunk(this.chunks[slot]);
			}

			Chunk chunk = this.loadChunk(chunkX, chunkZ);
			if(chunk == null) {
				chunk = this.chunkProvider.provideChunk(chunkX, chunkZ);
			}

			this.chunks[slot] = chunk;
			if(chunk != null) {
				chunk.loadEntities();
			}

			// The settle pass (trees/ores/water) of a chunk needs its whole neighbourhood
			// generated first. This chunk's own settlement runs once the (NE, N, E) square is
			// present, and the three neighbour corners are re-checked as their own neighbours
			// come into existence. Each clause re-enters provideChunk (which recurses) so a
			// generated frontier settles in the correct order as it fans out from the player.
			int cx = chunkX;
			int cz = chunkZ;
			if(!this.chunks[slot].isTerrainPopulated && this.chunkExists(cx + 1, cz + 1) && this.chunkExists(cx, cz + 1) && this.chunkExists(cx + 1, cz)) {
				this.populate(this, cx, cz);
			}

			cx = chunkX - 1;
			if(this.chunkExists(cx, cz) && !this.provideChunk(cx, cz).isTerrainPopulated && this.chunkExists(cx, cz + 1) && this.chunkExists(chunkX, cz + 1) && this.chunkExists(cx, cz)) {
				this.populate(this, cx, cz);
			}

			cx = chunkX;
			cz = chunkZ - 1;
			if(this.chunkExists(cx, cz) && !this.provideChunk(cx, cz).isTerrainPopulated && this.chunkExists(cx + 1, cz) && this.chunkExists(cx, cz) && this.chunkExists(cx + 1, chunkZ)) {
				this.populate(this, cx, cz);
			}

			cx = chunkX - 1;
			cz = chunkZ - 1;
			if(this.chunkExists(cx, cz) && !this.provideChunk(cx, cz).isTerrainPopulated && this.chunkExists(cx, cz) && this.chunkExists(chunkX, cz) && this.chunkExists(cx, chunkZ)) {
				this.populate(this, cx, cz);
			}
		}

		return this.chunks[slot];
	}

	/**
	 * Maps a (chunkX, chunkZ) pair onto the 1024-slot ring: the low 5 bits of each coordinate
	 * combine into a single cache slot.
	 */
	private static int chunkSlot(int chunkX, int chunkZ) {
		return chunkX & CACHE_MASK | (chunkZ & CACHE_MASK) << CACHE_BITS;
	}

	/**
	 * The save file for a chunk. Files live under the save folder in two base-36 sub-folders
	 * (x bucket / z bucket, each the low 6 bits) with the conventional {@code c.x.z.dat} name —
	 * this is the classic pre-Anvil ".mca" era layout.
	 */
	private File chunkFileForXZ(int chunkX, int chunkZ) {
		String fileName = "c." + Integer.toString(chunkX, 36) + "." + Integer.toString(chunkZ, 36) + ".dat";
		String xBucket = Integer.toString(chunkX & FILE_BUCKET_MASK, 36);
		String zBucket = Integer.toString(chunkZ & FILE_BUCKET_MASK, 36);
		File file = new File(this.saveDirectory, xBucket);
		file.mkdirs();
		file = new File(file, zBucket);
		file.mkdirs();
		return new File(file, fileName);
	}

	/** Reads a chunk back from disk, or null when the save file is missing/unreadable. */
	private Chunk loadChunk(int chunkX, int chunkZ) {
		File file = this.chunkFileForXZ(chunkX, chunkZ);
		if(file.exists()) {
			try {
				FileInputStream in = new FileInputStream(file);
				NBTTagCompound root = LoadingScreenRenderer.read(in);
				return Chunk.readChunkNBTData(this.worldObj, root.getCompoundTag("Level"));
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	/** Serializes a chunk to its save file, keeping the world's on-disk size guess accurate. */
	private void saveChunk(Chunk chunk) {
		File file = this.chunkFileForXZ(chunk.xPosition, chunk.zPosition);
		if(file.exists()) {
			this.worldObj.sizeOnDisk -= file.length();
		}

		try {
			FileOutputStream out = new FileOutputStream(file);
			NBTTagCompound root = new NBTTagCompound();
			NBTTagCompound level = new NBTTagCompound();
			root.setTag("Level", level);
			chunk.writeChunkNBTData(level);
			LoadingScreenRenderer.write(root, out);
			this.worldObj.sizeOnDisk += file.length();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Marks a chunk's terrain as fully settled, then delegates the actual tree/ore decoration to
	 * the underlying generating provider so the two providers share the one populate pass.
	 */
	@Override
	public final void populate(IChunkProvider chunkProvider, int chunkX, int chunkZ) {
		Chunk chunk = this.provideChunk(chunkX, chunkZ);
		if(!chunk.isTerrainPopulated) {
			chunk.isTerrainPopulated = true;
			this.chunkProvider.populate(chunkProvider, chunkX, chunkZ);
		}
	}

	/**
	 * Saves dirty chunks. When {@code unload} is true the whole cache is flushed; otherwise only
	 * up to two chunks per call are written, spreading the periodic autosave across frames.
	 */
	@Override
	public final void saveChunks(boolean unload) {
		int saved = 0;
		for(Chunk chunk : this.chunks) {
			if(chunk != null && chunk.needsSaving(unload)) {
				this.saveChunk(chunk);
				chunk.isModified = false;
				++saved;
				if(saved == 2 && !unload) {
					return;
				}
			}
		}
	}

	/**
	 * The {@code emptyList}-driven eviction stub of the original (it removed chunks from a list
	 * that was always empty, i.e. it never actually evicted anything and always reported false).
	 * The generating layer's own {@link IChunkProvider#unload100OldestChunks} is still consulted,
	 * so this ring simply never has anything of its own to evict.
	 */
	@Override
	public final boolean unload100OldestChunks() {
		return this.chunkProvider.unload100OldestChunks();
	}
}