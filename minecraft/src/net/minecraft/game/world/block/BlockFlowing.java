package net.minecraft.game.world.block;

import java.util.Random;
import net.minecraft.game.world.World;
import net.minecraft.game.world.material.Material;

/**
 * The "moving" role of a liquid (waterMoving id 8, lavaMoving id 10). In this
 * build everything is inert here on purpose: the base flow engine in
 * {@link BlockFluid#update} is disabled (this class overrides {@link #update}
 * to just {@code false}), so a placed flowing block never spreads on its own
 * and ignores neighbour changes ({@link #onNeighborBlockChange}). Flow happens
 * only through the {@link BlockStationary} side, which converts itself into a
 * moving id when a neighbour changes.
 */
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