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

public final class Chunk {
	public static boolean isLit;
	private byte[] blocks;
	private World worldObj;
	private NibbleArray data;
	private NibbleArray skyLightMap;
	private NibbleArray blockLightMap;
	private byte[] heightMap;
	private int lowestBlockHeight;
	public final int xPosition;
	public final int zPosition;
	private Map<Integer, TileEntity> chunkTileEntityMap;
	@SuppressWarnings("unchecked")
	private List<Entity>[] entities = (List<Entity>[])new List<?>[8];
	public boolean isTerrainPopulated;
	public boolean isModified;
	private boolean hasEntities;

	private Chunk(World world, int chunkX, int chunkZ) {
		this.chunkTileEntityMap = new HashMap<>();
		this.worldObj = world;
		this.xPosition = chunkX;
		this.zPosition = chunkZ;
		this.heightMap = new byte[256];

		for(int i = 0; i < this.entities.length; ++i) {
			this.entities[i] = new ArrayList<>();
		}

	}

	public Chunk(World world, byte[] blockArray, int chunkX, int chunkZ) {
		this(world, chunkX, chunkZ);
		this.blocks = blockArray;
		this.data = new NibbleArray(blockArray.length);
		this.skyLightMap = new NibbleArray(blockArray.length);
		this.blockLightMap = new NibbleArray(blockArray.length);
	}

	public final int getHeightValue(int x, int z) {
		return this.heightMap[z << 4 | x] & 255;
	}

	public final void generateHeightMap() {
		int lowestHeight = 127;

		for(int x = 0; x < 16; ++x) {
			for(int z = 0; z < 16; ++z) {
				this.heightMap[z << 4 | x] = -128;
				this.relightBlock(x, 127, z);
				if((this.heightMap[z << 4 | x] & 255) < lowestHeight) {
					lowestHeight = this.heightMap[z << 4 | x] & 255;
				}
			}
		}

		this.lowestBlockHeight = lowestHeight;

		for(int x = 0; x < 16; ++x) {
			for(int z = 0; z < 16; ++z) {
				this.updateSkylight_do(x, z);
			}
		}

		this.isModified = true;
	}

	private void updateSkylight_do(int x, int z) {
		int height = this.getHeightValue(x, z);
		int worldX = x + (this.xPosition << 4);
		int worldZ = z + (this.zPosition << 4);
		this.checkSkylightNeighborHeight(worldX - 1, worldZ, height);
		this.checkSkylightNeighborHeight(worldX + 1, worldZ, height);
		this.checkSkylightNeighborHeight(worldX, worldZ - 1, height);
		this.checkSkylightNeighborHeight(worldX, worldZ + 1, height);
	}

	private void checkSkylightNeighborHeight(int worldX, int worldZ, int height) {
		int neighborHeight = this.worldObj.getHeightValue(worldX, worldZ);
		if(neighborHeight > height) {
			this.worldObj.scheduleLightingUpdate(EnumSkyBlock.Sky, worldX, height, worldZ, worldX, neighborHeight, worldZ);
		} else if(neighborHeight < height) {
			this.worldObj.scheduleLightingUpdate(EnumSkyBlock.Sky, worldX, neighborHeight, worldZ, worldX, height, worldZ);
		}

		this.isModified = true;
	}

	private void relightBlock(int x, int y, int z) {
		int oldHeight = this.heightMap[z << 4 | x] & 255;
		int newHeight = oldHeight;
		if(y > newHeight) {
			newHeight = y;
		}

		while(newHeight > 0 && Block.lightOpacity[this.getBlockID(x, newHeight - 1, z)] == 0) {
			--newHeight;
		}

		if(newHeight != oldHeight) {
			this.worldObj.markBlocksDirtyVertical(x, z, newHeight, oldHeight);
			this.heightMap[z << 4 | x] = (byte)newHeight;
			if(newHeight < this.lowestBlockHeight) {
				this.lowestBlockHeight = newHeight;
			} else {
				int lowestHeight = 127;
				for(int scanZ = 0; scanZ < 16; ++scanZ) {
					for(int scanX = 0; scanX < 16; ++scanX) {
						if((this.heightMap[scanZ << 4 | scanX] & 255) < lowestHeight) {
							lowestHeight = this.heightMap[scanZ << 4 | scanX] & 255;
						}
					}
				}

				this.lowestBlockHeight = lowestHeight;
			}

			int worldX = (this.xPosition << 4) + x;
			int worldZ = (this.zPosition << 4) + z;
			if(newHeight < oldHeight) {
				for(int skyY = newHeight; skyY < oldHeight; ++skyY) {
					this.skyLightMap.set(x, skyY, z, 15);
				}
			} else {
				this.worldObj.scheduleLightingUpdate(EnumSkyBlock.Sky, worldX, oldHeight, worldZ, worldX, newHeight, worldZ);

				for(int skyY = oldHeight; skyY < newHeight; ++skyY) {
					this.skyLightMap.set(x, skyY, z, 0);
				}
			}

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

			while(newHeight > 0 && Block.lightOpacity[this.getBlockID(x, newHeight - 1, z)] == 0) {
				--newHeight;
			}

			if(newHeight != referenceHeight) {
				this.worldObj.scheduleLightingUpdate(EnumSkyBlock.Sky, worldX - 1, newHeight, worldZ - 1, worldX + 1, referenceHeight, worldZ + 1);
			}

			this.isModified = true;
		}
	}

	public final int getBlockID(int x, int y, int z) {
		return this.blocks[x << 11 | z << 7 | y];
	}

