package net.minecraft.game.world;

import net.minecraft.game.world.block.Block;
import net.minecraft.game.world.block.tileentity.TileEntity;
import net.minecraft.game.world.chunk.Chunk;
import net.minecraft.game.world.material.Material;

/**
 * A bounded, read-only window into the world used while compiling a chunk's
 * geometry. The constructor snapshots the {@link Chunk} references of one
 * region plus a one-block margin, so every block lookup made during
 * tessellation (including neighbor checks for face culling and lighting)
 * resolves through a pre-fetched array index instead of re-resolving the world
 * chunk per call.
 */
public class ChunkCache implements IBlockAccess {
	private int chunkX;
	private int chunkZ;
	private Chunk[][] chunkArray;
	private World worldObj;

	public ChunkCache(World world, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
		this.worldObj = world;
		this.chunkX = minX >> 4;
		this.chunkZ = minZ >> 4;
		int chunkMaxX = maxX >> 4;
		int chunkMaxZ = maxZ >> 4;
		this.chunkArray = new Chunk[chunkMaxX - this.chunkX + 1][chunkMaxZ - this.chunkZ + 1];

		for(int x = this.chunkX; x <= chunkMaxX; ++x) {
			for(int z = this.chunkZ; z <= chunkMaxZ; ++z) {
				this.chunkArray[x - this.chunkX][z - this.chunkZ] = world.getChunkFromChunkCoords(x, z);
			}
		}

	}

	@Override
	public int getBlockId(int x, int y, int z) {
		// Mirrors World#getBlockId: the bottom of the world is a solid lava
		// ocean, the top is void.
		if(y <= 0) {
			return Block.lavaStill.blockID;
		} else if(y >= 128) {
			return 0;
		} else {
			int chunkOffsetX = (x >> 4) - this.chunkX;
			int chunkOffsetZ = (z >> 4) - this.chunkZ;
			return this.chunkArray[chunkOffsetX][chunkOffsetZ].getBlockID(x & 15, y, z & 15);
		}
	}

	@Override
	public TileEntity getBlockTileEntity(int x, int y, int z) {
		int chunkOffsetX = (x >> 4) - this.chunkX;
		int chunkOffsetZ = (z >> 4) - this.chunkZ;
		return this.chunkArray[chunkOffsetX][chunkOffsetZ].getChunkBlockTileEntity(x & 15, y, z & 15);
	}

	@Override
	public float getBrightness(int x, int y, int z) {
		return World.lightBrightnessTable[this.getLightValue(x, y, z)];
	}

	public int getLightValue(int x, int y, int z) {
		return this.getLightValueExt(x, y, z, true);
	}

	public int getLightValueExt(int x, int y, int z, boolean isCenterBlock) {
		int lightValue;

		if (isCenterBlock) {
			// Slabs and farmland take their light from the brightest of the
			// five neighbouring cells instead of their own (too dark) cell.
			lightValue = this.getBlockId(x, y, z);
			if (lightValue == Block.stairSingle.blockID || lightValue == Block.tilledField.blockID) {
				int above = this.getLightValueExt(x, y + 1, z, false);
				int east = this.getLightValueExt(x + 1, y, z, false);
				int west = this.getLightValueExt(x - 1, y, z, false);
				int south = this.getLightValueExt(x, y, z + 1, false);
				int north = this.getLightValueExt(x, y, z - 1, false);
				if (east > above) {
					above = east;
				}

				if (west > above) {
					above = west;
				}

				if (south > above) {
					above = south;
				}

				if (north > above) {
					above = north;
				}

				return above;
			}
		}

		if (y < 0) {
			return 0;
		} else if (y >= 128) {
			lightValue = 15 - this.worldObj.skylightSubtracted;
			if (lightValue < 0) {
				lightValue = 0;
			}

			return lightValue;
		} else {
			int chunkOffsetX = (x >> 4) - this.chunkX;
			int chunkOffsetZ = (z >> 4) - this.chunkZ;
			return this.chunkArray[chunkOffsetX][chunkOffsetZ].getBlockLightValue(x & 15, y, z & 15,
					this.worldObj.skylightSubtracted);
		}

	}

	@Override
	public int getBlockMetadata(int x, int y, int z) {
		if(y < 0) {
			return 0;
		} else if(y >= 128) {
			return 0;
		} else {
			int chunkOffsetX = (x >> 4) - this.chunkX;
			int chunkOffsetZ = (z >> 4) - this.chunkZ;
			return this.chunkArray[chunkOffsetX][chunkOffsetZ].getBlockMetadata(x & 15, y, z & 15);
		}
	}

	@Override
	public Material getBlockMaterial(int x, int y, int z) {
		int blockId = this.getBlockId(x, y, z);
		return blockId == 0 ? Material.air : Block.blocksList[blockId].blockMaterial;
	}

	@Override
	public boolean isSolid(int x, int y, int z) {
		Block block = Block.blocksList[this.getBlockId(x, y, z)];
		return block == null ? false : block.isOpaqueCube();
	}
}