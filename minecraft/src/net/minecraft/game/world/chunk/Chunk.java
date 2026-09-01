package net.minecraft.game.world.chunk;

import com.mojang.nbt.NBTTagCompound;
import com.mojang.nbt.NBTTagList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.game.entity.Entity;
import net.minecraft.game.entity.EntityList;
import net.minecraft.game.physics.AxisAlignedBB;
import net.minecraft.game.world.EnumSkyBlock;
import net.minecraft.game.world.World;
import net.minecraft.game.world.block.Block;
import net.minecraft.game.world.block.BlockContainer;
import net.minecraft.game.world.block.tileentity.TileEntity;
import util.MathHelper;

/**
 * A 16&times;16&times;128 slab of persistent world data — the unit of generation, storage and
 * rendering. A chunk is a set of parallel, identically-indexed planes:
 *
 * <ul>
 *   <li>{@code blocks} — one byte per block cell, holding the block id (0 = air).
 *   <li>{@code data} — 4 bits per block cell: the block's metadata (orientation, partial state).
 *   <li>{@code skyLightMap} / {@code blockLightMap} — 4 bits per cell: how lit the cell is, from
 *       0 to 15, split into skylight (from above) and blocklight (torches, lava). Kept separate
 *       so {@link #relightBlock} can manage overhangs per-column without a 3-D flood fill.
 *   <li>{@code heightMap} — one byte per (x, z) column: the y of the highest cell whose light
 *       opacity is non-zero (i.e. an approximate, opaque-tops sky surface).
 *   <li>{@code entities} — the chunk's live entities, bucketed into 8 vertical 16-high segments
 *       so the renderer/AABB queries only touch the vertical band they need.
 *   <li>{@code chunkTileEntityMap} — the non-block tile entities keyed by packed cell coordinates.
 * </ul>
 *
 * <p>The young simulation uses a single boolean here to remember, across the whole render pass,
 * whether any block it sampled was "lit" ({@link #isLit}); the renderer waits one frame before
 * applying the lit-world texture set so a freshly edited chunk re-lights smoothly instead of
 * popping. A static (rather than per-chunk) flag is a genuine 2010 quirk.
 */
public final class Chunk {
	/** Chunk-local dimension along X and Z (block cells). */
	private static final int SECTION_SIZE = 16;
	/** Chunk-local height, in block cells. */
	private static final int SECTION_HEIGHT = 128;
	/** Number of vertical entity-buckets a chunk is split into (8 &times; 16 cells). */
	private static final int ENTITY_SEGMENTS = 8;
	/** Bits to left-shift a chunk coordinate to reach the world coordinate (one chunk = 16 blocks). */
	private static final int CHUNK_SHIFT = 4;
	/** Bits to shift X left to position a cell index within a 128-high 16-wide column. */
	private static final int X_SHIFT = 11;
	/** Bits to shift Z left within a 16-wide row of a chunk's block planes. */
	private static final int Z_SHIFT = 7;
	/**
	 * Bits to shift Z left within the 16&times;16 height map. This must stay a 4-bit shift
	 * (index = z &times; 16 + x); reusing {@link #Z_SHIFT} here overruns the 256-cell map and
	 * throws during {@link #generateHeightMap}.
	 */
	private static final int HEIGHTMAP_Z_SHIFT = 4;
	/** Bits to shift the tile-entity Y so its packed key is unique across the 128-high span. */
	private static final int TILE_Y_SHIFT = 10;
	/** Camera/cull flag — see the class doc for why it lives on the chunk as a static. */
	public static boolean isLit;

	private byte[] blocks;
	private World worldObj;
	private NibbleArray data;
	private NibbleArray skyLightMap;
	private NibbleArray blockLightMap;
	private byte[] heightMap;
	/** The 16&times;16 grid of biome ids (one byte per (x, z) column), indexed z-major. */
	private byte[] biomes = new byte[SECTION_SIZE * SECTION_SIZE];
	private int lowestBlockHeight;
	public final int xPosition;
	public final int zPosition;
	private Map<Integer, TileEntity> chunkTileEntityMap;
	@SuppressWarnings("unchecked")
	private List<Entity>[] entities = (List<Entity>[])new List<?>[ENTITY_SEGMENTS];
	public boolean isTerrainPopulated;
	public boolean isModified;
	private boolean hasEntities;

