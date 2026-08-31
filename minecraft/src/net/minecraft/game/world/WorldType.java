package net.minecraft.game.world;

import java.util.Arrays;
import net.minecraft.game.world.biome.BiomeProvider;
import net.minecraft.game.world.biome.BiomeProviderInfdev;
import net.minecraft.game.world.terrain.ChunkProviderGenerate;
import net.minecraft.game.world.terrain.ChunkProviderGenerate420;

/**
 * Describes a family of worlds: a display name, the base atmosphere palette
 * (sky, cloud and fog colors), the height of the cloud layer and the chunk
 * generator that builds the terrain. The object is immutable - each distinct
 * world "type" is a single shared instance, selected by id.
 *
 * <p>Colors are the raw RGB pixel values (0xRRGGBB over the low 24 bits) used
 * as the day palette by {@link World}'s sky/cloud/fog getters, which then apply
 * the time-of-day shading. {@link #createChunkProvider} hands out the freshly
 * constructed generator for the type; the factory is stored as a method
 * reference so adding a type only declares its constructor.
 *
 * <p>{@link #WORLDTYPE_420} is the only type today and the default used when
 * level.dat carries no "WorldType" entry (or carries an id nobody knows). New
 * world types register themselves in {@link #worldTypes}, and their string id
 * is what gets written to level.dat so a saved world can be re-identified on
 * load.
 *
 * <p>Alongside the chunk generator a type also owns the {@link BiomeProvider}
 * that decides which {@link BiomeGenerator} describes each column. Today the
 * single type uses {@link BiomeProviderInfdev}, which always yields the one
 * world biome.
 */
public final class WorldType {
	/**
	 * The over-world type for this version's world format
	 * ({@code inf-20100420}), using {@link ChunkProviderGenerate420},
	 * {@link BiomeProviderInfdev} and the classic atmosphere values.
	 */
	public static final WorldType WORLDTYPE_420 = new WorldType("WORLDTYPE_420", "Infdev 420", 10079487L, 16777215L, 11587839L, 120, ChunkProviderGenerate420::new, new BiomeProviderInfdev());

	/** All known world types; the order also defines any future selection order. */
	private static final WorldType[] worldTypes = {WORLDTYPE_420};

	/** The unique id persisted in level.dat. */
	private final String id;
	/** The human-readable name shown in world creation screens. */
	private final String description;
	/** The base sky color (0xRRGGBB), shaded by the time of day. */
	private final long skyColor;
	/** The base cloud color (0xRRGGBB), shaded by the time of day. */
	private final long cloudColor;
	/** The base fog color (0xRRGGBB), shaded by the time of day. */
	private final long fogColor;
	/** The world-space height of the drifting cloud layer. */
	private final int cloudHeight;
	/** Factory that builds the {@link ChunkProviderGenerate} for this type. */
	private final ChunkProviderFactory chunkProviderFactory;
	/** The {@link BiomeProvider} that maps every column to a biome for this type. */
	private final BiomeProvider biomeProvider;

	/**
	 * Builds a world type. The factory is typically a constructor reference,
	 * e.g. {@code ChunkProviderGenerate420::new}.
	 */
	private WorldType(String id, String description, long skyColor, long cloudColor, long fogColor, int cloudHeight, ChunkProviderFactory chunkProviderFactory, BiomeProvider biomeProvider) {
		this.id = id;
		this.description = description;
		this.skyColor = skyColor;
		this.cloudColor = cloudColor;
		this.fogColor = fogColor;
		this.cloudHeight = cloudHeight;
		this.chunkProviderFactory = chunkProviderFactory;
		this.biomeProvider = biomeProvider;
	}

	/** Returns the unique, level.dat-safe id of this type. */
	public final String getId() {
		return this.id;
	}

	/** Returns the display name shown in world creation screens. */
	public final String getDescription() {
		return this.description;
	}

	/** Returns the base sky color (0xRRGGBB). */
	public final long getSkyColor() {
		return this.skyColor;
	}

	/** Returns the base cloud color (0xRRGGBB). */
	public final long getCloudColor() {
		return this.cloudColor;
	}

	/** Returns the base fog color (0xRRGGBB). */
	public final long getFogColor() {
		return this.fogColor;
	}

	/** Returns the world-space height of the cloud layer. */
	public final int getCloudHeight() {
		return this.cloudHeight;
	}

	/**
	 * Creates a fresh terrain generator for this type bound to the given world.
	 */
	public final ChunkProviderGenerate createChunkProvider(World world, long seed, WorldOptions worldOptions) {
		return this.chunkProviderFactory.create(world, seed, worldOptions);
	}

	/** Returns the {@link BiomeProvider} that maps columns to biomes for this type. */
	public final BiomeProvider getBiomeProvider() {
		return this.biomeProvider;
	}

	/** Returns the type registered under the given id, or null when unknown. */
	public static final WorldType fromId(String id) {
		return Arrays.stream(worldTypes).filter(type -> type.id.equals(id)).findFirst().orElse(null);
	}

	/**
	 * Returns all known world types (a defensive copy), in registration order.
	 */
	public static final WorldType[] getWorldTypes() {
		return Arrays.copyOf(worldTypes, worldTypes.length);
	}

	/** Builds the {@link ChunkProviderGenerate} that generates a world type's terrain. */
	@FunctionalInterface
	public interface ChunkProviderFactory {
		ChunkProviderGenerate create(World world, long seed, WorldOptions worldOptions);
	}
}