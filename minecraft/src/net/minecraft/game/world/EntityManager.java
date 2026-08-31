package net.minecraft.game.world;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.game.entity.Entity;
import net.minecraft.game.entity.EntityLiving;
import net.minecraft.game.entity.animal.EntityAnimal;
import net.minecraft.game.entity.misc.EntityItem;
import net.minecraft.game.entity.monster.EntityMonster;
import net.minecraft.game.world.block.tileentity.TileEntity;
import net.minecraft.game.world.chunk.Chunk;
import util.MathHelper;

/**
 * Owns the world's live-entity bookkeeping: the {@link #loadedEntityList} plus
 * the cached monster/animal counters, and the entity lifecycle — spawning,
 * the per-frame {@link #levelEntities()} update pass (which advances each
 * entity, migrates it between chunk buckets when it moves, and sweeps dead
 * entities), bulk add/unload for chunk (de)activation, and the count queries
 * used by {@link MobSpawner}.
 *
 * <p>Extracted from {@link World} in the refactor. {@code World} keeps its
 * public {@code spawnEntityInWorld}/{@code levelEntities}/{@code getLoadedEntityList}
 * (&amp; co.) entry points as thin delegates, so external callers
 * ({@link net.minecraft.client.Minecraft}, {@link Chunk}, block/entity code)
 * are unchanged.
 */
final class EntityManager {
	/** The owning world, used for chunk lookups, the player and world-access notify. */
	private final World world;
	/** Every live entity in the world, in no particular order. */
	private final List<Entity> loadedEntityList = new ArrayList<>();
	/** Cached count of {@link EntityMonster} instances in {@link #loadedEntityList}. */
	private int monsterCount;
	/** Cached count of {@link EntityAnimal} instances in {@link #loadedEntityList}. */
	private int animalCount;

	EntityManager(World world) {
		this.world = world;
	}

	List<Entity> getLoadedEntityList() {
		return this.loadedEntityList;
	}

	boolean contains(Entity entity) {
		return this.loadedEntityList.contains(entity);
	}

	int getMonsterCount() {
		return this.monsterCount;
	}

	int getAnimalCount() {
		return this.animalCount;
	}

	/**
	 * Returns a cached entity count suitable for use by {@link MobSpawner}.
	 * Dispatches to {@link #monsterCount} or {@link #animalCount} based on the
	 * supplied class — avoids a full-list scan per spawner tick.
	 */
	int getCachedEntityCount(Class<? extends EntityLiving> entityClass) {
		if(EntityMonster.class.isAssignableFrom(entityClass)) {
			return this.monsterCount;
		} else if(EntityAnimal.class.isAssignableFrom(entityClass)) {
			return this.animalCount;
		}
		return this.countEntities(entityClass);
	}

	/**
	 * Counts every entity in the world whose class is assignable from {@code entityClass}.
	 * O(n) over the entity list. Used as fallback for non-hostile/non-passive types.
	 */
	int countEntities(Class<? extends Entity> entityClass) {
		int count = 0;
		for(int i = 0; i < this.loadedEntityList.size(); ++i) {
			Entity e = this.loadedEntityList.get(i);
			if(entityClass.isAssignableFrom(e.getClass())) {
				++count;
			}
		}
		return count;
	}

	/**
	 * Adds an entity to the world: it goes into the chunk's entity list, the
	 * world entity list, the cached mob counters, and the world accesses are
	 * notified so the renderer can request textures.
	 */
	void spawnEntityInWorld(Entity entity) {
		int chunkX = MathHelper.floor_double(entity.posX / 16.0D);
		int chunkZ = MathHelper.floor_double(entity.posZ / 16.0D);
		if(!this.world.chunkExists(chunkX, chunkZ)) {
			System.out.println("Failed to add entity " + entity);
		} else {
			this.world.getChunkFromChunkCoords(chunkX, chunkZ).addEntity(entity);
			this.loadedEntityList.add(entity);
			this.updateEntityCountOnAdd(entity);
			for(int i = 0; i < this.world.worldAccesses.size(); ++i) {
				this.world.worldAccesses.get(i).obtainEntitySkin(entity);
			}
		}
	}