	private Chunk(World world, int chunkX, int chunkZ) {
		this.chunkTileEntityMap = new HashMap<>();
		this.worldObj = world;
		this.xPosition = chunkX;
		this.zPosition = chunkZ;
		this.heightMap = new byte[SECTION_SIZE * SECTION_SIZE];

		for(int i = 0; i < this.entities.length; ++i) {
			this.entities[i] = new ArrayList<>();
		}
	}

	/** Builds an empty (unpacked) chunk; used for both fresh worldgen and NBT load. */
	public Chunk(World world, byte[] blockArray, int chunkX, int chunkZ) {
		this(world, chunkX, chunkZ);
		this.blocks = blockArray;
		this.data = new NibbleArray(blockArray.length);
		this.skyLightMap = new NibbleArray(blockArray.length);
		this.blockLightMap = new NibbleArray(blockArray.length);
	}

	/** Height of the highest light-opaque block in the given column (x, z are chunk-local). */
	public final int getHeightValue(int x, int z) {
		return this.heightMap[z << HEIGHTMAP_Z_SHIFT | x] & 255;
	}

	/** The stored biome id of the given chunk-local column (x, z). */
	public final int getBiomeID(int x, int z) {
		return this.biomes[z << HEIGHTMAP_Z_SHIFT | x] & 255;
	}

	/** Records the biome id of the given chunk-local column (x, z). */
	public final void setBiome(int x, int z, int id) {
		this.biomes[z << HEIGHTMAP_Z_SHIFT | x] = (byte)id;
		this.isModified = true;
	}

	/**
	 * (Re)computes the chunk's height map and skylight from scratch. Called once the raw terrain
	 * of a newly generated chunk is in place, and again when an old save is missing its maps.
	 *
	 * <p>It works column by column: seed each column's height at the top with a flag value, let
	 * {@link #relightBlock} walk the column down to the first light-opaque block (setting the
	 * height and sky gradient in one pass), then reconcile the skylight across neighbour columns
	 * so a cliff face is dimmed where a taller neighbour blocks the sun.
	 */
	public final void generateHeightMap() {
		// First pass descends every column from the spawn of the sky, taking the null-opacity
		// walk all the way to the highest opaque block (air cells above it are trimmed).
		int lowestHeight = SECTION_HEIGHT - 1;
		for(int x = 0; x < SECTION_SIZE; ++x) {
			for(int z = 0; z < SECTION_SIZE; ++z) {
				this.heightMap[z << HEIGHTMAP_Z_SHIFT | x] = -128;
				this.relightBlock(x, SECTION_HEIGHT - 1, z);
				if((this.heightMap[z << HEIGHTMAP_Z_SHIFT | x] & 255) < lowestHeight) {
					lowestHeight = this.heightMap[z << HEIGHTMAP_Z_SHIFT | x] & 255;
				}
			}
		}
		this.lowestBlockHeight = lowestHeight;

		// Second pass cross-feeds each column's height to its four neighbours so shadow-walls
		// appear where a tall column sits beside a short one.
		for(int x = 0; x < SECTION_SIZE; ++x) {
			for(int z = 0; z < SECTION_SIZE; ++z) {
				this.updateSkylight_do(x, z);
			}
		}
		this.isModified = true;
	}

	/**
	 * Registers the four horizontal neighbours of a column as candidates for a sky-shadow
	 * update: if a neighbour's surface is higher or lower than this column's, the strip between
	 * them must be re-lit by the deferred lighting pass.
	 */
	private void updateSkylight_do(int x, int z) {
		int height = this.getHeightValue(x, z);
		int worldX = x + (this.xPosition << CHUNK_SHIFT);
		int worldZ = z + (this.zPosition << CHUNK_SHIFT);
		this.checkSkylightNeighborHeight(worldX - 1, worldZ, height);
		this.checkSkylightNeighborHeight(worldX + 1, worldZ, height);
		this.checkSkylightNeighborHeight(worldX, worldZ - 1, height);
		this.checkSkylightNeighborHeight(worldX, worldZ + 1, height);
	}

