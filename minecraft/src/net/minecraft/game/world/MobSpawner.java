package net.minecraft.game.world;

import net.minecraft.game.entity.Entity;
import net.minecraft.game.entity.EntityLiving;
import net.minecraft.game.world.material.Material;
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

    private final World world;
    private final int maxSpawns;
    private final Class<? extends EntityLiving> entityType;
    private final Class<?>[] entityClasses;

    /**
     * Creates a spawner.
     *
     * @param world          The world this spawner operates in (stored for use in {@link #tick})
     * @param maxSpawns     Maximum number of live entities of {@code entityType} allowed
     * @param entityType    Base class to count against the cap (e.g. {@link EntityMonster})
     * @param entityClasses Pool of concrete entity classes to pick from randomly
     */
    public MobSpawner(World world, int maxSpawns, Class<? extends EntityLiving> entityType, Class<?>[] entityClasses) {
        this.world = world;
        this.maxSpawns = maxSpawns;
        this.entityType = entityType;
        this.entityClasses = entityClasses;
    }

    /**
     * Called every world tick from {@link World#tick()}. If the current population is
     * below {@link #maxSpawns}, attempts to spawn up to one entity.
     */
    public final void tick() {
        int currentCount = this.world.getCachedEntityCount(this.entityType);
        if (currentCount < this.maxSpawns) {
            this.findSpawns(this.world.playerEntity);
        }
    }

    /**
     * Attempts to find and spawn an entity near {@code anchor}.
     *
     * @param anchor The entity to stay 16+ blocks away from (may be null;
     *               in that case the world spawn point is used as the exclusion center)
     * @return Number of entities successfully spawned
     */
    private int findSpawns(Entity anchor) {
        int spawnedCount = 0;

        int anchorChunkX = MathHelper.floor_double(anchor.posX);
        int anchorChunkZ = MathHelper.floor_double(anchor.posZ);

        int[] spawnPos = this.findSpawnPosition(anchorChunkX, anchorChunkZ);

        if (spawnPos != null) {
            float entityX = spawnPos[0] + 0.5F;
            float entityY = spawnPos[1] + 1.0F;
            float entityZ = spawnPos[2] + 0.5F;

            if (this.trySpawn(anchor, entityX, entityY, entityZ) != null) {
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
     * @param anchorChunkX Player's chunk X (used as spawn area center)
     * @param anchorChunkZ Player's chunk Z (used as spawn area center)
     * @return int[3] with {x, y, z} of a valid surface, or null if none found
     */
    private int[] findSpawnPosition(int anchorChunkX, int anchorChunkZ) {
        int baseX = anchorChunkX + this.world.rand.nextInt(SPAWN_HORIZONTAL_SPREAD * 2) - SPAWN_HORIZONTAL_SPREAD;
        int baseY = this.world.rand.nextInt(SPAWN_VERTICAL_RANGE);
        int baseZ = anchorChunkZ + this.world.rand.nextInt(SPAWN_HORIZONTAL_SPREAD * 2) - SPAWN_HORIZONTAL_SPREAD;

        if (this.world.isSolid(baseX, baseY, baseZ) || this.world.getBlockMaterial(baseX, baseY, baseZ) != Material.air) {
            return null;
        }

        for (int surfaceAttempt = 0; surfaceAttempt < SURFACE_ATTEMPTS; surfaceAttempt++) {
            int x = baseX;
            int y = baseY;
            int z = baseZ;

            for (int jitterAttempt = 0; jitterAttempt < JITTER_ATTEMPTS; jitterAttempt++) {
                x += this.jitter(MAX_JITTER_HORIZONTAL);
                y += this.jitter(MAX_JITTER_VERTICAL);
                z += this.jitter(MAX_JITTER_HORIZONTAL);

                if (this.isValidSurface(x, y, z)) {
                    return new int[]{x, y, z};
                }
            }
        }

        return null;
    }

    /**
     * Returns a random offset in the range [-range, +range] using the world's RNG.
     * Uses {@code rand.nextInt(range + 1) - rand.nextInt(range + 1)} so that zero is
     * possible and the distribution is triangular (values near zero are more likely
     * than ±range).
     */
    private int jitter(int range) {
        return this.world.rand.nextInt(range + 1) - this.world.rand.nextInt(range + 1);
    }

    /**
     * Returns true when the block at (x, y, z) is a valid spawn surface:
     * solid below, air above, and no liquid in the spawn cell.
     */
    private boolean isValidSurface(int x, int y, int z) {
        return this.world.isSolid(x, y - 1, z)
            && !this.world.isSolid(x, y, z)
            && !this.world.getBlockMaterial(x, y, z).getIsLiquid()
            && !this.world.isSolid(x, y + 1, z);
    }

    /**
     * Checks that the spawn position is at least 16 blocks away from
     * {@code anchor}. When anchor is null the world's spawn point is used.
     */
    private boolean isFarEnoughFrom(Entity anchor, float x, float y, float z) {
        if (anchor != null) {
            double dx = x - (float) anchor.posX;
            double dy = y - (float) anchor.posY;
            double dz = z - (float) anchor.posZ;
            return (dx * dx + dy * dy + dz * dz) >= MIN_SPAWN_DISTANCE_SQ;
        } else {
            float dx = x - (float) this.world.spawnX;
            float dy = y - (float) this.world.spawnY;
            float dz = z - (float) this.world.spawnZ;
            return (dx * dx + dy * dy + dz * dz) >= (float) MIN_SPAWN_DISTANCE_SQ;
        }
    }

    /**
     * Attempts to instantiate a random entity from {@link #entityClasses}
     * using the World-only constructor via reflection.
     *
     * @return The new entity instance, or null if instantiation failed
     */
    private EntityLiving createEntity() {
        int entityIndex = this.world.rand.nextInt(this.entityClasses.length);
        try {
            return (EntityLiving) this.entityClasses[entityIndex]
                .getConstructor(World.class)
                .newInstance(this.world);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Validates and spawns an entity at the given position.
     * Returns the spawned entity on success, null otherwise.
     */
    private EntityLiving trySpawn(Entity anchor, float x, float y, float z) {
        if (!this.isFarEnoughFrom(anchor, x, y, z)) {
            return null;
        }

        EntityLiving entity = this.createEntity();
        if (entity == null) {
            return null;
        }

        entity.setLocationAndAngles(x, y, z, this.world.rand.nextFloat() * 360.0F, 0.0F);

        if (!entity.getCanSpawnHere(x, y, z)) {
            return null;
        }

        if (entity.mightSpawnArmored()) {
            entity.addRandomArmor();
        }

        this.world.spawnEntityInWorld(entity);
        return entity;
    }
}
