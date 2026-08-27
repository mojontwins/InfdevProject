package net.minecraft.game.world.block;

import java.util.Random;
import net.minecraft.game.world.World;
import net.minecraft.game.world.material.Material;

public final class BlockStep extends Block {
	private boolean doubleSlab;

	public BlockStep(int blockID, boolean doubleSlab) {
		super(blockID, 6, Material.rock);
		this.doubleSlab = doubleSlab;
		if(!doubleSlab) {
			this.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 0.5F, 1.0F);
		}
		this.setLightOpacity(255);
	}

	@Override
	public final int getBlockTextureFromSide(int side) {
		return side <= 1 ? 6 : 5;
	}

	@Override
	public final boolean isOpaqueCube() {
		return this.doubleSlab;
	}

	@Override
	public final void onBlockAdded(World world, int x, int y, int z) {
		int belowBlockID = world.getBlockId(x, y - 1, z);
		if(belowBlockID == Block.stairSingle.blockID) {
			world.setBlockWithNotify(x, y, z, 0);
			world.setBlockWithNotify(x, y - 1, z, Block.stairDouble.blockID);
		}
	}

	@Override
	public final int idDropped(int metadata, Random random) {
		return Block.stairSingle.blockID;
	}

	@Override
	public final boolean renderAsNormalBlock() {
		return this.doubleSlab;
	}

	@Override
	public final boolean shouldSideBeRendered(World world, int x, int y, int z, int side) {
		boolean renderSide = super.shouldSideBeRendered(world, x, y, z, side);
		return side == 1 ? true : (!renderSide ? false : (side == 0 ? true : world.getBlockId(x, y, z) != this.blockID));
	}
}