	/**
	 * If the surface at a neighbouring column is higher/​lower than this column's surface,
	 * queues a skylight update of exactly the vertical strip that differs between the two, so a
	 * height discontinuity darkens the exposed face instead of leaving it sun-bright.
	 */
	private void checkSkylightNeighborHeight(int worldX, int worldZ, int height) {
		int neighborHeight = this.worldObj.getHeightValue(worldX, worldZ);
		if(neighborHeight > height) {
			this.worldObj.scheduleLightingUpdate(EnumSkyBlock.Sky, worldX, height, worldZ, worldX, neighborHeight, worldZ);
		} else if(neighborHeight < height) {
			this.worldObj.scheduleLightingUpdate(EnumSkyBlock.Sky, worldX, neighborHeight, worldZ, worldX, height, worldZ);
		}
		this.isModified = true;
	}

	/**
	 * Recalculates the height and skylight of one column after the blocks in it changed.
	 *
	 * <p>This is where the former save-corruption bug lived: the original walked down through
	 * "blocks whose light opacity is exactly zero" as if opacity were an exact {@code 0/1} test,
	 * but several blocks {@link Block#setLightOpacity} to a <em>multi-step</em> value (water 3,
	 * leaves 1). Starting from the current height it treated such a block's own column as fully
	 * transparent and drilled straight through it to the cave floor below, so the height map and
	 * skylight silently described a lower cave and the saved height ended up wrong — which later
	 * booted into a failed-to-spawn or an empty chunk. The surface is now correctly found at the
	 * first non-zero-opacity block, matching how {@code setBlockID} seeds the column.
	 */
	private void relightBlock(int x, int y, int z) {
		int oldHeight = this.heightMap[z << HEIGHTMAP_Z_SHIFT | x] & 255;
		int newHeight = oldHeight;
		if(y > newHeight) {
			newHeight = y;
		}

		// Descend to the highest opaque block: every cell below a non-zero-opacity block keeps
		// the column's height, but run-on light-transparent cells (0) are climbed up past.
		while(newHeight > 0 && Block.lightOpacity[this.getBlockID(x, newHeight - 1, z)] == 0) {
			--newHeight;
		}

		if(newHeight != oldHeight) {
			this.worldObj.markBlocksDirtyVertical(x, z, newHeight, oldHeight);
			this.heightMap[z << HEIGHTMAP_Z_SHIFT | x] = (byte)newHeight;
			if(newHeight < this.lowestBlockHeight) {
				this.lowestBlockHeight = newHeight;
			} else {
				// Nothing got lower, so recompute the global lowest from all columns.
				int lowestHeight = SECTION_HEIGHT - 1;
				for(int scanZ = 0; scanZ < SECTION_SIZE; ++scanZ) {
					for(int scanX = 0; scanX < SECTION_SIZE; ++scanX) {
						if((this.heightMap[scanZ << HEIGHTMAP_Z_SHIFT | scanX] & 255) < lowestHeight) {
							lowestHeight = this.heightMap[scanZ << HEIGHTMAP_Z_SHIFT | scanX] & 255;
						}
					}
				}
				this.lowestBlockHeight = lowestHeight;
			}

			int worldX = (this.xPosition << CHUNK_SHIFT) + x;
			int worldZ = (this.zPosition << CHUNK_SHIFT) + z;
			if(newHeight < oldHeight) {
				// Surface dropped — the newly exposed cells are bright sky again.
				for(int skyY = newHeight; skyY < oldHeight; ++skyY) {
					this.skyLightMap.set(x, skyY, z, 15);
				}
			} else {
				// Surface rose — the buried cells darken, and the strip between is deferred.
				this.worldObj.scheduleLightingUpdate(EnumSkyBlock.Sky, worldX, oldHeight, worldZ, worldX, newHeight, worldZ);
				for(int skyY = oldHeight; skyY < newHeight; ++skyY) {
					this.skyLightMap.set(x, skyY, z, 0);
				}
			}

			// Walk back up from just under the surface, accumulating each block's opacity into
			// the sky gradient so deeper cells are progressively dimmer until fully dark.
			int skylight = 15;
			int referenceHeight = newHeight;
			for(; newHeight > 0 && skylight > 0; this.skyLightMap.set(x, newHeight, z, skylight)) {
				--newHeight;
				int opacity = Block.lightOpacity[this.getBlockID(x, newHeight, z)];
				if(opacity == 0) {
					opacity = 1;
				}
				skylight -= opacity;
				if(skylight < 0) {
					skylight = 0;
				}
			}

			// Trim the run-on transparent cell overhang again so the height/light agree, and
			// (if the surface actually moved) queue the region the neighbours should refresh.
			while(newHeight > 0 && Block.lightOpacity[this.getBlockID(x, newHeight - 1, z)] == 0) {
				--newHeight;
			}
			if(newHeight != referenceHeight) {
				this.worldObj.scheduleLightingUpdate(EnumSkyBlock.Sky, worldX - 1, newHeight, worldZ - 1, worldX + 1, referenceHeight, worldZ + 1);
			}
			this.isModified = true;
		}
	}

