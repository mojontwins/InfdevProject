package net.minecraft.game.world;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.game.entity.Entity;
import net.minecraft.game.physics.AxisAlignedBB;
import net.minecraft.game.physics.Vec3D;
import net.minecraft.game.world.block.Block;
import util.MathHelper;

/**
 * Handles explosion mechanics including block destruction, entity damage, 
 * and particle effects. Explosions radiate from a center point and affect
 * blocks and entities within a configurable radius.
 */
public class Explosion {
    /** Whether this explosion should ignite fires (not used in Infdev) */
    public boolean isFlaming = false;
    
    /** The world in which the explosion occurs */
    private final World worldObj;
    
    /** The entity causing the explosion (can be null) */
    public Entity exploder;
    
    /** X coordinate of explosion center */
    public double explosionX;
    
    /** Y coordinate of explosion center */
    public double explosionY;
    
    /** Z coordinate of explosion center */
    public double explosionZ;
    
    /** Size/radius of the explosion */
    public float explosionSize;
    
    /** Set of block positions destroyed by the explosion */
    public final Set<ChunkPosition> destroyedBlockPositions = new HashSet<>();

    /**
     * Creates a new explosion.
     * 
     * @param world The world where the explosion happens
     * @param entity The entity causing the explosion (may be null)
     * @param x X coordinate of explosion center
     * @param y Y coordinate of explosion center
     * @param z Z coordinate of explosion center
     * @param size Size/radius of the explosion
     */
    public Explosion(World world, Entity entity, double x, double y, double z, float size) {
        this.worldObj = world;
        this.exploder = entity;
        this.explosionX = x;
        this.explosionY = y;
        this.explosionZ = z;
        this.explosionSize = size;
    }

    /**
     * Executes the core explosion logic: finds affected blocks and entities,
     * applies damage, and determines what blocks will be destroyed.
     */
    public void explode() {
        float size = this.explosionSize;
        int gridSize = 16;

        // Cast rays outward from center to detect which blocks would be destroyed
        for (int x = 0; x < gridSize; x++) {
            for (int y = 0; y < gridSize; y++) {
                for (int z = 0; z < gridSize; z++) {
                    // Only check blocks on the surface of the 16^3 cube
                    if (x == 0 || x == gridSize - 1 || y == 0 || y == gridSize - 1 || z == 0 || z == gridSize - 1) {
                        // Calculate normalized direction vector from center to this cube face point
                        double dx = ((float) x / (gridSize - 1.0F) * 2.0F - 1.0F);
                        double dy = ((float) y / (gridSize - 1.0F) * 2.0F - 1.0F);
                        double dz = ((float) z / (gridSize - 1.0F) * 2.0F - 1.0F);
                        
                        // Normalize the direction vector
                        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
                        dx /= length;
                        dy /= length;
                        dz /= length;
                        
                        // Start with reduced explosion size due to randomness
                        float remainingSize = this.explosionSize * (0.7F + this.worldObj.rand.nextFloat() * 0.6F);
                        double rayX = this.explosionX;
                        double rayY = this.explosionY;
                        double rayZ = this.explosionZ;

                        // Trace the ray until explosion force is depleted
                        for (; remainingSize > 0.0F; remainingSize -= 0.22500001F) {
                            int blockX = MathHelper.floor_double(rayX);
                            int blockY = MathHelper.floor_double(rayY);
                            int blockZ = MathHelper.floor_double(rayZ);
                            int blockId = this.worldObj.getBlockId(blockX, blockY, blockZ);
                            
                            // Reduce explosion force by block resistance
                            if (blockId > 0) {
                                remainingSize -= (Block.blocksList[blockId].getExplosionResistance() + 0.3F) * 0.3F;
                            }
                            
                            // If force remains, mark this block for destruction
                            if (remainingSize > 0.0F) {
                                this.destroyedBlockPositions.add(new ChunkPosition(blockX, blockY, blockZ));
                            }
                            
                            // Advance the ray
                            rayX += dx * 0.3;
                            rayY += dy * 0.3;
                            rayZ += dz * 0.3;
                        }
                    }
                }
            }
        }

        // Apply explosion effects to entities within expanded radius
        this.explosionSize *= 2.0F;
        int minX = MathHelper.floor_double(this.explosionX - this.explosionSize - 1.0D);
        int maxX = MathHelper.floor_double(this.explosionX + this.explosionSize + 1.0D);
        int minY = MathHelper.floor_double(this.explosionY - this.explosionSize - 1.0D);
        int maxY = MathHelper.floor_double(this.explosionY + this.explosionSize + 1.0D);
        int minZ = MathHelper.floor_double(this.explosionZ - this.explosionSize - 1.0D);
        int maxZ = MathHelper.floor_double(this.explosionZ + this.explosionSize + 1.0D);
        
        List<Entity> affectedEntities = this.worldObj.getEntitiesWithinAABBExcludingEntity(
            this.exploder, 
            new AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ)
        );
        Vec3D explosionVec = new Vec3D(this.explosionX, this.explosionY, this.explosionZ);

