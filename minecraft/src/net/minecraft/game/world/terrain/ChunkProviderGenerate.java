package net.minecraft.game.world.terrain;

import java.util.Random;
import net.minecraft.game.world.World;
import net.minecraft.game.world.WorldOptions;
import net.minecraft.game.world.biome.BiomeGenerator;
import net.minecraft.game.world.chunk.Chunk;
import net.minecraft.game.world.chunk.IChunkProvider;

/**
 * The procedural terrain provider. It builds every chunk from scratch out of
 * layered fractal noise rather than loading it from disk. Generation is split
 * into named stages so a subclass can customise one phase without touching the
 * others:
 *
 * <ol>
 *   <li><b>Terrain</b> - {@link #generateTerrain} fills the raw stone/water
 *       volume using a coarse noise height field produced by
 *       {@link #initializeNoiseField}.</li>
 *   <li><b>Surface</b> - {@link #replaceBlocks} carves the per-column surface
 *       layer (grass, dirt, sand, gravel) onto the top of the stone volume.</li>
 *   <li><b>Decoration</b> - {@link #populate} scatters ore veins and trees. It
 *       deliberately lives outside {@link #provideChunk}: the chunk pipeline
 *       invokes it only once every neighbour needed by the settlement square has
 *       been generated, so the decoration always sees a complete world.</li>
 * </ol>
 *
 * <p>{@link #provideChunk} is the fixed template shared by all {@link
 * ChunkProviderGenerate} subclasses: it re-seeds the chunk-local {@link Random}
 * (so the same chunk coordinates always produce the same terrain), sets up the
 * block buffer and then hands two stages off. {@link ChunkProviderGenerate420}
 * is the concrete generator for this version's world format.
 */
public abstract class ChunkProviderGenerate implements IChunkProvider {
	/** The world the generated chunks are attached to. */
	protected final World worldObj;
	/** The shared generator RNG; re-seeded once per chunk and per populate pass. */
	protected final Random rand;
	/** The world generation options chosen at world creation, held for future passes. */
	protected final WorldOptions worldOptions;

	public ChunkProviderGenerate(World world, long seed, WorldOptions worldOptions) {
		this.worldObj = world;
		this.rand = new Random(seed);
		this.worldOptions = worldOptions;
	}

	/**
	 * Generates the chunk at these coordinates. Follows the original build's
	 * phase order: the per-chunk {@link Random} seed is fixed first, then the
	 * raw terrain volume comes from {@link #generateTerrain}, the surface layer
	 * from {@link #replaceBlocks}, and finally the chunk's height map is
	 * regenerated so that lighting and spawning see the fresh terrain.
	 */
	@Override
	public final Chunk provideChunk(int chunkX, int chunkZ) {
		this.rand.setSeed((long) chunkX * 341873128712L + (long) chunkZ * 132897987541L);
		byte[] blocks = new byte[-Short.MIN_VALUE];
		// The chunk is built empty first (so it can hold the biome grid), the raw terrain volume
		// is generated into the flat buffer, and only then is the buffer loaded into the chunk's
		// subchunks — generateTerrain and replaceBlocks write to the flat buffer, so loading must
		// happen after both.
		Chunk chunk = new Chunk(this.worldObj, chunkX, chunkZ);
		// Stash the biome grid before the surface pass: replaceBlocks consults
		// the per-column biome, and the grid is later persisted with the chunk.
		this.fillBiomeArray(chunk, chunkX, chunkZ);
		this.generateTerrain(chunkX, chunkZ, blocks);
		this.replaceBlocks(chunkX, chunkZ, blocks, chunk);
		chunk.loadFlatBlocks(blocks);
		chunk.generateHeightMap();
		return chunk;
	}

	/**
	 * Fills the chunk's 16&times;16 biome grid from the world type's
	 * {@code BiomeProvider}, one id per column.
	 */
	protected final void fillBiomeArray(Chunk chunk, int chunkX, int chunkZ) {
		BiomeGenerator[] biomes =
				this.worldObj.worldType.getBiomeProvider().getBiomes(chunkX << 4, chunkZ << 4, 16, 16);
		for(int x = 0; x < 16; ++x) {
			for(int z = 0; z < 16; ++z) {
				chunk.setBiome(x, z, biomes[z << 4 | x].getBiomeID());
			}
		}
	}

	@Override
	public final boolean chunkExists(int chunkX, int chunkZ) {
		// This provider never owns cached chunks; existence is answered by the
		// disk-backed layer above it.
		return true;
	}

	/**
	 * Fills {@code blocks} with the raw terrain volume: the coarse noise height
	 * field (see {@link #initializeNoiseField}) tri-linearly up-sampled to full
	 * resolution, stamping stone above the surface and still water below sea
	 * level.
	 */
	protected abstract void generateTerrain(int chunkX, int chunkZ, byte[] blocks);

	/**
	 * Replaces the top of the plain stone volume with the world's surface layer.
	 * Subclasses walk the chunk's columns, consulting the per-column biome (read
	 * from {@code chunk}) and the surface noise, and call the biome's own surface
	 * replacement. This is where the version's beach/gravel/dirt noise is
	 * consulted.
	 *
	 * @param chunkX the chunk's block-coordinate origin along x (&times;16)
	 * @param chunkZ the chunk's block-coordinate origin along z (&times;16)
	 * @param blocks the chunk's block id buffer
	 * @param chunk the chunk being generated (its biome grid is already filled)
	 */
	protected abstract void replaceBlocks(int chunkX, int chunkZ, byte[] blocks, Chunk chunk);

	/**
	 * Samples and blends the coarse 5x5x17 noise field for the chunk at these
	 * coordinates, leaving the result in the field used by
	 * {@link #generateTerrain}.
	 */
	protected abstract void initializeNoiseField(int chunkX, int chunkZ);

	/**
	 * Decoration stage: scatters the ore veins and trees. Kept out of
	 * {@link #provideChunk} so that the chunk pipeline can defer it until the
	 * chunk's whole settlement square exists and only ever run it once; the
	 * pipeline also carries the {@code chunkProvider} through to hand this stage
	 * down the chain.
	 */
	@Override
	public abstract void populate(IChunkProvider chunkProvider, int chunkX, int chunkZ);

	@Override
	public final void saveChunks(boolean unload) {
		// Terrain is fully procedural: the loading layer above owns persistence.
	}

	@Override
	public final boolean unload100OldestChunks() {
		// Nothing to evict on the procedural side; the emptyList stub of the
		// original always reported false.
		return false;
	}
}