	/** Block id at a chunk-local cell (0 = air). Masked to unsigned (0–255). */
	public final int getBlockID(int x, int y, int z) {
		return this.blocks[x << X_SHIFT | z << Z_SHIFT | y] & 0xFF;
	}

	/** Sets a block id and updates height/skylight metadata accordingly; returns true if changed. */
	public final boolean setBlockID(int x, int y, int z, int blockID) {
		byte blockByte = (byte)blockID;
		int height = this.heightMap[z << HEIGHTMAP_Z_SHIFT | x] & 255;
		int currentBlockID = this.blocks[x << X_SHIFT | z << Z_SHIFT | y] & 0xFF;
		if(currentBlockID == blockID) {
			return false;
		}

		int worldX = (this.xPosition << CHUNK_SHIFT) + x;
		int worldZ = (this.zPosition << CHUNK_SHIFT) + z;
		if(currentBlockID != 0) {
			Block.blocksList[currentBlockID].onBlockRemoval(this.worldObj, worldX, y, worldZ);
		}

		this.blocks[x << X_SHIFT | z << Z_SHIFT | y] = blockByte;
		this.data.set(x, y, z, 0);
		if(Block.lightOpacity[blockID & 0xFF] != 0) {
			if(y >= height) {
				this.relightBlock(x, y + 1, z);
			}
		} else if(y == height - 1) {
			this.relightBlock(x, y, z);
		}

		this.worldObj.scheduleLightingUpdate(EnumSkyBlock.Sky, worldX, y, worldZ, worldX, y, worldZ);
		this.worldObj.scheduleLightingUpdate(EnumSkyBlock.Block, worldX, y, worldZ, worldX, y, worldZ);
		this.updateSkylight_do(x, z);
		if(blockID != 0) {
			Block.blocksList[blockID].onBlockAdded(this.worldObj, worldX, y, worldZ);
		}

		this.isModified = true;
		return true;
	}

	public final int getBlockMetadata(int x, int y, int z) {
		return this.data.get(x, y, z);
	}

	/**
	 * Writes a block id and its metadata in a single step. {@link #setBlockID}
	 * zeroes the metadata nibble, so the flow system cannot use it and then
	 * re-set the nibble separately without an intermediate air frame — this
	 * combined write keeps both planes coherent while still running the full
	 * height/skylight bookkeeping of the plain id set.
	 */
	public final boolean setBlockIDWithMetadata(int x, int y, int z, int blockID, int metadata) {
		boolean changed = this.setBlockID(x, y, z, blockID);
		this.setBlockMetadata(x, y, z, metadata);
		return changed;
	}

	public final void setBlockMetadata(int x, int y, int z, int metadata) {
		this.isModified = true;
		this.data.set(x, y, z, metadata);
	}

	public final int getSavedLightValue(EnumSkyBlock lightType, int x, int y, int z) {
		if(lightType == EnumSkyBlock.Sky) {
			return this.skyLightMap.get(x, y, z);
		} else {
			return lightType == EnumSkyBlock.Block ? this.blockLightMap.get(x, y, z) : 0;
		}
	}

