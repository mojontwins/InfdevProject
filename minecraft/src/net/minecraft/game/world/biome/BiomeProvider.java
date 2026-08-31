package net.minecraft.game.world.biome;

/**
 * Answers the question <em>"which biome is at a position?"</em> for a world.
 * A {@code BiomeProvider} has no terrain state of its own; it only decides
 * which {@link BiomeGenerator} describes a given column or block of columns.
 *
 * <p>The block form {@link #getBiomes} exists so a chunk can fetch its whole
 * 16&times;16 biome grid in one contiguous lookup (typically with chunk origins
 * and 16&times;16 sizes) and persist it as a flat array; {@link #getBiome} reads
 * a single world column. {@link #getBiomeFromID} turns a stored byte id back
 * into the {@link BiomeGenerator} the chunk data refers to.
 *
 * <p>A world type owns the {@code BiomeProvider} it uses (see
 * {@link net.minecraft.game.world.WorldType#getBiomeProvider}). Today only
 * {@link BiomeProviderInfdev} exists, which always returns {@link BiomeGenInfdev}.
 */
public abstract class BiomeProvider {
	/** The biome at a single world column (x, z). */
	public abstract BiomeGenerator getBiome(int x, int z);

	/**
	 * A 2-D block of biomes covering [x0, x0+xSize) &times; [z0, z0+zSize).
	 * Normally called with chunk origins and 16&times;16 sizes. The returned
	 * array is indexed z-major: {@code index = z * xSize + x}.
	 *
	 * @param x0 world x of the first column
	 * @param z0 world z of the first column
	 * @param xSize number of columns along x
	 * @param zSize number of columns along z
	 * @return an array of {@code xSize * zSize} {@link BiomeGenerator}s
	 */
	public abstract BiomeGenerator[] getBiomes(int x0, int z0, int xSize, int zSize);

	/**
	 * Resolves a stored biome id (a byte kept in a chunk's biome array) back to
	 * the {@link BiomeGenerator} it names.
	 *
	 * @param id the stored biome id
	 * @return the matching {@link BiomeGenerator}
	 */
	public abstract BiomeGenerator getBiomeFromID(int id);
}
