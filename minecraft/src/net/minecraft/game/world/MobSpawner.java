package net.minecraft.game.world;

import java.util.HashSet;
import java.util.Random;
import net.minecraft.game.entity.Entity;
import net.minecraft.game.entity.EntityLiving;
import net.minecraft.game.entity.animal.EntityAnimal;
import net.minecraft.game.entity.animal.EntityCow;
import net.minecraft.game.entity.animal.EntityPig;
import net.minecraft.game.entity.animal.EntitySheep;
import net.minecraft.game.entity.monster.EntityCreeper;
import net.minecraft.game.entity.monster.EntityMonster;
import net.minecraft.game.entity.monster.EntitySkeleton;
import net.minecraft.game.entity.monster.EntitySpider;
import net.minecraft.game.entity.monster.EntityZombie;
import net.minecraft.game.world.material.Material;
import util.MathHelper;

/**
 * Spawns hostile and passive creatures around the player, keeping each
 * population under a cap that scales with the size of the loaded area.
 *
 * <p>The old spawner (see project history) compared a world-wide count against
 * a fixed global cap, so once 100 monsters existed <em>anywhere</em> the world
 * it stopped spawning <em>everywhere</em>. This rework keys the whole thing to
 * the player's loaded region: it collects the 9x9 chunk square (±4 chunks)
 * around each player, caps each creature type by
 * {@code baseCap * eligibleChunks / 256}, and tries a handful of spawns per
 * tick inside those chunks — so density is local and the cap grows with the
 * explored area instead of jamming at a global 100.</p>
 */
public final class MobSpawner {

	/** Half-width, in chunks, of the spawnable region around each player. */
	private static final int SPAWN_RADIUS_CHUNKS = 4;

	/** Monster cap multiplier, normalised over 256 eligible chunks. */
	private static final int MAX_MONSTERS_BASE = 70;

	/** Animal cap multiplier, normalised over 256 eligible chunks. */
	private static final int MAX_ANIMALS_BASE = 15;

	/** Spawn attempts issued per creature type per world tick. */
	private static final int SPAWN_ATTEMPTS_PER_TICK = 3;

	/** 1-in-N per chunk per tick chance of even trying (rate limiting). */
	private static final int CHUNK_RATE_LIMIT = 10;

	/** No spawn within this distance of any player (blocks). */
	private static final float MIN_PLAYER_DISTANCE = 24.0F;

	/** Squared exclusion radius around the world spawn point. */
	private static final float MIN_WORLD_SPAWN_DISTANCE_SQ = 576.0F;

	/** Vertical range searched for a spawn cell (world height cap). */
	private static final int WORLD_HEIGHT = 128;

	/** Maximum horizontal jitter applied to a candidate position (±5 blocks). */
	private static final int MAX_JITTER_HORIZONTAL = 5;

	/** Maximum vertical jitter applied to a candidate position (±1 block). */
	private static final int MAX_JITTER_VERTICAL = 1;

	/** Number of distinct surface candidates to try before giving up. */
	private static final int SURFACE_ATTEMPTS = 6;

	/** Number of jitter iterations per surface candidate. */
	private static final int JITTER_ATTEMPTS = 6;

	/** Hostile mobs the spawner can pick from, chosen at random. */
	private static final Class<?>[] MONSTER_CLASSES = new Class<?>[]{
		EntityZombie.class, EntitySkeleton.class, EntityCreeper.class, EntitySpider.class};

	/** Passive mobs the spawner can pick from, chosen at random. */
	private static final Class<?>[] ANIMAL_CLASSES = new Class<?>[]{
		EntitySheep.class, EntityPig.class, EntityCow.class};

	private MobSpawner() {
	}

	/**
	 * The single entry point called from {@link World#tick()}. Re-populates
	 * both the monster and the animal pool, subject to their local caps.
	 */
	public static void performSpawning(World world) {
		Entity player = world.playerEntity;
		if(player == null) {
			return;
		}

		Random rand = world.rand;
		long[] eligibleChunks = buildEligibleChunks(player);
		int monsterChunks = eligibleChunks.length;
		int maxMonsters = MAX_MONSTERS_BASE * monsterChunks / 256;
		int maxAnimals = MAX_ANIMALS_BASE * monsterChunks / 256;

		if(world.getCachedEntityCount(EntityMonster.class) < maxMonsters) {
			spawnCreatures(world, player, rand, eligibleChunks, MONSTER_CLASSES);
		}

		if(world.getCachedEntityCount(EntityAnimal.class) < maxAnimals) {
			spawnCreatures(world, player, rand, eligibleChunks, ANIMAL_CLASSES);
		}
	}

	/**
	 * Builds the deduplicated list of chunk coordinates (±4 chunks around the
	 * player) that creatures may spawn in. Coordinates are packed into a single
	 * {@code long} ({@code x} in the high 32 bits, {@code z} in the low 32).
	 */
	private static long[] buildEligibleChunks(Entity player) {
		int centerX = MathHelper.floor_double(player.posX / 16.0D);
		int centerZ = MathHelper.floor_double(player.posZ / 16.0D);
		HashSet<Long> seen = new HashSet<>();
		int radius = SPAWN_RADIUS_CHUNKS;
		for(int dx = -radius; dx <= radius; ++dx) {
			for(int dz = -radius; dz <= radius; ++dz) {
				seen.add(packChunk(centerX + dx, centerZ + dz));
			}
		}

		long[] chunks = new long[seen.size()];
		int index = 0;
		for(Long key : seen) {
			chunks[index++] = key.longValue();
		}

		return chunks;
	}

