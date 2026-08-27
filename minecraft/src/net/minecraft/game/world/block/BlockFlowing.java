package net.minecraft.game.world.block;

import java.util.Random;
import net.minecraft.game.world.World;
import net.minecraft.game.world.material.Material;

public final class BlockFlowing extends BlockFluid {
	protected BlockFlowing(int blockID, Material material) {
		super(blockID, material);
	}

	@Override
	public final void updateTick(World world, int x, int y, int z, Random random) {
	}

	@Override
	public final boolean update(World world, int x, int y, int z, int level) {
		return false;
	}

	@Override
	public final void onNeighborBlockChange(World world, int x, int y, int z, int neighborID) {
	}
}