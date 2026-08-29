package net.minecraft.game.world.chunk;

/**
 * A source of {@link Chunk} data. Minecraft's chunk pipeline is a thin chain of these providers:
 * a loading provider ({@link ChunkProviderLoadOrGenerate}) sits on top of a generating provider
 * ({@code ChunkProviderGenerate}), which itself can hand populating/decoration duty to lower
 * providers. Each layer satisfies the interface by either serving what it has or delegating down.
 */
public interface IChunkProvider {
	/** Whether the chunk at these chunk coordinates is already loaded. */
	boolean chunkExists(int chunkX, int chunkZ);

	/** Loads/creates the chunk at these chunk coordinates and returns it (never null here). */
	Chunk provideChunk(int chunkX, int chunkZ);

	/**
	 * Fills in the interesting detail of a chunk (ores, trees, water/lava) once its raw
	 * terrain has been generated and all its neighbours exist. {@code chunkProvider} is the
	 * caller walking the neighbour chain; coordinates are in chunk units.
	 */
	void populate(IChunkProvider chunkProvider, int chunkX, int chunkZ);

	/**
	 * Persists dirty chunks. When {@code unload} is true every chunk that needs saving is
	 * written; when false only a couple are flushed per call (spreading the disk cost).
	 */
	void saveChunks(boolean unload);

	/** Attempts to evict old chunks; returns whether any remain to be evicted. */
	boolean unload100OldestChunks();
}