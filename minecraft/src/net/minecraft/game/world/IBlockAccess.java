package net.minecraft.game.world;

import net.minecraft.game.world.block.tileentity.TileEntity;
import net.minecraft.game.world.material.Material;

/**
 * The read-only view of the world that the block renderer queries. Both the
 * full {@link World} and the region-bounded {@link ChunkCache} satisfy it, so
 * tessellation code stays agnostic of where the block data actually lives.
 */
public interface IBlockAccess {
	int getBlockId(int x, int y, int z);

	TileEntity getBlockTileEntity(int x, int y, int z);

	float getBrightness(int x, int y, int z);

	int getBlockMetadata(int x, int y, int z);

	Material getBlockMaterial(int x, int y, int z);

	boolean isSolid(int x, int y, int z);
}