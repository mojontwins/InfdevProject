package net.minecraft.game.world.biome;

/**
 * The single {@link BiomeProvider} of this stage. Every column in the world is
 * described by the one biome the engine knows, {@link BiomeGenInfdev}, so both
 * {@link #getBiome} and {@link #getBiomes} always resolve to it. Because it is
 * stateless it is safe to share a single instance (the owning {@code WorldType}
 * is itself a shared singleton).
 *
 * <p>This is the natural place a future, noise-driven biome distribution will
 * appear: {@link #getBiome} and {@link #getBiomes} would consult a noise field
 * and return different {@link BiomeGenerator}s by position, and
 * {@link #getBiomeFromID} would use a real id&rarr;biome table.
 */
public final class BiomeProviderInfdev extends BiomeProvider {
	@Override
	public final BiomeGenerator getBiome(int x, int z) {
		return BiomeGenInfdev.INSTANCE;
	}

	@Override
	public final BiomeGenerator[] getBiomes(int x0, int z0, int xSize, int zSize) {
		BiomeGenerator[] biomes = new BiomeGenerator[xSize * zSize];
		for(int i = 0; i < biomes.length; ++i) {
			biomes[i] = BiomeGenInfdev.INSTANCE;
		}
		return biomes;
	}

	@Override
	public final BiomeGenerator getBiomeFromID(int id) {
		// Only one biome is registered; any stored id (including unknown ones
		// from a future save) resolves to the default world biome.
		return BiomeGenInfdev.INSTANCE;
	}
}
