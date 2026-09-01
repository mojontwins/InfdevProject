package net.minecraft.game.world;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.game.entity.Entity;
import net.minecraft.game.physics.AxisAlignedBB;
import net.minecraft.game.physics.Vec3D;
import net.minecraft.game.world.block.Block;
import net.minecraft.game.world.chunk.Chunk;
import net.minecraft.game.world.material.Material;
import util.MathHelper;

/**
 * Static spatial queries against a {@link World}. Every method takes the world
 * as its first argument and reads only (never mutates world state): they either
 * collect {@link AxisAlignedBB}s of colliding blocks, or find {@link Entity}s
 * within a box, or test whether an AABB intersects a particular material/block.
 * Because they have no state of their own they are written as pure static
 * functions extracted from {@link World} in the refactor.
 */
final class EntityQueryService {
	private EntityQueryService() {
	}

	/**
	 * Returns every block's collision AABB inside the given query box. Used by
	 * entity physics to find which solid cells the entity is overlapping.
	 */
	static List<AxisAlignedBB> getCollidingBoundingBoxes(World world, AxisAlignedBB queryBox) {
		ArrayList<AxisAlignedBB> result = new ArrayList<>();
		int minX = MathHelper.floor_double(queryBox.minX);
		int maxX = MathHelper.floor_double(queryBox.maxX + 1.0D);
		int minY = MathHelper.floor_double(queryBox.minY);
		int maxY = MathHelper.floor_double(queryBox.maxY + 1.0D);
		int minZ = MathHelper.floor_double(queryBox.minZ);
		int maxZ = MathHelper.floor_double(queryBox.maxZ + 1.0D);

		for(int x = minX; x < maxX; ++x) {
			for(int y = minY; y < maxY; ++y) {
				for(int z = minZ; z < maxZ; ++z) {
					Block block = Block.blocksList[world.getBlockId(x, y, z)];
					if(block != null) {
						AxisAlignedBB blockAABB = block.getCollisionBoundingBoxFromPool(x, y, z);
						if(blockAABB != null && queryBox.intersectsWith(blockAABB)) {
							result.add(blockAABB);
						}
					}
				}
			}
		}
		return result;
	}

	/**
	 * Returns all entities of the given class intersecting the given expanded box.
	 * The box is expanded by 2 units in every direction before querying chunk
	 * entity lists. {@code excludeEntity} is skipped (pass null to include it).
	 * Used for collision checks and entity-picking.
	 */
	static List<Entity> getEntitiesWithinAABBExcludingEntity(World world, Entity excludeEntity, AxisAlignedBB queryBox) {
		int minChunkX = MathHelper.floor_double((queryBox.minX - 2.0D) / 16.0D);
		int maxChunkX = MathHelper.floor_double((queryBox.maxX + 2.0D) / 16.0D);
		int minChunkZ = MathHelper.floor_double((queryBox.minZ - 2.0D) / 16.0D);
		int maxChunkZ = MathHelper.floor_double((queryBox.maxZ + 2.0D) / 16.0D);
		ArrayList<Entity> result = new ArrayList<>();

		for(int chunkX = minChunkX; chunkX <= maxChunkX; ++chunkX) {
			for(int chunkZ = minChunkZ; chunkZ <= maxChunkZ; ++chunkZ) {
				if(world.chunkExists(chunkX, chunkZ)) {
					Chunk chunk = world.getChunkFromChunkCoords(chunkX, chunkZ);
					chunk.getEntitiesWithinAABBForEntity(excludeEntity, queryBox, result);
				}
			}
		}
		return result;
	}

