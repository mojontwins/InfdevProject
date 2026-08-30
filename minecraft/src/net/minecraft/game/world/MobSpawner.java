package net.minecraft.game.world;

import java.util.Random;
import net.minecraft.game.entity.Entity;
import net.minecraft.game.entity.EntityLiving;
import net.minecraft.game.world.material.Material;
import util.IProgressUpdate;
import util.MathHelper;

/**
 * Spawns creatures of a given type around the player, keeping the population
 * within a configurable cap. Used for both monster and animal spawning:
 * one instance for {@link EntityMonster} (max 100) and one for
 * {@link EntityAnimal} (max 50), both owned by {@code PlayerControllerSP}.
 *
 * <p>The spawning algorithm picks a random position near the player, searches
 * for a valid surface (solid below, air above, no liquid), validates that it
 * is far enough from the player, then instantiates and spawns the entity.
 *
 * <p>Two independent search loops try up to {@link #SURFACE_ATTEMPTS} surface
 * positions, each jittered {@link #JITTER_ATTEMPTS} times for variety.
 */
public final class MobSpawner {

    /** Half-width of the spawn area; entities spawn within ±128 blocks of the player. */
    private static final int SPAWN_HORIZONTAL_SPREAD = 128;

    /** Vertical range for spawn attempts (0 to 127 blocks). */
    private static final int SPAWN_VERTICAL_RANGE = 128;

    /** Squared exclusion radius: no spawn within 16 blocks of the player/spawn point. */
    private static final double MIN_SPAWN_DISTANCE_SQ = 256.0D;

    /** Maximum horizontal jitter applied to a candidate position (±5 blocks). */
    private static final int MAX_JITTER_HORIZONTAL = 5;

    /** Maximum vertical jitter applied to a candidate position (±1 block). */
    private static final int MAX_JITTER_VERTICAL = 1;

    /** Number of distinct surface candidates to try before giving up. */
    private static final int SURFACE_ATTEMPTS = 6;

    /** Number of jitter iterations per surface candidate. */
    private static final int JITTER_ATTEMPTS = 6;

    private final int maxSpawns;
    private final Class<? extends EntityLiving> entityType;
    private final Class<?>[] entityClasses;

    /**
     * Creates a spawner.
     *
     * @param maxSpawns    Maximum number of live entities of {@code entityType} allowed
     * @param entityType   Base class to count against the cap (e.g. {@link EntityMonster})
     * @param entityClasses Pool of concrete entity classes to pick from randomly
     */
    public MobSpawner(int maxSpawns, Class<? extends EntityLiving> entityType, Class<?>[] entityClasses) {
        this.maxSpawns = maxSpawns;
        this.entityType = entityType;
        this.entityClasses = entityClasses;
    }

    /**
     * Called every tick from {@code PlayerControllerSP.onUpdate}. If the current
     * population is below {@link #maxSpawns}, attempts to spawn up to one entity.
     *
     * @param world The world to spawn into
     */
    public final void onUpdate(World world) {
        int currentCount = world.getCachedEntityCount(this.entityType);
        if (currentCount < this.maxSpawns) {
            this.findSpawns(world, world.playerEntity, null);
        }
    }

    /**
     * Attempts to find and spawn entities near {@code anchor}. When the player
     * is present {@code anchor} is used as the exclusion center; otherwise the
     * world's designated spawn point is used.
     *
     * @param world        The world
     * @param anchor       The entity to stay 16+ blocks away from (may be null)
     * @param progressSink Unused; present for API compatibility with MCP
     * @return Number of entities successfully spawned
     */
    private int findSpawns(World world, Entity anchor, IProgressUpdate progressSink) {
        int spawnedCount = 0;

        int anchorChunkX = MathHelper.floor_double(anchor.posX);
        int anchorChunkZ = MathHelper.floor_double(anchor.posZ);

        int[] spawnPos = this.findSpawnPosition(world, anchorChunkX, anchorChunkZ);

        if (spawnPos != null) {
            int spawnX = spawnPos[0];
            int spawnY = spawnPos[1];
            int spawnZ = spawnPos[2];
            float entityX = spawnX + 0.5F;
            float entityY = spawnY + 1.0F;
            float entityZ = spawnZ + 0.5F;

            EntityLiving entity = this.trySpawn(world, anchor, entityX, entityY, entityZ);
            if (entity != null) {
                spawnedCount++;
            }
        }

        return spawnedCount;
    }