	private static long packChunk(int x, int z) {
		return ((long)x << 32) | (z & 0xFFFFFFFFL);
	}

	private static int unpackChunkX(long key) {
		return (int)(key >> 32);
	}

	private static int unpackChunkZ(long key) {
		return (int)(key & 0xFFFFFFFFL);
	}

	/**
	 * Issues up to {@link #SPAWN_ATTEMPTS_PER_TICK} roll attempts inside random
	 * eligible chunks, rate-limited to {@link #CHUNK_RATE_LIMIT}. Each attempt
	 * spawns at most one entity.
	 */
	private static void spawnCreatures(World world, Entity player, Random rand, long[] chunks, Class<?>[] pool) {
		for(int attempt = 0; attempt < SPAWN_ATTEMPTS_PER_TICK; ++attempt) {
			if(rand.nextInt(CHUNK_RATE_LIMIT) != 0) {
				continue;
			}

			long chunk = chunks[rand.nextInt(chunks.length)];
			attemptSpawn(world, player, rand, chunk, pool);
		}
	}

	/**
	 * Picks a random cell inside the chunk's column and, once a valid surface
	 * is found far enough from player and world spawn, instantiates and spawns
	 * a random creature from {@code pool}. Returns true on success.
	 */
	private static boolean attemptSpawn(World world, Entity player, Random rand, long chunk, Class<?>[] pool) {
		int baseX = unpackChunkX(chunk) * 16 + rand.nextInt(16);
		int baseY = rand.nextInt(WORLD_HEIGHT);
		int baseZ = unpackChunkZ(chunk) * 16 + rand.nextInt(16);

		if(world.isSolid(baseX, baseY, baseZ) || world.getBlockMaterial(baseX, baseY, baseZ) != Material.air) {
			return false;
		}

		for(int surfaceAttempt = 0; surfaceAttempt < SURFACE_ATTEMPTS; ++surfaceAttempt) {
			int x = baseX;
			int y = baseY;
			int z = baseZ;

			for(int jitterAttempt = 0; jitterAttempt < JITTER_ATTEMPTS; ++jitterAttempt) {
				x += jitter(rand, MAX_JITTER_HORIZONTAL);
				y += jitter(rand, MAX_JITTER_VERTICAL);
				z += jitter(rand, MAX_JITTER_HORIZONTAL);

				if(!isValidSurface(world, x, y, z)) {
					continue;
				}

				float posX = x + 0.5F;
				float posY = y + 1.0F;
				float posZ = z + 0.5F;
				if(distanceTo(player, posX, posY, posZ) <= MIN_PLAYER_DISTANCE) {
					continue;
				}

				float worldX = posX - (float)world.spawnX;
				float worldY = posY - (float)world.spawnY;
				float worldZ = posZ - (float)world.spawnZ;
				if(worldX * worldX + worldY * worldY + worldZ * worldZ < MIN_WORLD_SPAWN_DISTANCE_SQ) {
					continue;
				}

				if(spawnEntity(world, rand, pool, posX, posY, posZ)) {
					return true;
				}
			}
		}

		return false;
	}

	private static boolean isValidSurface(World world, int x, int y, int z) {
		return world.isSolid(x, y - 1, z)
			&& !world.isSolid(x, y, z)
			&& !world.getBlockMaterial(x, y, z).getIsLiquid()
			&& !world.isSolid(x, y + 1, z);
	}

	/**
	 * Returns a random offset in the range [-range, +range] using the supplied
	 * RNG. Uses {@code nextInt(range + 1) - nextInt(range + 1)} so zero is
	 * possible and the distribution is triangular.
	 */
	private static int jitter(Random rand, int range) {
		return rand.nextInt(range + 1) - rand.nextInt(range + 1);
	}

	private static float distanceTo(Entity from, float x, float y, float z) {
		float dx = x - (float)from.posX;
		float dy = y - (float)from.posY;
		float dz = z - (float)from.posZ;
		return MathHelper.sqrt_float(dx * dx + dy * dy + dz * dz);
	}

	/**
	 * Instantiates a random entity from {@code pool} via its World-constructor,
	 * validates the cell with {@link EntityLiving#getCanSpawnHere} (which is
	 * side-effect free) and, if all checks pass, adds it to the world.
	 */
	private static boolean spawnEntity(World world, Random rand, Class<?>[] pool, float x, float y, float z) {
		EntityLiving entity;
		try {
			int entityIndex = rand.nextInt(pool.length);
			entity = (EntityLiving)pool[entityIndex].getConstructor(World.class).newInstance(world);
		} catch(Exception e) {
			e.printStackTrace();
			return false;
		}

		entity.setLocationAndAngles(x, y, z, rand.nextFloat() * 360.0F, 0.0F);
		if(!entity.getCanSpawnHere(x, y, z)) {
			return false;
		}

		if(entity.mightSpawnArmored()) {
			entity.addRandomArmor();
		}

		world.spawnEntityInWorld(entity);
		return true;
	}
}