        // Apply damage and knockback to each affected entity within explosion radius
        for (Entity entity : affectedEntities) {
            double dx = entity.posX - this.explosionX;
            double dy = entity.posY - this.explosionY;
            double dz = entity.posZ - this.explosionZ;
            double distance = MathHelper.sqrt_double(dx * dx + dy * dy + dz * dz) / this.explosionSize;
            if (distance <= 1.0D) {
                // Normalize the direction vector from explosion center to entity
                double entityDistance = MathHelper.sqrt_double(dx * dx + dy * dy + dz * dz);
                dx /= entityDistance;
                dy /= entityDistance;
                dz /= entityDistance;
                
                // Calculate blocking factor based on terrain density
                double blockDensity = this.worldObj.getBlockDensity(explosionVec, entity.boundingBox);
                double blockingFactor = (1.0D - distance) * blockDensity;
                
                // Calculate damage and apply knockback
                int damage = (int) ((blockingFactor * blockingFactor + blockingFactor) / 2.0D * 8.0D * this.explosionSize + 1.0D);
                entity.attackEntityFrom(this.exploder, damage);
                entity.motionX += dx * blockingFactor;
                entity.motionY += dy * blockingFactor;
                entity.motionZ += dz * blockingFactor;
            }
        }

        // Reset explosion size for block processing
        this.explosionSize = size;
    }

    /**
     * Applies the visual and physical effects of the explosion:
     * plays sound, spawns particles, and destroys blocks.
     */
    public void applyEffects() {
        // Play explosion sound
        this.worldObj.playSoundEffect(
            this.explosionX, 
            this.explosionY, 
            this.explosionZ, 
            "random.explode", 
            4.0F, 
            (1.0F + (this.worldObj.rand.nextFloat() - this.worldObj.rand.nextFloat()) * 0.2F) * 0.7F
        );

        // Process each destroyed block for particles and block removal
        List<ChunkPosition> blocksToProcess = new ArrayList<>(this.destroyedBlockPositions);
        
        for (ChunkPosition blockPos : blocksToProcess) {
            int blockX = blockPos.x;
            int blockY = blockPos.y;
            int blockZ = blockPos.z;
            int blockId = this.worldObj.getBlockId(blockX, blockY, blockZ);

            // Spawn explosion and smoke particles at block edges
            for (int i = 0; i <= 0; i++) {
                double particleX = blockX + this.worldObj.rand.nextFloat();
                double particleY = blockY + this.worldObj.rand.nextFloat();
                double particleZ = blockZ + this.worldObj.rand.nextFloat();
                
                double dx = particleX - this.explosionX;
                double dy = particleY - this.explosionY;
                double dz = particleZ - this.explosionZ;
                
                // Normalize the direction vector
                double length = MathHelper.sqrt_double(dx * dx + dy * dy + dz * dz);
                dx /= length;
                dy /= length;
                dz /= length;
                
                // Calculate particle motion with randomness
                double motion = 0.5D / (length / this.explosionSize + 0.1D);
                motion *= this.worldObj.rand.nextFloat() * this.worldObj.rand.nextFloat() + 0.3F;
                
                double motionX = dx * motion;
                double motionY = dy * motion;
                double motionZ = dz * motion;
                
                // Spawn explode particles at block center
                this.worldObj.spawnParticle(
                    "explode", 
                    (particleX + this.explosionX) / 2.0D, 
                    (particleY + this.explosionY) / 2.0D, 
                    (particleZ + this.explosionZ) / 2.0D, 
                    motionX, 
                    motionY, 
                    motionZ
                );
                
                // Spawn smoke particles at block position
                this.worldObj.spawnParticle(
                    "smoke", 
                    particleX, 
                    particleY, 
                    particleZ, 
                    motionX, 
                    motionY, 
                    motionZ
                );
            }

            // Destroy the block and drop items
            if (blockId > 0) {
                Block.blocksList[blockId].dropBlockAsItemWithChance(
                    this.worldObj, 
                    blockX, 
                    blockY, 
                    blockZ, 
                    this.worldObj.getBlockMetadata(blockX, blockY, blockZ), 
                    0.3F
                );
                this.worldObj.setBlockWithNotify(blockX, blockY, blockZ, 0);
                Block.blocksList[blockId].onBlockDestroyedByExplosion(
                    this.worldObj, 
                    blockX, 
                    blockY, 
                    blockZ
                );
            }
        }
    }
}