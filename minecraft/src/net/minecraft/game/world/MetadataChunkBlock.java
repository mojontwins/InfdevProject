package net.minecraft.game.world;

import net.minecraft.game.world.block.Block;

public final class MetadataChunkBlock {
	private static final int MAX_BOX_VOLUME = 32768;
	private static final int MAX_MERGE_VOLUME_GROWTH = 2;

	public final EnumSkyBlock lightType;
	public int minX;
	public int minY;
	public int minZ;
	public int maxX;
	public int maxY;
	public int maxZ;

	public MetadataChunkBlock(EnumSkyBlock lightType, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
		this.lightType = lightType;
		this.minX = minX;
		this.minY = minY;
		this.minZ = minZ;
		this.maxX = maxX;
		this.maxY = maxY;
		this.maxZ = maxZ;
	}

	public boolean tryMerge(int x1, int y1, int z1, int x2, int y2, int z2) {
		if(x1 >= this.minX && y1 >= this.minY && z1 >= this.minZ && x2 <= this.maxX && y2 <= this.maxY && z2 <= this.maxZ) {
			return true;
		}

		if(x1 < this.minX - 1 || y1 < this.minY - 1 || z1 < this.minZ - 1 || x2 > this.maxX + 1 || y2 > this.maxY + 1 || z2 > this.maxZ + 1) {
			return false;
		}

		int mergedMinX = x1 < this.minX ? x1 : this.minX;
		int mergedMinY = y1 < this.minY ? y1 : this.minY;
		int mergedMinZ = z1 < this.minZ ? z1 : this.minZ;
		int mergedMaxX = x2 > this.maxX ? x2 : this.maxX;
		int mergedMaxY = y2 > this.maxY ? y2 : this.maxY;
		int mergedMaxZ = z2 > this.maxZ ? z2 : this.maxZ;

		long currentVolume = (long)(this.maxX - this.minX) * (this.maxY - this.minY) * (this.maxZ - this.minZ);
		long mergedVolume = (long)(mergedMaxX - mergedMinX) * (mergedMaxY - mergedMinY) * (mergedMaxZ - mergedMinZ);
		if(mergedVolume - currentVolume > MAX_MERGE_VOLUME_GROWTH) {
			return false;
		}

		this.minX = mergedMinX;
		this.minY = mergedMinY;
		this.minZ = mergedMinZ;
		this.maxX = mergedMaxX;
		this.maxY = mergedMaxY;
		this.maxZ = mergedMaxZ;
		return true;
	}

	public void updateLight(World world) {
		int startY = Math.max(this.minY, 0);
		int endY = Math.min(this.maxY, 127);
		if(startY > endY) {
			return;
		}

		long volume = (long)(this.maxX - this.minX) * (this.maxY - this.minY) * (this.maxZ - this.minZ);
		if(volume > MAX_BOX_VOLUME) {
			return;
		}

		boolean isSkyLight = this.lightType == EnumSkyBlock.Sky;
		for(int x = this.minX; x <= this.maxX; ++x) {
			for(int z = this.minZ; z <= this.maxZ; ++z) {
				if(!world.blockExists(x, 0, z)) {
					continue;
				}

				for(int y = startY; y <= endY; ++y) {
					this.relightBlock(world, x, y, z, isSkyLight);
				}
			}
		}
	}

	private void relightBlock(World world, int x, int y, int z, boolean isSkyLight) {
		int currentLightValue = world.getSavedLightValue(this.lightType, x, y, z);
		int blockID = world.getBlockId(x, y, z);
		int opacity = Block.lightOpacity[blockID];
		if(opacity == 0) {
			opacity = 1;
		}

		int selfLight = 0;
		if(isSkyLight) {
			if(world.canExistingBlockSeeTheSky(x, y, z)) {
				selfLight = 15;
			}
		} else {
			Block block = Block.blocksList[blockID];
			int meta = world.getBlockMetadata(x, y, z);
			selfLight = block != null ? block.getLightValue(meta) : 0;
		}

		int newLightValue;
		if(opacity >= 15 && selfLight == 0) {
			newLightValue = 0;
		} else {
			newLightValue = world.getSavedLightValue(this.lightType, x - 1, y, z);
			int plusX = world.getSavedLightValue(this.lightType, x + 1, y, z);
			int minusY = world.getSavedLightValue(this.lightType, x, y - 1, z);
			int plusY = world.getSavedLightValue(this.lightType, x, y + 1, z);
			int minusZ = world.getSavedLightValue(this.lightType, x, y, z - 1);
			int plusZ = world.getSavedLightValue(this.lightType, x, y, z + 1);
			if(plusX > newLightValue) {
				newLightValue = plusX;
			}
			if(minusY > newLightValue) {
				newLightValue = minusY;
			}
			if(plusY > newLightValue) {
				newLightValue = plusY;
			}
			if(minusZ > newLightValue) {
				newLightValue = minusZ;
			}
			if(plusZ > newLightValue) {
				newLightValue = plusZ;
			}

			newLightValue -= opacity;
			if(newLightValue < 0) {
				newLightValue = 0;
			}
			if(selfLight > newLightValue) {
				newLightValue = selfLight;
			}
		}

		if(currentLightValue != newLightValue) {
			world.setLightValue(this.lightType, x, y, z, newLightValue);

			--newLightValue;
			if(newLightValue < 0) {
				newLightValue = 0;
			}

			world.neighborLightPropagationChanged(this.lightType, x - 1, y, z, newLightValue);
			world.neighborLightPropagationChanged(this.lightType, x, y - 1, z, newLightValue);
			world.neighborLightPropagationChanged(this.lightType, x, y, z - 1, newLightValue);
			if(x + 1 >= this.maxX) {
				world.neighborLightPropagationChanged(this.lightType, x + 1, y, z, newLightValue);
			}
			if(y + 1 >= this.maxY) {
				world.neighborLightPropagationChanged(this.lightType, x, y + 1, z, newLightValue);
			}
			if(z + 1 >= this.maxZ) {
				world.neighborLightPropagationChanged(this.lightType, x, y, z + 1, newLightValue);
			}
		}
	}
}