	public final void setLightValue(EnumSkyBlock lightType, int x, int y, int z, int value) {
		this.isModified = true;
		if(lightType == EnumSkyBlock.Sky) {
			this.skyLightMap.set(x, y, z, value);
		} else if(lightType == EnumSkyBlock.Block) {
			this.blockLightMap.set(x, y, z, value);
		}
	}

	/**
	 * Combined brightness of a cell after subtracting a darkness scrim ({@code lightSubtracted}),
	 * clamped to the 4-bit range. Also notes on {@link #isLit} that real light was seen, for the
	 * renderer's one-frame re-light handshake.
	 */
	public final int getBlockLightValue(int x, int y, int z, int lightSubtracted) {
		int skyLight = this.skyLightMap.get(x, y, z);
		if(skyLight > 0) {
			isLit = true;
		}

		skyLight -= lightSubtracted;
		if(skyLight < 0) {
			skyLight = 0;
		}
		int blockLight = this.blockLightMap.get(x, y, z);
		if(blockLight > skyLight) {
			skyLight = blockLight;
		}

		return skyLight;
	}

	/** Serializes the chunk (blocks, metadata, both light planes, entities, tile entities) to NBT. */
	public final void writeChunkNBTData(NBTTagCompound nbtTag) {
		nbtTag.setInteger("xPos", this.xPosition);
		nbtTag.setInteger("zPos", this.zPosition);
		nbtTag.setLong("LastUpdate", this.worldObj.worldTime);
		nbtTag.setByteArray("Blocks", this.blocks);
		nbtTag.setByteArray("Data", this.data.data);
		nbtTag.setByteArray("SkyLight", this.skyLightMap.data);
		nbtTag.setByteArray("BlockLight", this.blockLightMap.data);
		nbtTag.setByteArray("HeightMap", this.heightMap);
		nbtTag.setByteArray("Biomes", this.biomes);
		nbtTag.setBoolean("TerrainPopulated", this.isTerrainPopulated);
		this.hasEntities = false;
		NBTTagList entityList = new NBTTagList();
		for(List<Entity> entitySegment : this.entities) {
			for(Entity entity : entitySegment) {
				NBTTagCompound entityTag = new NBTTagCompound();
				if(entity.addEntityID(entityTag)) {
					entityList.setTag(entityTag);
					this.hasEntities = true;
				}
			}
		}

		nbtTag.setTag("Entities", entityList);
		NBTTagList tileEntityList = new NBTTagList();
		for(TileEntity tileEntity : this.chunkTileEntityMap.values()) {
			NBTTagCompound tileEntityTag = new NBTTagCompound();
			tileEntity.writeToNBT(tileEntityTag);
			tileEntityList.setTag(tileEntityTag);
		}

		nbtTag.setTag("TileEntities", tileEntityList);
	}

