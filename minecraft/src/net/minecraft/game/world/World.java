package net.minecraft.game.world;

import com.mojang.nbt.NBTTagCompound;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.client.LoadingScreenRenderer;
import net.minecraft.game.entity.Entity;
import net.minecraft.game.entity.EntityLiving;
import net.minecraft.game.entity.misc.EntityItem;
import net.minecraft.game.entity.animal.EntityAnimal;
import net.minecraft.game.entity.animal.EntityPig;
import net.minecraft.game.entity.animal.EntitySheep;
import net.minecraft.game.entity.monster.EntityCreeper;
import net.minecraft.game.entity.monster.EntityMonster;
import net.minecraft.game.entity.monster.EntitySkeleton;
import net.minecraft.game.entity.monster.EntitySpider;
import net.minecraft.game.entity.monster.EntityZombie;
import net.minecraft.game.physics.AxisAlignedBB;
import net.minecraft.game.physics.MovingObjectPosition;
import net.minecraft.game.physics.Vec3D;
import net.minecraft.game.world.block.Block;
import net.minecraft.game.world.block.tileentity.TileEntity;
import net.minecraft.game.world.biome.BiomeGenerator;
import net.minecraft.game.world.chunk.Chunk;
import net.minecraft.game.world.chunk.ChunkProviderLoadOrGenerate;
import net.minecraft.game.world.chunk.IChunkProvider;
import net.minecraft.game.world.material.Material;
import net.minecraft.game.world.path.Pathfinder;
import util.MathHelper;

public class World implements IBlockAccess {
	/** Owns the deferred block-light update queue and its processing budget. */
	private LightingManager lightingManager;
	/** Schedules and fires delayed block ticks (fire, water/lava flow, ...). */
	private BlockTickScheduler blockTickScheduler;
	/** Owns the live-entity list, cached counters and the spawn/update/sweep lifecycle. */
	private EntityManager entityManager;

	public List<TileEntity> loadedTileEntityList;
	/** Spawns hostile mobs (max 100). Tick-driven from {@link #tick()}. */
	private MobSpawner monsterSpawner;
	/** Spawns passive mobs (max 50). Tick-driven from {@link #tick()}. */
	private MobSpawner animalSpawner;
	public long worldTime;
	private long skyColor;
	private long fogColor;
	private long cloudColor;
	int skylightSubtracted;
	private int updateLCG;
	private int distHashSeed;
	static float[] lightBrightnessTable = new float[16];
	private static final int blocksToTickPerFrame = 80;
	public static long autosavePeriod = 3600L;
	public Entity playerEntity;
	public int difficultySetting;
	public final Pathfinder pathFinder;
	public Random rand;
	public int spawnX;
	public int spawnY;
	public int spawnZ;
	public boolean isNewWorld;
	List<IWorldAccess> worldAccesses;
	private IChunkProvider chunkProvider;
	private File saveDirectory;
	private long randomSeed;
	private NBTTagCompound nbtCompoundPlayer;
	public long sizeOnDisk;
	/** The world generation options, created at world creation and persisted in level.dat. */
	public WorldOptions worldOptions;
	/** The world type, chosen at world creation and read back from level.dat when loading. */
	public WorldType worldType;
	/** Squared distance beyond which entity onUpdate() is skipped. Timers (ticksExisted, age) still advance. */
	public static final double ENTITY_VIEW_DISTANCE_SQ = 2048.0D;

