package net.minecraft.game.world.block;

import net.minecraft.game.world.World;
import net.minecraft.game.world.block.tileentity.TileEntity;
import net.minecraft.game.world.material.Material;

public abstract class BlockContainer extends Block {
	protected BlockContainer(int blockID, Material material) {
		super(blockID, material);
	}

	@Override
	public void onBlockAdded(World world, int x, int y, int z) {
		super.onBlockAdded(world, x, y, z);
		world.setBlockTileEntity(x, y, z, this.getBlockEntity());
	}

	@Override
	public void onBlockRemoval(World world, int x, int y, int z) {
		super.onBlockRemoval(world, x, y, z);
		world.removeBlockTileEntity(x, y, z);
	}

	protected abstract TileEntity getBlockEntity();
}