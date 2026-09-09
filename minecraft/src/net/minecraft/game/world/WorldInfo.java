package net.minecraft.game.world;

import com.mojang.nbt.NBTTagCompound;

/**
 * The persistent, level.dat-backed metadata of a world, in the spirit of the classic
 * {@code WorldInfo} value object: a pure container for the fields stored in {@code level.dat} so
 * the (de)serialization and folder bookkeeping can live outside {@link World} in
 * {@link WorldStorage}. It knows nothing about the live {@code World} instance it describes —
 * {@code World} copies these fields into itself on load and rebuilds one on save.
 *
 * <p>Constructed either from the {@code "Data"} compound of an existing {@code level.dat}
 * ({@link #WorldInfo(NBTTagCompound)}) or empty with the seed of a brand-new world
 * ({@link #WorldInfo(long, WorldOptions, WorldType)}). A player is carried separately as its own
 * compound so that a freshly created world (no live player to serialize yet) can pass a null one.
 */
public final class WorldInfo {
	private long randomSeed;
	private int spawnX;
	private int spawnY;
	private int spawnZ;
	private long worldTime;
	private long sizeOnDisk;
	private WorldOptions worldOptions;
	private WorldType worldType;
	private NBTTagCompound playerTag;

	public WorldInfo(long randomSeed, WorldOptions worldOptions, WorldType worldType) {
		this.randomSeed = randomSeed;
		this.worldOptions = worldOptions;
		this.worldType = worldType;
	}

	/** Rebuilds the info from a saved level.dat "Data" compound. Callers may pass a synthesized {@code Data} (no world on disk yet). */
	public WorldInfo(NBTTagCompound dataTag) {
		this.randomSeed = dataTag.getLong("RandomSeed");
		this.spawnX = dataTag.getInteger("SpawnX");
		this.spawnY = dataTag.getInteger("SpawnY");
		this.spawnZ = dataTag.getInteger("SpawnZ");
		this.worldTime = dataTag.getLong("Time");
		this.sizeOnDisk = dataTag.getLong("SizeOnDisk");
		this.worldOptions = new WorldOptions();
		this.worldOptions.readFromNBT(dataTag);
		WorldType savedType = WorldType.fromId(dataTag.getString("WorldType"));
		this.worldType = savedType != null ? savedType : WorldType.WORLDTYPE_420;
		NBTTagCompound savedPlayer = dataTag.getCompoundTag("Player");
		this.playerTag = savedPlayer != null ? savedPlayer : null;
	}

	/** Serializes this info (optionally with a live player) into the caller's "Data" compound. */
	public void updateTagCompound(NBTTagCompound dataTag, NBTTagCompound playerTag) {
		dataTag.setLong("RandomSeed", this.randomSeed);
		dataTag.setInteger("SpawnX", this.spawnX);
		dataTag.setInteger("SpawnY", this.spawnY);
		dataTag.setInteger("SpawnZ", this.spawnZ);
		dataTag.setLong("Time", this.worldTime);
		dataTag.setLong("SizeOnDisk", this.sizeOnDisk);
		dataTag.setLong("LastPlayed", System.currentTimeMillis());
		dataTag.setString("WorldType", this.worldType.getId());
		this.worldOptions.writeToNBT(dataTag);
		if(playerTag != null) {
			dataTag.setCompoundTag("Player", playerTag);
		}
	}

	public long getRandomSeed() {
		return this.randomSeed;
	}

	public void setRandomSeed(long randomSeed) {
		this.randomSeed = randomSeed;
	}

	public int getSpawnX() {
		return this.spawnX;
	}

	public int getSpawnY() {
		return this.spawnY;
	}

	public int getSpawnZ() {
		return this.spawnZ;
	}

	public void setSpawn(int x, int y, int z) {
		this.spawnX = x;
		this.spawnY = y;
		this.spawnZ = z;
	}

	public long getWorldTime() {
		return this.worldTime;
	}

	public void setWorldTime(long worldTime) {
		this.worldTime = worldTime;
	}

	public long getSizeOnDisk() {
		return this.sizeOnDisk;
	}

	public void setSizeOnDisk(long sizeOnDisk) {
		this.sizeOnDisk = sizeOnDisk;
	}

	public WorldOptions getWorldOptions() {
		return this.worldOptions;
	}

	public WorldType getWorldType() {
		return this.worldType;
	}

	public void setWorldType(WorldType worldType) {
		this.worldType = worldType;
	}

	public NBTTagCompound getPlayerTag() {
		return this.playerTag;
	}

	public void setPlayerTag(NBTTagCompound playerTag) {
		this.playerTag = playerTag;
	}
}
