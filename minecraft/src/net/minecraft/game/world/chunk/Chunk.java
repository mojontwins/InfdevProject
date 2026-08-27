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

	private Chunk(World var1, int var2, int var3) {
		this.chunkTileEntityMap = new HashMap<>();
		this.isTerrainPopulated = false;
		this.isModified = false;
		this.hasEntities = false;
		this.worldObj = var1;
		this.xPosition = var2;
		this.zPosition = var3;
		this.heightMap = new byte[256];

		for(int var4 = 0; var4 < this.entities.length; ++var4) {
			this.entities[var4] = new ArrayList<>();
		}

	}

	public Chunk(World var1, byte[] var2, int var3, int var4) {
		this(var1, var3, var4);
		this.blocks = var2;
		this.data = new NibbleArray(var2.length);
		this.skyLightMap = new NibbleArray(var2.length);
		this.blockLightMap = new NibbleArray(var2.length);
	}

	public final int getHeightValue(int var1, int var2) {
		return this.heightMap[var2 << 4 | var1] & 255;
	}

	public final void generateHeightMap() {
		int var1 = 127;

		int var2;
		int var3;
		for(var2 = 0; var2 < 16; ++var2) {
			for(var3 = 0; var3 < 16; ++var3) {
				this.heightMap[var3 << 4 | var2] = -128;
				this.relightBlock(var2, 127, var3);
				if((this.heightMap[var3 << 4 | var2] & 255) < var1) {
					var1 = this.heightMap[var3 << 4 | var2] & 255;
				}
			}
		}

		this.lowestBlockHeight = var1;

		for(var2 = 0; var2 < 16; ++var2) {
			for(var3 = 0; var3 < 16; ++var3) {
				this.updateSkylight_do(var2, var3);
			}
		}

		this.isModified = true;
	}

	private void updateSkylight_do(int var1, int var2) {
		int var3 = this.getHeightValue(var1, var2);
		var1 += this.xPosition << 4;
		var2 += this.zPosition << 4;
		this.checkSkylightNeighborHeight(var1 - 1, var2, var3);
		this.checkSkylightNeighborHeight(var1 + 1, var2, var3);
		this.checkSkylightNeighborHeight(var1, var2 - 1, var3);
		this.checkSkylightNeighborHeight(var1, var2 + 1, var3);
	}

	private void checkSkylightNeighborHeight(int var1, int var2, int var3) {
		int var4 = this.worldObj.getHeightValue(var1, var2);
		if(var4 > var3) {
			this.worldObj.scheduleLightingUpdate(EnumSkyBlock.Sky, var1, var3, var2, var1, var4, var2);
		} else if(var4 < var3) {
			this.worldObj.scheduleLightingUpdate(EnumSkyBlock.Sky, var1, var4, var2, var1, var3, var2);
		}

		this.isModified = true;
	}

	private void relightBlock(int var1, int var2, int var3) {
		int var4 = this.heightMap[var3 << 4 | var1] & 255;
		int var5 = var4;
		if(var2 > var4) {
			var5 = var2;
		}

		while(var5 > 0 && Block.lightOpacity[this.getBlockID(var1, var5 - 1, var3)] == 0) {
			--var5;
		}

		if(var5 != var4) {
			this.worldObj.markBlocksDirtyVertical(var1, var3, var5, var4);
			this.heightMap[var3 << 4 | var1] = (byte)var5;
			int var6;
			int var7;
			if(var5 < this.lowestBlockHeight) {
				this.lowestBlockHeight = var5;
			} else {
				var2 = 127;

				for(var6 = 0; var6 < 16; ++var6) {
					for(var7 = 0; var7 < 16; ++var7) {
						if((this.heightMap[var7 << 4 | var6] & 255) < var2) {
							var2 = this.heightMap[var7 << 4 | var6] & 255;
						}
					}
				}

				this.lowestBlockHeight = var2;
			}

			var2 = (this.xPosition << 4) + var1;
			var6 = (this.zPosition << 4) + var3;
			if(var5 < var4) {
				for(var7 = var5; var7 < var4; ++var7) {
					this.skyLightMap.set(var1, var7, var3, 15);
				}
			} else {
				this.worldObj.scheduleLightingUpdate(EnumSkyBlock.Sky, var2, var4, var6, var2, var5, var6);

				for(var7 = var4; var7 < var5; ++var7) {
					this.skyLightMap.set(var1, var7, var3, 0);
				}
			}

			var7 = 15;

			for(var4 = var5; var5 > 0 && var7 > 0; this.skyLightMap.set(var1, var5, var3, var7)) {
				--var5;
				int var8 = Block.lightOpacity[this.getBlockID(var1, var5, var3)];
				if(var8 == 0) {
					var8 = 1;
				}

				var7 -= var8;
				if(var7 < 0) {
					var7 = 0;
				}
			}

			while(var5 > 0 && Block.lightOpacity[this.getBlockID(var1, var5 - 1, var3)] == 0) {
				--var5;
			}

			if(var5 != var4) {
				this.worldObj.scheduleLightingUpdate(EnumSkyBlock.Sky, var2 - 1, var5, var6 - 1, var2 + 1, var4, var6 + 1);
			}

			this.isModified = true;
		}
	}

	public final int getBlockID(int var1, int var2, int var3) {
		return this.blocks[var1 << 11 | var3 << 7 | var2];
	}

	public final boolean setBlockID(int var1, int var2, int var3, int var4) {
		byte var5 = (byte)var4;
		int var6 = this.heightMap[var3 << 4 | var1] & 255;
		int var7 = this.blocks[var1 << 11 | var3 << 7 | var2] & 255;
		if(var7 == var4) {
			return false;
		} else {
			int var8 = (this.xPosition << 4) + var1;
			int var9 = (this.zPosition << 4) + var3;
			if(var7 != 0) {
				Block.blocksList[var7].onBlockRemoval(this.worldObj, var8, var2, var9);
			}

			this.blocks[var1 << 11 | var3 << 7 | var2] = var5;
			this.data.set(var1, var2, var3, 0);
			if(Block.lightOpacity[var5] != 0) {
				if(var2 >= var6) {
					this.relightBlock(var1, var2 + 1, var3);
				}
			} else if(var2 == var6 - 1) {
				this.relightBlock(var1, var2, var3);
			}

			this.worldObj.scheduleLightingUpdate(EnumSkyBlock.Sky, var8, var2, var9, var8, var2, var9);
			this.worldObj.scheduleLightingUpdate(EnumSkyBlock.Block, var8, var2, var9, var8, var2, var9);
			this.updateSkylight_do(var1, var3);
			if(var4 != 0) {
				Block.blocksList[var4].onBlockAdded(this.worldObj, var8, var2, var9);
			}

			this.isModified = true;
			return true;
		}
	}

	public final int getBlockMetadata(int var1, int var2, int var3) {
		return this.data.get(var1, var2, var3);
	}

	public final void setBlockMetadata(int var1, int var2, int var3, int var4) {
		this.isModified = true;
		this.data.set(var1, var2, var3, var4);
	}

	public final int getSavedLightValue(EnumSkyBlock var1, int var2, int var3, int var4) {
		return var1 == EnumSkyBlock.Sky ? this.skyLightMap.get(var2, var3, var4) : (var1 == EnumSkyBlock.Block ? this.blockLightMap.get(var2, var3, var4) : 0);
	}

	public final void setLightValue(EnumSkyBlock var1, int var2, int var3, int var4, int var5) {
		this.isModified = true;
		if(var1 == EnumSkyBlock.Sky) {
			this.skyLightMap.set(var2, var3, var4, var5);
		} else if(var1 == EnumSkyBlock.Block) {
			this.blockLightMap.set(var2, var3, var4, var5);
		}
	}

	public final int getBlockLightValue(int var1, int var2, int var3, int var4) {
		int var5 = this.skyLightMap.get(var1, var2, var3);
		if(var5 > 0) {
			isLit = true;
		}

		var5 -= var4;
		var1 = this.blockLightMap.get(var1, var2, var3);
		if(var1 > var5) {
			var5 = var1;
		}

		return var5;
	}

	public final void writeChunkNBTData(NBTTagCompound var1) {
		var1.setInteger("xPos", this.xPosition);
		var1.setInteger("zPos", this.zPosition);
		var1.setLong("LastUpdate", this.worldObj.worldTime);
		var1.setByteArray("Blocks", this.blocks);
		var1.setByteArray("Data", this.data.data);
		var1.setByteArray("SkyLight", this.skyLightMap.data);
		var1.setByteArray("BlockLight", this.blockLightMap.data);
		var1.setByteArray("HeightMap", this.heightMap);
		var1.setBoolean("TerrainPopulated", this.isTerrainPopulated);
		this.hasEntities = false;
		NBTTagList var2 = new NBTTagList();

		for(int var3 = 0; var3 < this.entities.length; ++var3) {
			for(Entity var5 : this.entities[var3]) {
				NBTTagCompound var6 = new NBTTagCompound();
				if(var5.addEntityID(var6)) {
					var2.setTag(var6);
					this.hasEntities = true;
				}
			}
		}

		var1.setTag("Entities", var2);
		NBTTagList var7 = new NBTTagList();
		for(TileEntity var8 : this.chunkTileEntityMap.values()) {
			NBTTagCompound var6 = new NBTTagCompound();
			var8.writeToNBT(var6);
			var7.setTag(var6);
		}

		var1.setTag("TileEntities", var7);
	}

	public static Chunk readChunkNBTData(World var0, NBTTagCompound var1) {
		int var2 = var1.getInteger("xPos");
		int var3 = var1.getInteger("zPos");
		Chunk var9 = new Chunk(var0, var2, var3);
		var9.blocks = var1.getByteArray("Blocks");
		var9.data = new NibbleArray(var1.getByteArray("Data"));
		var9.skyLightMap = new NibbleArray(var1.getByteArray("SkyLight"));
		var9.blockLightMap = new NibbleArray(var1.getByteArray("BlockLight"));
		var9.heightMap = var1.getByteArray("HeightMap");
		var9.isTerrainPopulated = var1.getBoolean("TerrainPopulated");
		if(!var9.data.isValid()) {
			var9.data = new NibbleArray(var9.blocks.length);
		}

		if(var9.heightMap == null || !var9.skyLightMap.isValid()) {
			var9.heightMap = new byte[256];
			var9.skyLightMap = new NibbleArray(var9.blocks.length);
			var9.generateHeightMap();
		}

		if(!var9.blockLightMap.isValid()) {
			var9.blockLightMap = new NibbleArray(var9.blocks.length);
		}

		var9.hasEntities = false;
		NBTTagList var10 = var1.getTagList("Entities");
		if(var10 != null) {
			for(int var4 = 0; var4 < var10.tagCount(); ++var4) {
				NBTTagCompound var5 = (NBTTagCompound)var10.tagAt(var4);
				Entity var6 = EntityList.createEntityFromNBT(var5, var0);
				if(var6 != null) {
					var9.hasEntities = true;
					var9.addEntity(var6);
				}
			}
		}

		NBTTagList var11 = var1.getTagList("TileEntities");
		if(var11 != null) {
			for(int var12 = 0; var12 < var11.tagCount(); ++var12) {
				NBTTagCompound var13 = (NBTTagCompound)var11.tagAt(var12);
				TileEntity var8 = TileEntity.createAndLoadEntity(var13);
				if(var8 != null) {
					var3 = var8.xCoord - (var9.xPosition << 4);
					int var14 = var8.yCoord;
					int var7 = var8.zCoord - (var9.zPosition << 4);
					var9.setChunkBlockTileEntity(var3, var14, var7, var8);
				}
			}
		}

		return var9;
	}

	public final void addEntity(Entity var1) {
		int var2 = MathHelper.floor_double(var1.posX / 16.0D);
		int var3 = MathHelper.floor_double(var1.posZ / 16.0D);
		if(var2 != this.xPosition || var3 != this.zPosition) {
			System.out.println("Wrong location! " + var1);
		}

		var2 = MathHelper.floor_double(var1.posY / 16.0D);
		if(var2 < 0) {
			var2 = 0;
		}

		if(var2 >= this.entities.length) {
			var2 = this.entities.length - 1;
		}

		this.entities[var2].add(var1);
		this.isModified = true;
	}

	public final void removeEntityAtIndex(Entity var1, int var2) {
		if(var2 < 0) {
			var2 = 0;
		}

		if(var2 >= this.entities.length) {
			var2 = this.entities.length - 1;
		}

		if(!this.entities[var2].contains(var1)) {
			System.out.println("There\'s no such entity to remove: " + var1);
		}

		this.entities[var2].remove(var1);
		this.isModified = true;
	}

	public final boolean canBlockSeeTheSky(int var1, int var2, int var3) {
		return var2 >= (this.heightMap[var3 << 4 | var1] & 255);
	}

	public final TileEntity getChunkBlockTileEntity(int var1, int var2, int var3) {
		int var4 = var1 + (var2 << 10) + (var3 << 10 << 10);
		TileEntity var5 = this.chunkTileEntityMap.get(Integer.valueOf(var4));
		if(var5 == null) {
			int var6 = this.getBlockID(var1, var2, var3);
			BlockContainer var7 = (BlockContainer)Block.blocksList[var6];
			var7.onBlockAdded(this.worldObj, (this.xPosition << 4) + var1, var2, (this.zPosition << 4) + var3);
			var5 = this.chunkTileEntityMap.get(Integer.valueOf(var4));
		}

		return var5;
	}

	public final void setChunkBlockTileEntity(int var1, int var2, int var3, TileEntity var4) {
		this.isModified = true;
		int var5 = var1 + (var2 << 10) + (var3 << 10 << 10);
		var4.worldObj = this.worldObj;
		var4.xCoord = (this.xPosition << 4) + var1;
		var4.yCoord = var2;
		var4.zCoord = (this.zPosition << 4) + var3;
		if(this.getBlockID(var1, var2, var3) != 0 && Block.blocksList[this.getBlockID(var1, var2, var3)] instanceof BlockContainer) {
			this.chunkTileEntityMap.put(Integer.valueOf(var5), var4);
			this.worldObj.loadedTileEntityList.add(var4);
		} else {
			System.out.println("Attempted to place a tile entity where there was no entity tile!");
		}
	}

	public final void removeChunkBlockTileEntity(int var1, int var2, int var3) {
		this.isModified = true;
		var1 = var1 + (var2 << 10) + (var3 << 10 << 10);
		this.worldObj.loadedTileEntityList.remove(this.chunkTileEntityMap.remove(Integer.valueOf(var1)));
	}

	public final void loadEntities() {
		this.worldObj.loadedTileEntityList.addAll(this.chunkTileEntityMap.values());

		for(int var1 = 0; var1 < this.entities.length; ++var1) {
			this.worldObj.addLoadedEntities(this.entities[var1]);
		}

	}

	public final void unloadEntities() {
		this.worldObj.loadedTileEntityList.removeAll(this.chunkTileEntityMap.values());

		for(int var1 = 0; var1 < this.entities.length; ++var1) {
			this.worldObj.unloadEntities(this.entities[var1]);
		}

	}

	public final void getEntitiesWithinAABBForEntity(Entity var1, AxisAlignedBB var2, List<Entity> var3) {
		int var4 = MathHelper.floor_double((var2.minY - 2.0D) / 16.0D);
		int var5 = MathHelper.floor_double((var2.maxY + 2.0D) / 16.0D);
		if(var4 < 0) {
			var4 = 0;
		}

		if(var5 >= this.entities.length) {
			var5 = this.entities.length - 1;
		}

		for(; var4 <= var5; ++var4) {
			List<Entity> var6 = this.entities[var4];

			for(int var7 = 0; var7 < var6.size(); ++var7) {
				Entity var8 = var6.get(var7);
				if(var8 != var1 && var8.boundingBox.intersectsWith(var2)) {
					var3.add(var8);
				}
			}
		}

	}

	public final boolean needsSaving(boolean var1) {
		if(this.isModified) {
			return true;
		} else {
			if(var1) {
				if(this.hasEntities) {
					return true;
				}

				for(int var2 = 0; var2 < this.entities.length; ++var2) {
					if(this.entities[var2].size() > 0) {
						return true;
					}
				}
			}

			return false;
		}
	}
}