	/** Rebuilds a chunk from its saved NBT, regenerating missing light/height maps as needed. */
	public static Chunk readChunkNBTData(World world, NBTTagCompound nbtTag) {
		int chunkX = nbtTag.getInteger("xPos");
		int chunkZ = nbtTag.getInteger("zPos");
		Chunk chunk = new Chunk(world, chunkX, chunkZ);
		chunk.blocks = nbtTag.getByteArray("Blocks");
		chunk.data = new NibbleArray(nbtTag.getByteArray("Data"));
		chunk.skyLightMap = new NibbleArray(nbtTag.getByteArray("SkyLight"));
		chunk.blockLightMap = new NibbleArray(nbtTag.getByteArray("BlockLight"));
		chunk.heightMap = nbtTag.getByteArray("HeightMap");
		chunk.isTerrainPopulated = nbtTag.getBoolean("TerrainPopulated");
		if(!chunk.data.isValid()) {
			chunk.data = new NibbleArray(chunk.blocks.length);
		}

		if(chunk.heightMap == null || !chunk.skyLightMap.isValid()) {
			chunk.heightMap = new byte[SECTION_SIZE * SECTION_SIZE];
			chunk.skyLightMap = new NibbleArray(chunk.blocks.length);
			chunk.generateHeightMap();
		}

		if(!chunk.blockLightMap.isValid()) {
			chunk.blockLightMap = new NibbleArray(chunk.blocks.length);
		}

		// Old save files have no "Biomes" tag: getByteArray returns an empty
		// (shared) array in that case. Fall back to a fresh zero-filled grid,
		// whose every cell is biome id 0 (the default world biome).
		byte[] savedBiomes = nbtTag.getByteArray("Biomes");
		if(savedBiomes.length == SECTION_SIZE * SECTION_SIZE) {
			chunk.biomes = savedBiomes;
		} else {
			chunk.biomes = new byte[SECTION_SIZE * SECTION_SIZE];
		}

		chunk.hasEntities = false;
		NBTTagList entityList = nbtTag.getTagList("Entities");
		if(entityList != null) {
			for(int i = 0; i < entityList.tagCount(); ++i) {
				NBTTagCompound entityTag = (NBTTagCompound)entityList.tagAt(i);
				Entity entity = EntityList.createEntityFromNBT(entityTag, world);
				if(entity != null) {
					chunk.hasEntities = true;
					chunk.addEntity(entity);
				}
			}
		}

		NBTTagList tileEntityList = nbtTag.getTagList("TileEntities");
		if(tileEntityList != null) {
			for(int i = 0; i < tileEntityList.tagCount(); ++i) {
				NBTTagCompound tileEntityTag = (NBTTagCompound)tileEntityList.tagAt(i);
				TileEntity tileEntity = TileEntity.createAndLoadEntity(tileEntityTag);
				if(tileEntity != null) {
					chunk.setChunkBlockTileEntity(tileEntity.xCoord - (chunkX << CHUNK_SHIFT), tileEntity.yCoord, tileEntity.zCoord - (chunkZ << CHUNK_SHIFT), tileEntity);
				}
			}
		}

		return chunk;
	}

	/**
	 * Registers an entity in this chunk, bucketed by its vertical position into one of the 8
	 * segment lists. Emits a diagnostic (but proceeds) if the caller passed a "wrong location".
	 */
	/**
	 * Adds an entity to this chunk's entity list. Computes the vertical
	 * segment from the entity's current y-position, places it in the
	 * corresponding bucket, and records the chunk coordinates on the entity
	 * (the entity's {@link Entity#addedToChunk} flag and {@code chunkCoord*}
	 * fields are the authoritative record of which chunk owns the entity).
	 */
	public final void addEntity(Entity entity) {
		int segmentIndex = MathHelper.floor_double(entity.posY / SECTION_SIZE);
		if(segmentIndex < 0) {
			segmentIndex = 0;
		}
		if(segmentIndex >= this.entities.length) {
			segmentIndex = this.entities.length - 1;
		}

		this.entities[segmentIndex].add(entity);
		entity.addedToChunk = true;
		entity.chunkCoordX = this.xPosition;
		entity.chunkCoordY = segmentIndex;
		entity.chunkCoordZ = this.zPosition;
		this.isModified = true;
	}

	/**
	 * Removes an entity from a specific vertical segment and clears the
	 * entity's chunk-ownership fields. The {@code segmentIndex} is clamped
	 * defensively. Callers must pass the same segment the entity was placed
	 * in by {@link #addEntity}.
	 */
	public final void removeEntityAtIndex(Entity entity, int segmentIndex) {
		if(segmentIndex < 0) {
			segmentIndex = 0;
		}
		if(segmentIndex >= this.entities.length) {
			segmentIndex = this.entities.length - 1;
		}

		this.entities[segmentIndex].remove(entity);
		entity.addedToChunk = false;
		this.isModified = true;
	}

	/** Whether a cell sits at or above the column's opaque surface (i.e. opens to the sky). */
	public final boolean canBlockSeeTheSky(int x, int y, int z) {
		return y >= (this.heightMap[z << HEIGHTMAP_Z_SHIFT | x] & 255);
	}

