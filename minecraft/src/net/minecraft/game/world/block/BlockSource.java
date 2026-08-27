package net.minecraft.game.world.block;

import java.util.Random;
import net.minecraft.game.world.World;
import net.minecraft.game.world.material.Material;

public final class BlockSource extends Block {
	private int fluid;

	protected BlockSource(int blockID, int fluidID) {
		super(blockID, Block.blocksList[fluidID].blockIndexInTexture, Material.water);
		this.fluid = fluidID;
		this.setTickOnLoad(true);
	}

	@Override
	public final void onBlockAdded(World world, int x, int y, int z) {
		this.placeFluidIfEmpty(world, x - 1, y, z);
		this.placeFluidIfEmpty(world, x + 1, y, z);
		this.placeFluidIfEmpty(world, x, y, z - 1);
		this.placeFluidIfEmpty(world, x, y, z + 1);
	}

	@Override
	public final void updateTick(World world, int x, int y, int z, Random random) {
		this.placeFluidIfEmpty(world, x - 1, y, z);
		this.placeFluidIfEmpty(world, x + 1, y, z);
		this.placeFluidIfEmpty(world, x, y, z - 1);
		this.placeFluidIfEmpty(world, x, y, z + 1);
	}

	private void placeFluidIfEmpty(World world, int x, int y, int z) {
		if(world.getBlockId(x, y, z) == 0) {
			world.setBlockWithNotify(x, y, z, this.fluid);
		}
	}
}