    /**
     * Searches for a valid spawn surface near the player's chunk. Tries up to
     * {@link #SURFACE_ATTEMPTS} surface positions, each jittered up to
     * {@link #JITTER_ATTEMPTS} times.
     *
     * @param world            The world
     * @param anchorChunkX     Player's chunk X (used as spawn area center)
     * @param anchorChunkZ     Player's chunk Z (used as spawn area center)
     * @return int[3] with {x, y, z} of a valid surface, or null if none found
     */
    private int[] findSpawnPosition(World world, int anchorChunkX, int anchorChunkZ) {
        int baseX = anchorChunkX + world.rand.nextInt(SPAWN_HORIZONTAL_SPREAD * 2) - SPAWN_HORIZONTAL_SPREAD;
        int baseY = world.rand.nextInt(SPAWN_VERTICAL_RANGE);
        int baseZ = anchorChunkZ + world.rand.nextInt(SPAWN_HORIZONTAL_SPREAD * 2) - SPAWN_HORIZONTAL_SPREAD;

        if (world.isSolid(baseX, baseY, baseZ) || world.getBlockMaterial(baseX, baseY, baseZ) != Material.air) {
            return null;
        }

        for (int surfaceAttempt = 0; surfaceAttempt < SURFACE_ATTEMPTS; surfaceAttempt++) {
            int x = baseX;
            int y = baseY;
            int z = baseZ;

            for (int jitterAttempt = 0; jitterAttempt < JITTER_ATTEMPTS; jitterAttempt++) {
                x += this.jitter(world.rand, MAX_JITTER_HORIZONTAL);
                y += this.jitter(world.rand, MAX_JITTER_VERTICAL);
                z += this.jitter(world.rand, MAX_JITTER_HORIZONTAL);

                if (this.isValidSurface(world, x, y, z)) {
                    return new int[]{x, y, z};
                }
            }
        }

        return null;
    }

    /**
     * Returns a random offset in the range [-range, +range] using the supplied RNG.
     * Uses {@code rand.nextInt(range + 1) - rand.nextInt(range + 1)} so that zero is
     * possible and the distribution is triangular (values near zero are more likely
     * than ±range).
     */
    private int jitter(Random rng, int range) {
        return rng.nextInt(range + 1) - rng.nextInt(range + 1);
    }

    /**
     * Returns true when the block at (x, y, z) is a valid spawn surface:
     * solid below, air above, and no liquid in the spawn cell.
     */
    private boolean isValidSurface(World world, int x, int y, int z) {
        return world.isSolid(x, y - 1, z)
            && !world.isSolid(x, y, z)
            && !world.getBlockMaterial(x, y, z).getIsLiquid()
            && !world.isSolid(x, y + 1, z);
    }

    /**
     * Checks that the spawn position is at least 16 blocks away from
     * {@code anchor}. When anchor is null the world's spawn point is used.
     */
    private boolean isFarEnoughFrom(World world, Entity anchor, float x, float y, float z) {
        if (anchor != null) {
            double dx = x - (float) anchor.posX;
            double dy = y - (float) anchor.posY;
            double dz = z - (float) anchor.posZ;
            return (dx * dx + dy * dy + dz * dz) >= MIN_SPAWN_DISTANCE_SQ;
        } else {
            float dx = x - (float) world.spawnX;
            float dy = y - (float) world.spawnY;
            float dz = z - (float) world.spawnZ;
            return (dx * dx + dy * dy + dz * dz) >= (float) MIN_SPAWN_DISTANCE_SQ;
        }
    }

    /**
     * Attempts to instantiate a random entity from {@link #entityClasses}
     * using the World-only constructor via reflection.
     *
     * @param world The world to pass to the constructor
     * @return The new entity instance, or null if instantiation failed
     */
    private EntityLiving createEntity(World world) {
        int entityIndex = world.rand.nextInt(this.entityClasses.length);
        try {
            return (EntityLiving) this.entityClasses[entityIndex]
                .getConstructor(World.class)
                .newInstance(world);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Validates and spawns an entity at the given position.
     * Returns the spawned entity on success, null otherwise.
     */
    private EntityLiving trySpawn(World world, Entity anchor, float x, float y, float z) {
        if (!this.isFarEnoughFrom(world, anchor, x, y, z)) {
            return null;
        }

        EntityLiving entity = this.createEntity(world);
        if (entity == null) {
            return null;
        }

        entity.setLocationAndAngles(x, y, z, world.rand.nextFloat() * 360.0F, 0.0F);

        if (!entity.getCanSpawnHere(x, y, z)) {
            return null;
        }

        if (entity.mightSpawnArmored()) {
            entity.addRandomArmor();
        }

        world.spawnEntityInWorld(entity);
        return entity;
    }
}