	/**
	 * Tile entity at a chunk-local cell, lazily materializing one for a {@link BlockContainer}
	 * the first time it is asked for.
	 */
	public final TileEntity getChunkBlockTileEntity(int x, int y, int z) {
		int key = packedTileKey(x, y, z);
		TileEntity tileEntity = this.chunkTileEntityMap.get(Integer.valueOf(key));
		if(tileEntity == null) {
			int blockID = this.getBlockID(x, y, z);
			BlockContainer blockContainer = (BlockContainer)Block.blocksList[blockID];
			blockContainer.onBlockAdded(this.worldObj, (this.xPosition << CHUNK_SHIFT) + x, y, (this.zPosition << CHUNK_SHIFT) + z);
			tileEntity = this.chunkTileEntityMap.get(Integer.valueOf(key));
		}
		return tileEntity;
	}

	public final void setChunkBlockTileEntity(int x, int y, int z, TileEntity tileEntity) {
		this.isModified = true;
		int key = packedTileKey(x, y, z);
		tileEntity.worldObj = this.worldObj;
		tileEntity.xCoord = (this.xPosition << CHUNK_SHIFT) + x;
		tileEntity.yCoord = y;
		tileEntity.zCoord = (this.zPosition << CHUNK_SHIFT) + z;
		int blockID = this.getBlockID(x, y, z);
		if(blockID != 0 && Block.blocksList[blockID] instanceof BlockContainer) {
			this.chunkTileEntityMap.put(Integer.valueOf(key), tileEntity);
			this.worldObj.loadedTileEntityList.add(tileEntity);
		} else {
			System.out.println("Attempted to place a tile entity where there was no entity tile!");
		}
	}

	public final void removeChunkBlockTileEntity(int x, int y, int z) {
		this.isModified = true;
		int key = packedTileKey(x, y, z);
		TileEntity removedTileEntity = this.chunkTileEntityMap.remove(Integer.valueOf(key));
		if(removedTileEntity != null) {
			this.worldObj.loadedTileEntityList.remove(removedTileEntity);
		}
	}

	/** Packs a chunk-local cell position into a single unique key for the tile-entity map. */
	private static int packedTileKey(int x, int y, int z) {
		return x + (y << TILE_Y_SHIFT) + (z << TILE_Y_SHIFT << TILE_Y_SHIFT);
	}

	public final void loadEntities() {
		this.worldObj.loadedTileEntityList.addAll(this.chunkTileEntityMap.values());
		for(List<Entity> entitySegment : this.entities) {
			this.worldObj.addLoadedEntities(entitySegment);
		}
	}

	public final void unloadEntities() {
		this.worldObj.loadedTileEntityList.removeAll(this.chunkTileEntityMap.values());
		for(List<Entity> entitySegment : this.entities) {
			this.worldObj.unloadEntities(entitySegment);
		}
	}

	/**
	 * Gathers every entity in this chunk whose box intersects {@code boundingBox} into
	 * {@code result}, skipping the probe entity itself — the collision/pathing query the world
	 * runs against the player. Only the vertical entity segments the box spans are inspected.
	 */
	public final void getEntitiesWithinAABBForEntity(Entity entity, AxisAlignedBB boundingBox, List<Entity> result) {
		int segmentStart = MathHelper.floor_double((boundingBox.minY - 2.0D) / 16.0D);
		int segmentEnd = MathHelper.floor_double((boundingBox.maxY + 2.0D) / 16.0D);
		if(segmentStart < 0) {
			segmentStart = 0;
		}
		if(segmentEnd >= this.entities.length) {
			segmentEnd = this.entities.length - 1;
		}

		for(; segmentStart <= segmentEnd; ++segmentStart) {
			List<Entity> entitySegment = this.entities[segmentStart];
			int segmentSize = entitySegment.size();
			for(int i = 0; i < segmentSize; ++i) {
				Entity candidate = entitySegment.get(i);
				if(candidate != entity && candidate.boundingBox.intersectsWith(boundingBox)) {
					result.add(candidate);
				}
			}
		}
	}

	/**
	 * Whether this chunk should be written to disk: either it was dirty since the last save, or
	 * (when being unloaded) it still hosts entities that must not be lost.
	 */
	public final boolean needsSaving(boolean unload) {
		if(this.isModified) {
			return true;
		}
		if(unload) {
			if(this.hasEntities) {
				return true;
			}
			for(List<Entity> entitySegment : this.entities) {
				if(entitySegment.size() > 0) {
					return true;
				}
			}
		}
		return false;
	}
}