	/**
	 * Advances every live entity one step. Exists as the per-frame "update all
	 * entities" pass (the world itself does not call it from {@code tick()};
	 * the client does once per frame).
	 *
	 * <p>For far-away entities (beyond {@link World#ENTITY_VIEW_DISTANCE_SQ})
	 * only light timers advance ({@code ticksExisted}/{@code age}); their
	 * {@code onUpdate} is skipped. Near entities get full updates, and when one
	 * crosses a chunk boundary the chunk-level entity buckets are updated.
	 *
	 * <p>Dead entities are swept: removed from the world list, counters and
	 * textures released. onUpdate() is wrapped in a silent try/catch so a single
	 * misbehaving entity cannot freeze the entire tick loop.
	 */
	void levelEntities() {
		Entity player = this.world.playerEntity;
		double px = player != null ? player.posX : 0.0D;
		double py = player != null ? player.posY : 0.0D;
		double pz = player != null ? player.posZ : 0.0D;
		boolean hasPlayer = player != null;

		for(int i = 0; i < this.loadedEntityList.size(); ++i) {
			Entity entity = this.loadedEntityList.get(i);

			if(!entity.isDead) {
				double dx = hasPlayer ? entity.posX - px : 0.0D;
				double dy = hasPlayer ? entity.posY - py : 0.0D;
				double dz = hasPlayer ? entity.posZ - pz : 0.0D;
				double distSq = dx * dx + dy * dy + dz * dz;

				if(distSq > World.ENTITY_VIEW_DISTANCE_SQ) {
					entity.ticksExisted++;
					if (entity instanceof EntityItem) {
						((EntityItem) entity).age++;
					}
					continue;
				}

				entity.lastTickPosX = entity.posX;
				entity.lastTickPosY = entity.posY;
				entity.lastTickPosZ = entity.posZ;
				entity.prevRotationYaw = entity.rotationYaw;
				entity.prevRotationPitch = entity.rotationPitch;
				try {
					entity.onUpdate();
				} catch(Exception ignored) {
				}

				int newChunkX = MathHelper.floor_double(entity.posX / 16.0D);
				int newChunkY = MathHelper.floor_double(entity.posY / 16.0D);
				int newChunkZ = MathHelper.floor_double(entity.posZ / 16.0D);

				if(!entity.addedToChunk
					|| entity.chunkCoordX != newChunkX
					|| entity.chunkCoordY != newChunkY
					|| entity.chunkCoordZ != newChunkZ) {

					if(entity.addedToChunk && this.world.chunkExists(entity.chunkCoordX, entity.chunkCoordZ)) {
						this.world.getChunkFromChunkCoords(entity.chunkCoordX, entity.chunkCoordZ)
							.removeEntityAtIndex(entity, entity.chunkCoordY);
					}

					if(this.world.chunkExists(newChunkX, newChunkZ)) {
						this.world.getChunkFromChunkCoords(newChunkX, newChunkZ).addEntity(entity);
					} else {
						entity.addedToChunk = false;
						entity.isDead = true;
					}
				}
			}

			if(entity.isDead) {
				int deadChunkX = MathHelper.floor_double(entity.posX / 16.0D);
				int deadChunkZ = MathHelper.floor_double(entity.posZ / 16.0D);
				if(entity.addedToChunk && this.world.chunkExists(deadChunkX, deadChunkZ)) {
					this.world.getChunkFromChunkCoords(deadChunkX, deadChunkZ)
						.removeEntityAtIndex(entity, MathHelper.floor_double(entity.posY / 16.0D));
				}
				this.loadedEntityList.remove(i--);
				this.updateEntityCountOnRemove(entity);
				for(int j = 0; j < this.world.worldAccesses.size(); ++j) {
					this.world.worldAccesses.get(j).releaseEntitySkin(entity);
				}
			}
		}

		for(int i = 0; i < this.world.loadedTileEntityList.size(); ++i) {
			TileEntity tile = this.world.loadedTileEntityList.get(i);
			tile.updateEntity();
		}
	}

	/**
	 * Adds every entity in the list to the world in one bulk operation: adds to
	 * the entity list, updates cached mob counters, and notifies world accesses.
	 */
	void addLoadedEntities(List<Entity> entities) {
		this.loadedEntityList.addAll(entities);
		entities.forEach(this::updateEntityCountOnAdd);
		for(int i = 0; i < this.world.worldAccesses.size(); ++i) {
			IWorldAccess access = this.world.worldAccesses.get(i);
			for(Entity e : entities) {
				access.obtainEntitySkin(e);
			}
		}
	}

	/**
	 * Marks every entity in the list dead (the {@link #levelEntities()} loop
	 * removes dead entities from {@code loadedEntityList} in O(1) per entry),
	 * updates the cached mob counters, and notifies world accesses so the
	 * renderer drops the textures immediately.
	 *
	 * <p>Note: the entity's chunk-ownership fields are cleared when the
	 * {@link #levelEntities()} cleanup pass hits it, so the entity is safely
	 * released from its chunk without further coordination.
	 */
	void unloadEntities(List<Entity> entities) {
		for(Entity e : entities) {
			e.isDead = true;
			this.updateEntityCountOnRemove(e);
		}
		for(int i = 0; i < this.world.worldAccesses.size(); ++i) {
			IWorldAccess access = this.world.worldAccesses.get(i);
			for(Entity e : entities) {
				access.releaseEntitySkin(e);
			}
		}
	}

	/**
	 * Bumps the monster/animal cached counters after a single entity is added to
	 * {@link #loadedEntityList}.
	 */
	private void updateEntityCountOnAdd(Entity entity) {
		if(entity instanceof EntityMonster) {
			this.monsterCount++;
		} else if(entity instanceof EntityAnimal) {
			this.animalCount++;
		}
	}

	/**
	 * Bumps the monster/animal cached counters after a single entity is removed from
	 * {@link #loadedEntityList}.
	 */
	private void updateEntityCountOnRemove(Entity entity) {
		if(entity instanceof EntityMonster) {
			this.monsterCount--;
		} else if(entity instanceof EntityAnimal) {
			this.animalCount--;
		}
	}
}