	public static NBTTagCompound getWorldNBTTag(File savesDirectory, String worldName) {
		File savesFolder = new File(savesDirectory, "saves");
		File worldFolder = new File(savesFolder, worldName);
		if(!worldFolder.exists()) {
			return null;
		}
		File levelDat = new File(worldFolder, "level.dat");
		if(levelDat.exists()) {
			try {
				NBTTagCompound root = LoadingScreenRenderer.read(new FileInputStream(levelDat));
				return root.getCompoundTag("Data");
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		return null;
	}

	/**
	 * Recursively deletes the world save folder (saves/&lt;worldName&gt;/).
	 * Called by the level-select screen when the user confirms deletion.
	 */
	public static void deleteWorld(File savesDirectory, String worldName) {
		File savesFolder = new File(savesDirectory, "saves");
		File worldFolder = new File(savesFolder, worldName);
		if(worldFolder.exists()) {
			deleteFiles(worldFolder.listFiles());
			worldFolder.delete();
		}
	}

	private static void deleteFiles(File[] files) {
		for(File file : files) {
			if(file.isDirectory()) {
				deleteFiles(file.listFiles());
			}
			file.delete();
		}
	}

	public World(File workingDirectory, String worldName) {
		this(workingDirectory, worldName, (new Random()).nextLong(), new WorldOptions(), WorldType.WORLDTYPE_420);
	}

	public World(File workingDirectory, String worldName, WorldOptions options) {
		this(workingDirectory, worldName, (new Random()).nextLong(), options, WorldType.WORLDTYPE_420);
	}

	public World(File workingDirectory, String worldName, WorldOptions options, WorldType worldType) {
		this(workingDirectory, worldName, (new Random()).nextLong(), options, worldType);
	}

	private World(File workingDirectory, String worldName, long randomSeed, WorldOptions worldOptions, WorldType worldType) {
		this.lightingManager = new LightingManager(this);
		this.blockTickScheduler = new BlockTickScheduler(this);
		this.entityManager = new EntityManager(this);
		this.loadedTileEntityList = new ArrayList<>();
		this.worldTime = 0L;
		this.skylightSubtracted = 0;
		this.updateLCG = (new Random()).nextInt();
		this.distHashSeed = 1013904223;
		this.pathFinder = new Pathfinder(this);
		this.rand = new Random();
		this.monsterSpawner = new MobSpawner(this, 100, EntityMonster.class,
				new Class<?>[]{EntityZombie.class, EntitySkeleton.class, EntityCreeper.class, EntitySpider.class});
		this.animalSpawner = new MobSpawner(this, 50, EntityAnimal.class,
				new Class<?>[]{EntitySheep.class, EntityPig.class});
		this.isNewWorld = false;
		this.worldAccesses = new ArrayList<>();
		this.randomSeed = 0L;
		this.sizeOnDisk = 0L;
		this.worldOptions = worldOptions;
		this.worldType = worldType;
		workingDirectory.mkdirs();
		this.saveDirectory = new File(workingDirectory, worldName);
		this.saveDirectory.mkdirs();
		File levelDat = new File(this.saveDirectory, "level.dat");
		this.isNewWorld = !levelDat.exists();
		if(levelDat.exists()) {
			try {
				NBTTagCompound rootTag = LoadingScreenRenderer.read(new FileInputStream(levelDat));
				NBTTagCompound dataTag = rootTag.getCompoundTag("Data");
				this.randomSeed = dataTag.getLong("RandomSeed");
				this.spawnX = dataTag.getInteger("SpawnX");
				this.spawnY = dataTag.getInteger("SpawnY");
				this.spawnZ = dataTag.getInteger("SpawnZ");
				this.worldTime = dataTag.getLong("Time");
				this.sizeOnDisk = dataTag.getLong("SizeOnDisk");
				this.worldOptions.readFromNBT(dataTag);
				WorldType savedWorldType = WorldType.fromId(dataTag.getString("WorldType"));
				if(savedWorldType != null) {
					this.worldType = savedWorldType;
				}
				this.nbtCompoundPlayer = dataTag.getCompoundTag("Player");
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}

		if(this.randomSeed == 0L) {
			this.randomSeed = randomSeed;
			this.spawnX = 0;
			this.spawnY = 64;
			this.spawnZ = 0;
		}

		// The world type sets the atmosphere palette: a loaded world keeps the
		// palette of the type saved in its level.dat, a new world uses the one
		// passed in (defaulting to WORLDTYPE_420 above).
		this.skyColor = this.worldType.getSkyColor();
		this.fogColor = this.worldType.getFogColor();
		this.cloudColor = this.worldType.getCloudColor();

		this.chunkProvider = new ChunkProviderLoadOrGenerate(this, this.saveDirectory, this.worldType.createChunkProvider(this, this.randomSeed, this.worldOptions));
		this.saveWorld(false);
	}

	/**
	 * Reads the saved player NBT from level.dat and spawns the player entity
	 * into the world. Called once after the world and player have been
	 * constructed, before the first frame.
	 */
	public final void spawnPlayer() {
		try {
			if(this.nbtCompoundPlayer != null) {
				this.playerEntity.readFromNBT(this.nbtCompoundPlayer);
				this.nbtCompoundPlayer = null;
			}
			this.spawnEntityInWorld(this.playerEntity);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * Persists the world state to level.dat and optionally saves all dirty chunks.
	 *
	 * @param saveEntities true to flush entity positions back into the level file;
	 *                     false when autosaving (entities are already persisted on remove)
	 */
	private void saveWorld(boolean saveEntities) {
		File levelDat = new File(this.saveDirectory, "level.dat");
		NBTTagCompound dataTag = new NBTTagCompound();
		dataTag.setLong("RandomSeed", this.randomSeed);
		dataTag.setInteger("SpawnX", this.spawnX);
		dataTag.setInteger("SpawnY", this.spawnY);
		dataTag.setInteger("SpawnZ", this.spawnZ);
		dataTag.setLong("Time", this.worldTime);
		dataTag.setLong("SizeOnDisk", this.sizeOnDisk);
		dataTag.setLong("LastPlayed", System.currentTimeMillis());
		dataTag.setString("WorldType", this.worldType.getId());
		this.worldOptions.writeToNBT(dataTag);
		NBTTagCompound playerTag;
		if(this.playerEntity != null) {
			playerTag = new NBTTagCompound();
			this.playerEntity.writeToNBT(playerTag);
			dataTag.setCompoundTag("Player", playerTag);
		}
		playerTag = new NBTTagCompound();
		playerTag.setTag("Data", dataTag);
		try {
			LoadingScreenRenderer.write(playerTag, new FileOutputStream(levelDat));
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		this.chunkProvider.saveChunks(saveEntities);
	}

	/**
	 * Returns the block id at the given world coordinates. Bedrock (y &lt;= 0)
	 * is reported as still lava; out-of-range heights (y &gt;= 128) report
	 * air (0).
	 */
	public final int getBlockId(int x, int y, int z) {
		return y <= 0 ? Block.lavaStill.blockID : (y >= 128 ? 0 : this.getChunkFromChunkCoords(x >> 4, z >> 4).getBlockID(x & 15, y, z & 15));
	}

	/** True if there is a generated chunk covering (x, z) and y is in the world height range. */
	public final boolean blockExists(int x, int y, int z) {
		return y >= 0 && y < 128 ? this.chunkExists(x >> 4, z >> 4) : false;
	}

	boolean chunkExists(int chunkX, int chunkZ) {
		return this.chunkProvider.chunkExists(chunkX, chunkZ);
	}

	/**
	 * True when every chunk covering the box [x1..x2, y1..y2, z1..z2] is
	 * already generated (i.e. no chunk will be created by this call). Used
	 * before entity AABB queries so we can early-out on an unloaded world.
	 */
	public final boolean checkChunksExist(int x1, int y1, int z1, int x2, int y2, int z2) {
		if(y2 >= 0 && y1 < 128) {
			x1 >>= 4;
			y1 >>= 4;
			z1 >>= 4;
			x2 >>= 4;
			y2 >>= 4;
			z2 >>= 4;
			for(int chunkX = x1; chunkX <= x2; ++chunkX) {
				for(int chunkZ = z1; chunkZ <= z2; ++chunkZ) {
					if(!this.chunkExists(chunkX, chunkZ)) {
						return false;
					}
				}
			}
			return true;
		}
		return false;
	}

	public Chunk getChunkFromChunkCoords(int chunkX, int chunkZ) {
		return this.chunkProvider.provideChunk(chunkX, chunkZ);
	}

	/** Silent block id write: no render update, no neighbour notification. */
	public final boolean setTileNoUpdate(int x, int y, int z, int blockID) {
		if(y < 0) {
			return false;
		} else if(y >= 128) {
			return false;
		} else {
			Chunk chunk = this.getChunkFromChunkCoords(x >> 4, z >> 4);
			return chunk.setBlockID(x & 15, y, z & 15, blockID);
		}
	}

	public final Material getBlockMaterial(int x, int y, int z) {
		int blockID = this.getBlockId(x, y, z);
		return blockID == 0 ? Material.air : Block.blocksList[blockID].blockMaterial;
	}

	public final int getBlockMetadata(int x, int y, int z) {
		if(y < 0) {
			return 0;
		} else if(y >= 128) {
			return 0;
		} else {
			Chunk chunk = this.getChunkFromChunkCoords(x >> 4, z >> 4);
			x &= 15;
			z &= 15;
			return chunk.getBlockMetadata(x, y, z);
		}
	}

	/**
	 * Convenience: returns the {@link Block} at the given coordinates, or
	 * {@code null} if the cell is air.
	 */
	public Block getBlock(int x, int y, int z) {
		int id = this.getBlockId(x, y, z);
		return id == 0 ? null : Block.blocksList[id];
	}

	/**
	 * True when the block at (x, y, z) exists and
	 * {@link Block#canGrowPlants} is true for its metadata. This is the single
	 * place that answers "can a plant grow on the block below?".
	 */
	public boolean canPlantsGrowOn(int x, int y, int z) {
		Block block = this.getBlock(x, y, z);
		return block != null && block.canGrowPlants(this.getBlockMetadata(x, y, z));
	}

	public final void setBlockMetadataWithNotify(int x, int y, int z, int metadata) {
		if(this.setBlockMetadata(x, y, z, metadata)) {
			this.markBlockNeedsUpdate(x, y, z);
			int blockID = this.getBlockId(x, y, z);
			this.notifyBlocksOfNeighborChange(x, y, z, blockID);
		}
	}

	private boolean setBlockMetadata(int x, int y, int z, int metadata) {
		if(y < 0) {
			return false;
		} else if(y >= 128) {
			return false;
		} else {
			Chunk chunk = this.getChunkFromChunkCoords(x >> 4, z >> 4);
			x &= 15;
			z &= 15;
			chunk.setBlockMetadata(x, y, z, metadata);
			return true;
		}
	}

	/** Sets a block id and its metadata in one silent write (no render update, no neighbour notifications). */
	public final boolean setBlockAndMetadata(int x, int y, int z, int blockID, int metadata) {
		if(y < 0) {
			return false;
		} else if(y >= 128) {
			return false;
		} else {
			Chunk chunk = this.getChunkFromChunkCoords(x >> 4, z >> 4);
			return chunk.setBlockIDWithMetadata(x & 15, y, z & 15, blockID, metadata);
		}
	}

	/** Sets a block id and its metadata, then re-renders the cell and tells its neighbours. */
	public final boolean setBlockAndMetadataWithNotify(int x, int y, int z, int blockID, int metadata) {
		if(this.setBlockAndMetadata(x, y, z, blockID, metadata)) {
			this.markBlockNeedsUpdate(x, y, z);
			this.notifyBlocksOfNeighborChange(x, y, z, blockID);
			return true;
		} else {
			return false;
		}
	}

	public final boolean setBlockWithNotify(int x, int y, int z, int blockID) {
		if(!this.setTileNoUpdate(x, y, z, blockID)) {
			return false;
		}
		// Notify every world access (the renderer, the lighting queue) that
		// this cell and its six neighbours need a re-render.
		for(int i = 0; i < this.worldAccesses.size(); ++i) {
			this.worldAccesses.get(i).markBlockAndNeighborsNeedsUpdate(x, y, z);
		}
		this.notifyBlocksOfNeighborChange(x, y, z, blockID);
		return true;
	}

	/** Flags a single cell's renderer for rebuild (used whenever a block's state visibly changes). */
	public final void markBlockNeedsUpdate(int x, int y, int z) {
		this.markBlocksDirty(x, y, z, x, y, z);
	}

	public final void markBlocksDirtyVertical(int x, int y1, int z, int y2) {
		this.markBlocksDirty(x, z, y1, x, z, y2);
	}

	/** Flags every cell inside the given box for renderer rebuild. */
	public final void markBlocksDirty(int x1, int y1, int z1, int x2, int y2, int z2) {
		for(int i = 0; i < this.worldAccesses.size(); ++i) {
			this.worldAccesses.get(i).markBlockRangeNeedsUpdate(x1, y1, z1, x2, y2, z2);
		}
	}

	/**
	 * Swaps the two blocks at the given positions: copies block id and metadata
	 * in both directions, then notifies neighbours of both cells. Used by
	 * sand/gravel falling-block physics and piston block swapping.
	 */
	public final void swap(int x1, int y1, int z1, int x2, int y2, int z2) {
		int block1 = this.getBlockId(x1, y1, z1);
		int meta1 = this.getBlockMetadata(x1, y1, z1);
		int block2 = this.getBlockId(x2, y2, z2);
		int meta2 = this.getBlockMetadata(x2, y2, z2);
		this.setTileNoUpdate(x1, y1, z1, block2);
		this.setBlockMetadata(x1, y1, z1, meta2);
		this.setTileNoUpdate(x2, y2, z2, block1);
		this.setBlockMetadata(x2, y2, z2, meta1);
		this.notifyBlocksOfNeighborChange(x1, y1, z1, block2);
		this.notifyBlocksOfNeighborChange(x2, y2, z2, block1);
	}

	/**
	 * Notifies the six orthogonal neighbours of (x, y, z) that the block
	 * id at (x, y, z) changed to {@code changedBlockID}. Triggers
	 * redstone updates, falling-block detection, etc.
	 */
	public final void notifyBlocksOfNeighborChange(int x, int y, int z, int changedBlockID) {
		this.notifyBlockOfNeighborChange(x - 1, y, z, changedBlockID);
		this.notifyBlockOfNeighborChange(x + 1, y, z, changedBlockID);
		this.notifyBlockOfNeighborChange(x, y - 1, z, changedBlockID);
		this.notifyBlockOfNeighborChange(x, y + 1, z, changedBlockID);
		this.notifyBlockOfNeighborChange(x, y, z - 1, changedBlockID);
		this.notifyBlockOfNeighborChange(x, y, z + 1, changedBlockID);
	}

	private void notifyBlockOfNeighborChange(int x, int y, int z, int changedBlockID) {
		Block neighbour = Block.blocksList[this.getBlockId(x, y, z)];
		if(neighbour != null) {
			neighbour.onNeighborBlockChange(this, x, y, z, changedBlockID);
		}
	}

	public final boolean canBlockSeeTheSky(int x, int y, int z) {
		return this.getChunkFromChunkCoords(x >> 4, z >> 4).canBlockSeeTheSky(x & 15, y, z & 15);
	}

	public final int getBlockLightValue(int x, int y, int z) {
		return this.getBlockLightValue_do(x, y, z, true);
	}

	/**
	 * Internal recursive helper for {@link #getBlockLightValue}. The first
	 * call (with {@code firstCall = true}) checks if the block is a stair
	 * or tilled field, which have their effective light value taken from
	 * the brightest neighbour. Subsequent calls just read the chunk's
	 * light value.
	 */
	private int getBlockLightValue_do(int x, int y, int z, boolean firstCall) {
		int lightValue;
		if(firstCall) {
			lightValue = this.getBlockId(x, y, z);
			if(lightValue == Block.stairSingle.blockID || lightValue == Block.tilledField.blockID) {
				lightValue = this.getBlockLightValue_do(x, y + 1, z, false);
				int eastValue  = this.getBlockLightValue_do(x + 1, y, z, false);
				int westValue  = this.getBlockLightValue_do(x - 1, y, z, false);
				int northValue = this.getBlockLightValue_do(x, y, z + 1, false);
				x = this.getBlockLightValue_do(x, y, z - 1, false);
				if(eastValue  > lightValue) lightValue = eastValue;
				if(westValue  > lightValue) lightValue = westValue;
				if(northValue > lightValue) lightValue = northValue;
				if(x          > lightValue) lightValue = x;
				return lightValue;
			}
		}

		if(y < 0) {
			return 0;
		} else if(y >= 128) {
			lightValue = 15 - this.skylightSubtracted;
			if(lightValue < 0) {
				lightValue = 0;
			}
			return lightValue;
		} else {
			Chunk chunk = this.getChunkFromChunkCoords(x >> 4, z >> 4);
			x &= 15;
			z &= 15;
			return chunk.getBlockLightValue(x, y, z, this.skylightSubtracted);
		}
	}

	public final boolean canExistingBlockSeeTheSky(int x, int y, int z) {
		if(y < 0) {
			return false;
		} else if(y >= 128) {
			return true;
		} else if(!this.chunkExists(x >> 4, z >> 4)) {
			return false;
		} else {
			Chunk chunk = this.getChunkFromChunkCoords(x >> 4, z >> 4);
			x &= 15;
			z &= 15;
			return chunk.canBlockSeeTheSky(x, y, z);
		}
	}

	public final int getHeightValue(int x, int z) {
		if(!this.chunkExists(x >> 4, z >> 4)) {
			return 0;
		} else {
			Chunk chunk = this.getChunkFromChunkCoords(x >> 4, z >> 4);
			return chunk.getHeightValue(x & 15, z & 15);
		}
	}

	/**
	 * Returns the biome at the given world column. Extracts the chunk at
	 * (x &gt;&gt; 4, z &gt;&gt; 4) and resolves the biome id stored at the
	 * (x &amp; 15, z &amp; 15) cell of its biome grid through the world type's
	 * {@link net.minecraft.game.world.biome.BiomeProvider}.
	 */
	public final BiomeGenerator getBiome(int x, int z) {
		Chunk chunk = this.getChunkFromChunkCoords(x >> 4, z >> 4);
		return this.worldType.getBiomeProvider().getBiomeFromID(chunk.getBiomeID(x & 15, z & 15));
	}

	/**
	 * Called by neighbours when their own state changes, so we can recompute
	 * the saved light value at (x, y, z). For example, removing a ceiling
	 * block lets skylight in, so {@code nextLightValue} is set to 15 if the
	 * sky is now visible, or replaced with the block's own emissive light
	 * value for the Block sky.
	 */
	public final void neighborLightPropagationChanged(EnumSkyBlock lightType, int x, int y, int z, int nextLightValue) {
		if(!this.blockExists(x, y, z)) {
			return;
		}

		if(lightType == EnumSkyBlock.Sky) {
			if(this.canExistingBlockSeeTheSky(x, y, z)) {
				nextLightValue = 15;
			}
		} else if(lightType == EnumSkyBlock.Block) {
			int blockID = this.getBlockId(x, y, z);
			if(Block.lightValue[blockID] > nextLightValue) {
				nextLightValue = Block.lightValue[blockID];
			}
		}

		if(this.getSavedLightValue(lightType, x, y, z) != nextLightValue) {
			this.scheduleLightingUpdate(lightType, x, y, z, x, y, z);
		}
	}

	public final int getSavedLightValue(EnumSkyBlock lightType, int x, int y, int z) {
		if(y < 0 || y >= 128) {
			return lightType.defaultLightValue;
		}

		int chunkX = x >> 4;
		int chunkZ = z >> 4;
		if(!this.chunkExists(chunkX, chunkZ)) {
			return 0;
		}

		Chunk chunk = this.getChunkFromChunkCoords(chunkX, chunkZ);
		return chunk.getSavedLightValue(lightType, x & 15, y, z & 15);
	}

	public final float getBrightness(int x, int y, int z) {
		return lightBrightnessTable[this.getBlockLightValue(x, y, z)];
	}

	/** True between local sunrise and sunset (the 8 light-level threshold). */
	public final boolean isDaytime() {
		return this.skylightSubtracted < 8;
	}

	/**
	 * Voxel-based ray cast from rayStart to rayEnd. Steps through the world
	 * one axis-aligned unit cube at a time and returns the first collidable
	 * block intersection found, or null if no block is hit within 20 steps.
	 *
	 * @param rayStart  Start of the ray
	 * @param rayEnd    End of the ray
	 * @return a {@link MovingObjectPosition} with sub-block hit information, or null
	 */
	public final MovingObjectPosition rayTraceBlocks(Vec3D rayStart, Vec3D rayEnd) {
		if(!Double.isNaN(rayStart.xCoord) && !Double.isNaN(rayStart.yCoord) && !Double.isNaN(rayStart.zCoord)
			&& !Double.isNaN(rayEnd.xCoord) && !Double.isNaN(rayEnd.yCoord) && !Double.isNaN(rayEnd.zCoord)) {

			// Integer grid cell of the ray end (the target).
			int targetX = MathHelper.floor_double(rayEnd.xCoord);
			int targetY = MathHelper.floor_double(rayEnd.yCoord);
			int targetZ = MathHelper.floor_double(rayEnd.zCoord);

			// Integer grid cell of the ray start (the current position during stepping).
			int currentX = MathHelper.floor_double(rayStart.xCoord);
			int currentY = MathHelper.floor_double(rayStart.yCoord);
			int currentZ = MathHelper.floor_double(rayStart.zCoord);

			int stepsRemaining = 20;

			while(stepsRemaining-- >= 0) {
				if(Double.isNaN(rayStart.xCoord) || Double.isNaN(rayStart.yCoord) || Double.isNaN(rayStart.zCoord)) {
					return null;
				}

				if(currentX == targetX && currentY == targetY && currentZ == targetZ) {
					return null;
				}

				// For each axis, decide which face of the current cell we will cross next:
				// the t value is the distance along the ray to the plane of that face.
				double tCrossX = 999.0D;
				double tCrossY = 999.0D;
				double tCrossZ = 999.0D;

				if(targetX > currentX) {
					tCrossX = (double)currentX + 1.0D;
				}
				if(targetX < currentX) {
					tCrossX = (double)currentX;
				}

				if(targetY > currentY) {
					tCrossY = (double)currentY + 1.0D;
				}
				if(targetY < currentY) {
					tCrossY = (double)currentY;
				}

				if(targetZ > currentZ) {
					tCrossZ = (double)currentZ + 1.0D;
				}
				if(targetZ < currentZ) {
					tCrossZ = (double)currentZ;
				}

				// Direction vector of the ray.
				double dirX = 999.0D;
				double dirY = 999.0D;
				double dirZ = 999.0D;
				double dX = rayEnd.xCoord - rayStart.xCoord;
				double dY = rayEnd.yCoord - rayStart.yCoord;
				double dZ = rayEnd.zCoord - rayStart.zCoord;

				if(tCrossX != 999.0D) {
					dirX = (tCrossX - rayStart.xCoord) / dX;
				}
				if(tCrossY != 999.0D) {
					dirY = (tCrossY - rayStart.yCoord) / dY;
				}
				if(tCrossZ != 999.0D) {
					dirZ = (tCrossZ - rayStart.zCoord) / dZ;
				}

				byte faceDir;
				if(dirX < dirY && dirX < dirZ) {
					// Crossed the X face.
					faceDir = targetX > currentX ? (byte)4 : (byte)5;
					rayStart.xCoord = tCrossX;
					rayStart.yCoord += dY * dirX;
					rayStart.zCoord += dZ * dirX;
				} else if(dirY < dirZ) {
					// Crossed the Y face.
					faceDir = targetY > currentY ? (byte)0 : (byte)1;
					rayStart.xCoord += dX * dirY;
					rayStart.yCoord = tCrossY;
					rayStart.zCoord += dZ * dirY;
				} else {
					// Crossed the Z face.
					faceDir = targetZ > currentZ ? (byte)2 : (byte)3;
					rayStart.xCoord += dX * dirZ;
					rayStart.yCoord += dY * dirZ;
					rayStart.zCoord = tCrossZ;
				}

				// Snap the updated ray start to the integer grid cell we just entered.
				Vec3D nextPos = new Vec3D(rayStart.xCoord, rayStart.yCoord, rayStart.zCoord);
				currentX = (int)(nextPos.xCoord = (double)MathHelper.floor_double(rayStart.xCoord));
				if(faceDir == 5) {
					--currentX;
					++nextPos.xCoord;
				}

				currentY = (int)(nextPos.yCoord = (double)MathHelper.floor_double(rayStart.yCoord));
				if(faceDir == 1) {
					--currentY;
					++nextPos.yCoord;
				}

				currentZ = (int)(nextPos.zCoord = (double)MathHelper.floor_double(rayStart.zCoord));
				if(faceDir == 3) {
					--currentZ;
					++nextPos.zCoord;
				}

				int blockID = this.getBlockId(currentX, currentY, currentZ);
				Block block = Block.blocksList[blockID];
				if(blockID > 0 && block.isCollidable()) {
					MovingObjectPosition hit = block.collisionRayTrace(this, currentX, currentY, currentZ, rayStart, rayEnd);
					if(hit != null) {
						return hit;
					}
				}
			}

			return null;
		}
		return null;
	}

	/**
	 * Plays a sound attached to an entity. The sound is dropped if the player
	 * is further than 16 blocks (scaled up by volume).
	 */
	public final void playSoundAtEntity(Entity entity, String soundName, float volume, float pitch) {
		if(this.playerEntity == null) {
			return;
		}
		float range = 16.0F;
		if(volume > 1.0F) {
			range = 16.0F * volume;
		}
		for(int i = 0; i < this.worldAccesses.size(); ++i) {
			if(this.playerEntity.getDistanceSqToEntity(entity) < (double)(range * range)) {
				this.worldAccesses.get(i).playSound(soundName, entity.posX, entity.posY - (double)entity.yOffset, entity.posZ, volume, pitch);
			}
		}
	}

	/**
	 * Plays a positional sound effect at the given world coordinates. Drops
	 * the sound if the player is further than 16 blocks (scaled up by volume).
	 * Wrapped in try/catch so a bad access listener cannot freeze the world.
	 */
	public final void playSoundEffect(double x, double y, double z, String soundName, float volume, float pitch) {
		if(this.playerEntity == null) {
			return;
		}
		try {
			float range = 16.0F;
			if(volume > 1.0F) {
				range = 16.0F * volume;
			}
			double dx = x - this.playerEntity.posX;
			double dy = y - this.playerEntity.posY;
			double dz = z - this.playerEntity.posZ;
			if(dx * dx + dy * dy + dz * dz < (double)(range * range)) {
				for(int i = 0; i < this.worldAccesses.size(); ++i) {
					this.worldAccesses.get(i).playSound(soundName, x, y, z, volume, pitch);
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * Broadcasts a particle spawn to all world accesses (the renderer, etc.).
	 * The actual visual type is chosen by the renderer based on the {@code particleName}.
	 */
	public final void spawnParticle(String particleName, double x, double y, double z, double dx, double dy, double dz) {
		for(int i = 0; i < this.worldAccesses.size(); ++i) {
			this.worldAccesses.get(i).spawnParticle(particleName, x, y, z, dx, dy, dz);
		}
	}

	/**
	 * Adds an entity to the world: it goes into the chunk's entity list, the
	 * world entity list, the cached mob counters, and the world accesses are
	 * notified so the renderer can request textures.
	 */
	public final void spawnEntityInWorld(Entity entity) {
		this.entityManager.spawnEntityInWorld(entity);
	}

	/** Marks the entity dead so the next levelEntities() pass will remove it. */
	public static void setEntityDead(Entity entity) {
		entity.isDead = true;
	}

	public final void addWorldAccess(IWorldAccess access) {
		this.worldAccesses.add(access);
	}

	public final void removeWorldAccess(IWorldAccess access) {
		this.worldAccesses.remove(access);
	}

	/**
	 * Returns every block's collision AABB inside the given query box. Used by
	 * entity physics to find which solid cells the entity is overlapping.
	 */
	public final List<AxisAlignedBB> getCollidingBoundingBoxes(AxisAlignedBB queryBox) {
		return EntityQueryService.getCollidingBoundingBoxes(this, queryBox);
	}

	/**
	 * Returns the sky-color RGB for the current celestial angle, dimmed by a
	 * day/night factor. The factor is a cosine wave around 0.5 (1.0 = day, 0.0
	 * = midnight), so the color is multiplied by it.
	 */
	public final Vec3D getSkyColor(float celestialAngle) {
		return AtmosphereCalculator.getSkyColor(this.skyColor, AtmosphereCalculator.getCelestialAngle(this.worldTime, celestialAngle));
	}

	/**
	 * Returns the world time in [0, 1) by combining the world time tick (mod
	 * 24000, the length of one in-game day) with the render partial tick.
	 * Subtracts 0.15 to roughly align the 0 with sunrise.
	 */
	public final float getCelestialAngle(float partialTick) {
		return AtmosphereCalculator.getCelestialAngle(this.worldTime, partialTick);
	}

	/**
	 * Returns the cloud-color RGB. Same formula as {@link #getSkyColor} but
	 * with an asymmetric tint (clouds are slightly warmer at night).
	 */
	public final Vec3D getCloudColor(float celestialAngle) {
		return AtmosphereCalculator.getCloudColor(this.cloudColor, AtmosphereCalculator.getCelestialAngle(this.worldTime, celestialAngle));
	}

	/** Returns the fog-color RGB. Asymmetric tint makes the horizon look warmer. */
	public final Vec3D getFogColor(float celestialAngle) {
		return AtmosphereCalculator.getFogColor(this.fogColor, AtmosphereCalculator.getCelestialAngle(this.worldTime, celestialAngle));
	}

	/**
	 * Star brightness is a 0..1 value, brightest near midnight, drops to 0 in
	 * daylight. Cubic falloff to make stars visibly snap on at dusk.
	 */
	public final float getStarBrightness(float celestialAngle) {
		return AtmosphereCalculator.getStarBrightness(AtmosphereCalculator.getCelestialAngle(this.worldTime, celestialAngle));
	}

	/**
	 * Queues a block tick for a future update. {@code blockID} is the block id
	 * at this position that should be ticked. If non-zero, the block's own
	 * {@link Block#tickRate()} defines how many ticks to wait.
	 */
	public final void scheduleBlockUpdate(int x, int y, int z, int blockID) {
		this.blockTickScheduler.scheduleBlockUpdate(x, y, z, blockID);
	}

	/**
	 * Per-tick entity update pass. Advances every live entity, migrates it to
	 * the correct chunk when it crosses a boundary, and sweeps dead entities.
	 * Far-away entities only get their despawn timers advanced. See
	 * {@link EntityManager#levelEntities()}.
	 */
	public final void levelEntities() {
		this.entityManager.levelEntities();
	}

	/**
	 * Returns true if no entity in the given box has {@code preventEntitySpawning}
	 * set, i.e. the area is open for an entity to spawn. Used by
	 * {@link MobSpawner} to validate a candidate spawn position.
	 */
	public final boolean checkIfAABBIsClear1(AxisAlignedBB box) {
		return EntityQueryService.checkIfAABBIsClear1(this, box);
	}

	/**
	 * Returns true if any block cell intersecting the given box is a liquid.
	 * Used by entity physics to know if a position is submerged.
	 */
	public final boolean getIsAnyLiquid(AxisAlignedBB box) {
		return EntityQueryService.getIsAnyLiquid(this, box);
	}

	/**
	 * Returns true if any block cell intersecting the given box is fire or
	 * lava. Used by entity AI to decide whether to take fire damage.
	 */
	public final boolean isBoundingBoxBurning(AxisAlignedBB box) {
		return EntityQueryService.isBoundingBoxBurning(this, box);
	}

	/**
	 * Returns true if any block cell intersecting the given box is made of the
	 * specified material. Used by entity AI to detect standing on sand, in
	 * water, on ice, etc.
	 */
	public final boolean isMaterialInBB(AxisAlignedBB box, Material material) {
		return EntityQueryService.isMaterialInBB(this, box, material);
	}

	/**
	 * Creates and executes an explosion at the specified location.
	 *
	 * @param entity The entity causing the explosion (may be null for world-generated explosions)
	 * @param x X coordinate of explosion center
	 * @param y Y coordinate of explosion center
	 * @param z Z coordinate of explosion center
	 * @param size Explosion radius/size
	 */
	public final void createExplosion(Entity entity, double x, double y, double z, float size) {
		Explosion explosion = new Explosion(this, entity, x, y, z, size);
		explosion.explode();
		explosion.applyEffects();
	}

	/**
	 * Returns the fraction of rays from the entity's bounding box that reach
	 * the given target point without hitting a block. 1.0 = unobstructed,
	 * 0.0 = fully blocked. Used by the entity renderer to decide whether
	 * the camera is inside a solid block.
	 */
	public final float getBlockDensity(Vec3D target, AxisAlignedBB box) {
		return EntityQueryService.getBlockDensity(this, target, box);
	}

	/**
	 * Removes fire at a position adjacent to (x, y, z) in the direction
	 * given by {@code face} (0-5 matching the block-face constants in
	 * {@link Block}). Plays a "fizz" sound and replaces the fire with air.
	 */
	public final void extinguishFire(int x, int y, int z, int face) {
		if(face == 0) --y;
		if(face == 1) ++y;
		if(face == 2) --z;
		if(face == 3) ++z;
		if(face == 4) --x;
		if(face == 5) ++x;

		if(this.getBlockId(x, y, z) == Block.fire.blockID) {
			this.playSoundEffect((double)((float)x + 0.5F), (double)((float)y + 0.5F), (double)((float)z + 0.5F),
				"random.fizz", 0.5F, 2.6F + (this.rand.nextFloat() - this.rand.nextFloat()) * 0.8F);
			this.setBlockWithNotify(x, y, z, 0);
		}
	}

	/** Debug helper: returns a one-line summary of all loaded entities. */
	public final String getDebugLoadedEntities() {
		return "All: " + this.entityManager.getLoadedEntityList().size();
	}

	public final Entity getPlayerEntity() {
		return this.playerEntity;
	}

	public final TileEntity getBlockTileEntity(int x, int y, int z) {
		Chunk chunk = this.getChunkFromChunkCoords(x >> 4, z >> 4);
		return chunk != null ? chunk.getChunkBlockTileEntity(x & 15, y, z & 15) : null;
	}

	public final void setBlockTileEntity(int x, int y, int z, TileEntity tile) {
		Chunk chunk = this.getChunkFromChunkCoords(x >> 4, z >> 4);
		if(chunk != null) {
			chunk.setChunkBlockTileEntity(x & 15, y, z & 15, tile);
		}
	}

	public final void removeBlockTileEntity(int x, int y, int z) {
		Chunk chunk = this.getChunkFromChunkCoords(x >> 4, z >> 4);
		if(chunk != null) {
			chunk.removeChunkBlockTileEntity(x & 15, y, z & 15);
		}
	}

	/** True if the block at (x, y, z) is opaque (used by the world for solidity checks). */
	public final boolean isSolid(int x, int y, int z) {
		Block block = Block.blocksList[this.getBlockId(x, y, z)];
		return block == null ? false : block.isOpaqueCube();
	}

	public final void saveWorldIndirectly() {
		this.saveWorld(true);
	}

	/** Number of pending light-update boxes waiting to be processed. */
	public final int lightUpdatesNeeded() {
		return this.lightingManager.lightUpdatesNeeded();
	}

	/**
	 * Pops pending light-update boxes off the back of the queue and processes
	 * them, up to a fixed budget per call. Returns true if the budget was hit
	 * (caller should call again next tick).
	 */
	public final boolean updatingLighting() {
		return this.lightingManager.updatingLighting();
	}

	/**
	 * Schedules a light-update box. Tries to merge into the four most recent
	 * entries of the same light type to keep the queue short. If the queue
	 * grows too large, half is drained by processing on the calling thread.
	 */
	public final void scheduleLightingUpdate(EnumSkyBlock lightType, int x1, int y1, int z1, int x2, int y2, int z2) {
		this.lightingManager.scheduleLightingUpdate(lightType, x1, y1, z1, x2, y2, z2);
	}

	/**
	 * Package-private: writes a single light value into the chunk's storage
	 * and notifies the renderer of the changed cell.
	 */
	final void setLightValue(EnumSkyBlock lightType, int x, int y, int z, int lightValue) {
		if(y >= 0 && y < 128 && this.chunkExists(x >> 4, z >> 4)) {
			Chunk chunk = this.getChunkFromChunkCoords(x >> 4, z >> 4);
			chunk.setLightValue(lightType, x & 15, y, z & 15, lightValue);
			for(int i = 0; i < this.worldAccesses.size(); ++i) {
				this.worldAccesses.get(i).markBlockAndNeighborsNeedsUpdate(x, y, z);
			}
		}
	}

	/**
	 * Per-tick world update. Order of operations:
	 * <ol>
	 *   <li>unload the 100 oldest chunks;</li>
	 *   <li>re-attach the player if they were removed from the entity list;</li>
	 *   <li>run monster and animal spawners;</li>
	 *   <li>update the day/night light level and, on change, force a full render rebuild;</li>
	 *   <li>advance the world time and possibly autosave;</li>
	 *   <li>process up to 200 scheduled block ticks;</li>
	 *   <li>run the random block-tick pass within 8 chunks of the player.</li>
	 * </ol>
	 */
	public final void tick() {
		this.chunkProvider.unload100OldestChunks();
		if(!this.entityManager.contains(this.playerEntity)) {
			this.spawnEntityInWorld(this.playerEntity);
		}

		this.monsterSpawner.tick();
		this.animalSpawner.tick();

		float dayFactor = 1.0F;
		dayFactor = this.getCelestialAngle(1.0F);
		dayFactor = 1.0F - (MathHelper.cos(dayFactor * (float)Math.PI * 2.0F) * 2.0F + 0.5F);
		if(dayFactor < 0.0F) {
			dayFactor = 0.0F;
		}
		if(dayFactor > 1.0F) {
			dayFactor = 1.0F;
		}

		int newSkylight = (int)(dayFactor * 13.0F);
		if(newSkylight != this.skylightSubtracted) {
			this.skylightSubtracted = newSkylight;
			for(int i = 0; i < this.worldAccesses.size(); ++i) {
				this.worldAccesses.get(i).updateAllRenderers();
			}
		}

		++this.worldTime;
		if(this.worldTime % autosavePeriod == 0L) {
			this.saveWorld(false);
		}

		this.blockTickScheduler.updateTicks();

		this.updateBlocksAndPlayCaveSounds();
	}

	/**
	 * Called from {@link #tick()}. Iterates over the 17x17 chunk square around
	 * the player and calls {@link Block#updateTick} on random blocks within each
	 * chunk. This is how grass grows, lava spreads, farmland dries out, etc.
	 */
	public final void updateBlocksAndPlayCaveSounds() {
		int playerChunkX = MathHelper.floor_double(this.playerEntity.posX / 16.0D);
		int playerChunkZ = MathHelper.floor_double(this.playerEntity.posZ / 16.0D);

		for(int chunkXOffset = -8; chunkXOffset <= 8; ++chunkXOffset) {
			for(int chunkZOffset = -8; chunkZOffset <= 8; ++chunkZOffset) {
				int chunkX = chunkXOffset + playerChunkX;
				int chunkZ = chunkZOffset + playerChunkZ;
				if(this.chunkExists(chunkX, chunkZ)) {
					Chunk chunk = this.getChunkFromChunkCoords(chunkX, chunkZ);

					for(int tick = 0; tick < blocksToTickPerFrame; ++tick) {
						this.updateLCG = this.updateLCG * 3 + this.distHashSeed;
						int tIndex = this.updateLCG >> 2;
						int x = tIndex & 15;
						int z = tIndex >> 8 & 15;
						int y = tIndex >> 16 & 127;
						int blockID = chunk.getBlockID(x, y, z);
						if(Block.tickOnLoad[blockID]) {
							Block.blocksList[blockID].updateTick(this, x + chunk.xPosition * 16, y, z + chunk.zPosition * 16, this.rand);
						}
					}
				}
			}
		}
	}

	/**
	 * Spawns ambient particle effects (fire/smoke on furnaces, drips, etc.) for
	 * blocks in a 16x16x16 cube centred around (x, y, z). Called from the
	 * renderer's display-update loop, not the game tick.
	 */
	public final void randomDisplayUpdates(int centerX, int centerY, int centerZ) {
		Random rng = new Random();

		for(int i = 0; i < 1000; ++i) {
			int x = centerX + this.rand.nextInt(16) - this.rand.nextInt(16);
			int y = centerY + this.rand.nextInt(16) - this.rand.nextInt(16);
			int z = centerZ + this.rand.nextInt(16) - this.rand.nextInt(16);
			int blockID = this.getBlockId(x, y, z);
			if(blockID > 0) {
				Block.blocksList[blockID].randomDisplayTick(this, x, y, z, rng);
			}
		}
	}

	/**
	 * Returns all entities of the given class intersecting the given expanded box.
	 * The box is expanded by 2 units in every direction before querying chunk
	 * entity lists. {@code excludeEntity} is skipped (pass null to include it).
	 * Used for collision checks and entity-picking.
	 */
	public final List<Entity> getEntitiesWithinAABBExcludingEntity(Entity excludeEntity, AxisAlignedBB queryBox) {
		return EntityQueryService.getEntitiesWithinAABBExcludingEntity(this, excludeEntity, queryBox);
	}

	public final List<Entity> getLoadedEntityList() {
		return this.entityManager.getLoadedEntityList();
	}

	/**
	 * Marks the chunk containing (x, y, z) dirty so it will be saved to disk
	 * on the next chunk-save pass. Used by tile entities (furnaces, chests)
	 * when their state changes mid-tick.
	 */
	public final void updateTileEntityChunkAndDoNothing(int x, int y, int z) {
		if(this.blockExists(x, y, z)) {
			Chunk chunk = this.getChunkFromChunkCoords(x >> 4, z >> 4);
			chunk.isModified = true;
		}
	}

	/**
	 * Returns the cached count of live {@link EntityMonster} subclasses in the world.
	 * O(1) — the value is maintained incrementally at every entity-list mutation site.
	 */
	public final int getMonsterCount() {
		return this.entityManager.getMonsterCount();
	}

	/**
	 * Returns the cached count of live {@link EntityAnimal} subclasses in the world.
	 * O(1) — the value is maintained incrementally at every entity-list mutation site.
	 */
	public final int getAnimalCount() {
		return this.entityManager.getAnimalCount();
	}

	/**
	 * Returns a cached entity count suitable for use by {@link MobSpawner}.
	 * Dispatches to {@link #getMonsterCount} or {@link #getAnimalCount} based on
	 * the supplied class — avoids a full-list scan per spawner tick.
	 */
	public final int getCachedEntityCount(Class<? extends EntityLiving> entityClass) {
		return this.entityManager.getCachedEntityCount(entityClass);
	}

	/**
	 * Counts every entity in the world whose class is assignable from {@code entityClass}.
	 * O(n) over the entity list. Used as fallback for non-hostile/non-passive types.
	 */
	public final int countEntities(Class<? extends Entity> entityClass) {
		return this.entityManager.countEntities(entityClass);
	}

	/**
	 * Adds every entity in the list to the world in one bulk operation: adds to
	 * the entity list, updates cached mob counters, and notifies world accesses.
	 */
	public final void addLoadedEntities(List<Entity> entities) {
		this.entityManager.addLoadedEntities(entities);
	}

	/**
	 * Marks every entity in the list dead (the {@link #levelEntities()} loop
	 * removes dead entities from the entity list in O(1) per entry), updates the
	 * cached mob counters, and notifies world accesses so the renderer drops the
	 * textures immediately.
	 *
	 * <p>Note: the entity's chunk-ownership fields are cleared when the
	 * {@link #levelEntities()} cleanup pass hits it, so the entity is safely
	 * released from its chunk without further coordination.
	 */
	public final void unloadEntities(List<Entity> entities) {
		this.entityManager.unloadEntities(entities);
	}

	/** Repeatedly unloads the 100 oldest chunks until the chunk provider says nothing is left. */
	public final void dropOldChunks() {
		while(this.chunkProvider.unload100OldestChunks()) {
		}
	}

	static {
		for(int i = 0; i <= 15; ++i) {
			float t = 1.0F - (float)i / 15.0F;
			lightBrightnessTable[i] = (1.0F - t) / (t * 3.0F + 1.0F) * 0.95F + 0.05F;
		}
	}
}
