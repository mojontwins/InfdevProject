package net.minecraft.game.world.biome;

import java.util.Random;
import net.minecraft.game.world.World;
import net.minecraft.game.world.block.Block;

/**
 * Answers the question <em>"what terrain does this biome make?"</em> A
 * {@code BiomeGenerator} owns the surface replacement and decoration behaviour
 * for a single biome. Each chunk stores a grid of biome ids (see
 * {@link net.minecraft.game.world.chunk.Chunk#setBiome}), and generation asks
 * the matching {@code BiomeGenerator} to stamp that biome's surface and scatter
 * its decorations.
 *
 * <p>The surface pass runs per column: {@link #replaceBlocksForBiomeColumn} is
 * invoked once for every (x, z) of a chunk during
 * {@code ChunkProviderGenerate420.replaceBlocks}. The chunk provider computes the
 * per-column terrain noise (beach/gravel/dirt depth) with its own noise
 * generators and <em>passes those values in</em> rather than handing the biome
 * the generators, so the biome never owns the load-bearing noise state.
 *
 * <p>Decoration is driven by the center biome of a chunk: the provider picks the
 * biome at the chunk's middle column and calls {@link #populateOres} (the ore
 * veins) and then {@link #decorate} (trees and anything else).
 *
 * <p>{@code topBlock} and {@code fillerBlock} default to the timeless surface
 * pairing of grass over dirt ({@link Block#grass}/{@link Block#dirt}) and are
 * overridable by a biome that wants different surface blocks or materials.
 */
public abstract class BiomeGenerator {
	/** The byte id this biome is stored under in a chunk's biome array. */
	public abstract int getBiomeID();

	/** The surface block at the top of a column (defaults to grass). */
	public int topBlock() {
		return Block.grass.blockID;
	}

	/** The block just under the surface (defaults to dirt). */
	public int fillerBlock() {
		return Block.dirt.blockID;
	}

	/**
	 * Replaces the surface of one (x, z) column of the chunk at the given
	 * coordinates, writing directly into {@code blocks}. The terrain noise
	 * values for this column are computed by the chunk provider and passed in.
	 *
	 * @param world the world being generated
	 * @param rand the chunk's re-seeded generator RNG
	 * @param chunkX the chunk's block-coordinate origin along x (&times;16)
	 * @param chunkZ the chunk's block-coordinate origin along z (&times;16)
	 * @param x chunk-local column x (0..15)
	 * @param z chunk-local column z (0..15)
	 * @param blocks the chunk's block id buffer (indexed x&lt;&lt;11 | z&lt;&lt;7 | y)
	 * @param seaLevel the world height below which exposed cells become water
	 * @param sandBeach whether this column is a sand beach (computed by provider)
	 * @param gravelBed whether this column is a gravel bed (computed by provider)
	 * @param dirtDepth how many filler blocks to bury under the surface
	 */
	public abstract void replaceBlocksForBiomeColumn(
			World world, Random rand,
			int chunkX, int chunkZ, int x, int z,
			byte[] blocks, int seaLevel,
			boolean sandBeach, boolean gravelBed, int dirtDepth);

	/** Drops the ore veins for the chunk this biome decorates. */
	public abstract void populateOres(World world, Random rand, int baseX, int baseZ);

	/**
	 * Places everything that is not an ore vein (trees, ...) for the chunk's
	 * center biome. The tree-count noise is computed by the provider and passed
	 * in, keeping the noise generator provider-owned.
	 */
	public abstract void decorate(World world, Random rand, int baseX, int baseZ, double treeNoise);
}