	/**
	 * Returns true if no entity in the given box has {@code preventEntitySpawning}
	 * set, i.e. the area is open for an entity to spawn. Used by
	 * {@link MobSpawner} to validate a candidate spawn position.
	 */
	static boolean checkIfAABBIsClear1(World world, AxisAlignedBB box) {
		List<Entity> entities = getEntitiesWithinAABBExcludingEntity(world, (Entity)null, box);
		for(int i = 0; i < entities.size(); ++i) {
			if(entities.get(i).preventEntitySpawning) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Returns true if any block cell intersecting the given box is a liquid.
	 * Used by entity physics to know if a position is submerged.
	 */
	static boolean getIsAnyLiquid(World world, AxisAlignedBB box) {
		int minX = MathHelper.floor_double(box.minX);
		int maxX = MathHelper.floor_double(box.maxX + 1.0D);
		int minY = MathHelper.floor_double(box.minY);
		int maxY = MathHelper.floor_double(box.maxY + 1.0D);
		int minZ = MathHelper.floor_double(box.minZ);
		int maxZ = MathHelper.floor_double(box.maxZ + 1.0D);
		if(box.minX < 0.0D) --minX;
		if(box.minY < 0.0D) --minY;
		if(box.minZ < 0.0D) --minZ;

		for(int x = minX; x < maxX; ++x) {
			for(int y = minY; y < maxY; ++y) {
				for(int z = minZ; z < maxZ; ++z) {
					Block block = Block.blocksList[world.getBlockId(x, y, z)];
					if(block != null && block.blockMaterial.getIsLiquid()) {
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * Returns true if any block cell intersecting the given box is fire or
	 * lava. Used by entity AI to decide whether to take fire damage.
	 */
	static boolean isBoundingBoxBurning(World world, AxisAlignedBB box) {
		int minX = MathHelper.floor_double(box.minX);
		int maxX = MathHelper.floor_double(box.maxX + 1.0D);
		int minY = MathHelper.floor_double(box.minY);
		int maxY = MathHelper.floor_double(box.maxY + 1.0D);
		int minZ = MathHelper.floor_double(box.minZ);
		int maxZ = MathHelper.floor_double(box.maxZ + 1.0D);

		for(int x = minX; x < maxX; ++x) {
			for(int y = minY; y < maxY; ++y) {
				for(int z = minZ; z < maxZ; ++z) {
					Block block = world.getBlock(x, y, z);
					if(block != null && block.isBurning()) {
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * Returns true if any block cell intersecting the given box is made of the
	 * specified material. Used by entity AI to detect standing on sand, in
	 * water, on ice, etc.
	 */
	static boolean isMaterialInBB(World world, AxisAlignedBB box, Material material) {
		int minX = MathHelper.floor_double(box.minX);
		int maxX = MathHelper.floor_double(box.maxX + 1.0D);
		int minY = MathHelper.floor_double(box.minY);
		int maxY = MathHelper.floor_double(box.maxY + 1.0D);
		int minZ = MathHelper.floor_double(box.minZ);
		int maxZ = MathHelper.floor_double(box.maxZ + 1.0D);

		for(int x = minX; x < maxX; ++x) {
			for(int y = minY; y < maxY; ++y) {
				for(int z = minZ; z < maxZ; ++z) {
					Block block = Block.blocksList[world.getBlockId(x, y, z)];
					if(block != null && block.blockMaterial == material) {
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * Returns the fraction of sample points inside the given box that have a
	 * clear line of sight to the target (0.0 = fully blocked, 1.0 = open). Used
	 * to scale damage when the entity is partially hidden behind blocks.
	 */
	static float getBlockDensity(World world, Vec3D target, AxisAlignedBB box) {
		double stepX = 1.0D / ((box.maxX - box.minX) * 2.0D + 1.0D);
		double stepY = 1.0D / ((box.maxY - box.minY) * 2.0D + 1.0D);
		double stepZ = 1.0D / ((box.maxZ - box.minZ) * 2.0D + 1.0D);
		int hits = 0;
		int totalRays = 0;

		for(float tX = 0.0F; tX <= 1.0F; tX = (float)((double)tX + stepX)) {
			for(float tY = 0.0F; tY <= 1.0F; tY = (float)((double)tY + stepY)) {
				for(float tZ = 0.0F; tZ <= 1.0F; tZ = (float)((double)tZ + stepZ)) {
					double x = box.minX + (box.maxX - box.minX) * (double)tX;
					double y = box.minY + (box.maxY - box.minY) * (double)tY;
					double z = box.minZ + (box.maxZ - box.minZ) * (double)tZ;
					if(world.rayTraceBlocks(new Vec3D(x, y, z), target) == null) {
						++hits;
					}
					++totalRays;
				}
			}
		}

		return (float)hits / (float)totalRays;
	}
}