	public final boolean setBlockID(int x, int y, int z, int blockID) {
		byte blockByte = (byte)blockID;
		int height = this.heightMap[z << 4 | x] & 255;
		int currentBlockID = this.blocks[x << 11 | z << 7 | y] & 255;
		if(currentBlockID == blockID) {
			return false;
		}

		int worldX = (this.xPosition << 4) + x;
		int worldZ = (this.zPosition << 4) + z;
		if(currentBlockID != 0) {
			Block.blocksList[currentBlockID].onBlockRemoval(this.worldObj, worldX, y, worldZ);
		}

		this.blocks[x << 11 | z << 7 | y] = blockByte;
		this.data.set(x, y, z, 0);
		if(Block.lightOpacity[blockByte] != 0) {
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

	public final void setBlockMetadata(int x, int y, int z, int metadata) {
		this.isModified = true;
		this.data.set(x, y, z, metadata);
	}

	public final int getSavedLightValue(EnumSkyBlock lightType, int x, int y, int z) {
		return lightType == EnumSkyBlock.Sky ? this.skyLightMap.get(x, y, z) : (lightType == EnumSkyBlock.Block ? this.blockLightMap.get(x, y, z) : 0);
	}

	public final void setLightValue(EnumSkyBlock lightType, int x, int y, int z, int value) {
		this.isModified = true;
		if(lightType == EnumSkyBlock.Sky) {
			this.skyLightMap.set(x, y, z, value);
		} else if(lightType == EnumSkyBlock.Block) {
			this.blockLightMap.set(x, y, z, value);
		}
	}

	public final int getBlockLightValue(int x, int y, int z, int lightSubtracted) {
		int skyLight = this.skyLightMap.get(x, y, z);
		if(skyLight > 0) {
			isLit = true;
		}

		skyLight -= lightSubtracted;
		int blockLight = this.blockLightMap.get(x, y, z);
		if(blockLight > skyLight) {
			skyLight = blockLight;
		}

		return skyLight;
	}

	public final void writeChunkNBTData(NBTTagCompound nbtTag) {
		nbtTag.setInteger("xPos", this.xPosition);
		nbtTag.setInteger("zPos", this.zPosition);
		nbtTag.setLong("LastUpdate", this.worldObj.worldTime);
		nbtTag.setByteArray("Blocks", this.blocks);
		nbtTag.setByteArray("Data", this.data.data);
		nbtTag.setByteArray("SkyLight", this.skyLightMap.data);
		nbtTag.setByteArray("BlockLight", this.blockLightMap.data);
		nbtTag.setByteArray("HeightMap", this.heightMap);
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
			chunk.heightMap = new byte[256];
			chunk.skyLightMap = new NibbleArray(chunk.blocks.length);
			chunk.generateHeightMap();
		}

		if(!chunk.blockLightMap.isValid()) {
			chunk.blockLightMap = new NibbleArray(chunk.blocks.length);
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
					chunk.setChunkBlockTileEntity(tileEntity.xCoord - (chunkX << 4), tileEntity.yCoord, tileEntity.zCoord - (chunkZ << 4), tileEntity);
				}
			}
		}

		return chunk;
	}

	public final void addEntity(Entity entity) {
		int entityChunkX = MathHelper.floor_double(entity.posX / 16.0D);
		int entityChunkZ = MathHelper.floor_double(entity.posZ / 16.0D);
		if(entityChunkX != this.xPosition || entityChunkZ != this.zPosition) {
			System.out.println("Wrong location! " + entity);
		}

		int segmentIndex = MathHelper.floor_double(entity.posY / 16.0D);
		if(segmentIndex < 0) {
			segmentIndex = 0;
		}

		if(segmentIndex >= this.entities.length) {
			segmentIndex = this.entities.length - 1;
		}

		this.entities[segmentIndex].add(entity);
		this.isModified = true;
	}

	public final void removeEntityAtIndex(Entity entity, int segmentIndex) {
		if(segmentIndex < 0) {
			segmentIndex = 0;
		}

		if(segmentIndex >= this.entities.length) {
			segmentIndex = this.entities.length - 1;
		}

		List<Entity> entitySegment = this.entities[segmentIndex];
		if(!entitySegment.contains(entity)) {
			System.out.println("There\'s no such entity to remove: " + entity);
		}

		entitySegment.remove(entity);
		this.isModified = true;
	}

	public final boolean canBlockSeeTheSky(int x, int y, int z) {
		return y >= (this.heightMap[z << 4 | x] & 255);
	}

	public final TileEntity getChunkBlockTileEntity(int x, int y, int z) {
		int key = x + (y << 10) + (z << 10 << 10);
		TileEntity tileEntity = this.chunkTileEntityMap.get(Integer.valueOf(key));
		if(tileEntity == null) {
			int blockID = this.getBlockID(x, y, z);
			BlockContainer blockContainer = (BlockContainer)Block.blocksList[blockID];
			blockContainer.onBlockAdded(this.worldObj, (this.xPosition << 4) + x, y, (this.zPosition << 4) + z);
			tileEntity = this.chunkTileEntityMap.get(Integer.valueOf(key));
		}

		return tileEntity;
	}

	public final void setChunkBlockTileEntity(int x, int y, int z, TileEntity tileEntity) {
		this.isModified = true;
		int key = x + (y << 10) + (z << 10 << 10);
		tileEntity.worldObj = this.worldObj;
		tileEntity.xCoord = (this.xPosition << 4) + x;
		tileEntity.yCoord = y;
		tileEntity.zCoord = (this.zPosition << 4) + z;
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
		int key = x + (y << 10) + (z << 10 << 10);
		TileEntity removedTileEntity = this.chunkTileEntityMap.remove(Integer.valueOf(key));
		if(removedTileEntity != null) {
			this.worldObj.loadedTileEntityList.remove(removedTileEntity);
		}
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