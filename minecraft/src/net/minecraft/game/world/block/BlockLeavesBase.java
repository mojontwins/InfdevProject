package net.minecraft.game.world.block;

import net.minecraft.game.world.IBlockAccess;
import net.minecraft.game.world.material.Material;

public class BlockLeavesBase extends Block {
	private boolean graphicsLevel = true;

	protected BlockLeavesBase(int blockID, int textureIndex, Material material) {
		super(blockID, textureIndex, material);
	}

	@Override
	public final boolean isOpaqueCube() {
		return false;
	}

	@Override
	public final boolean shouldSideBeRendered(IBlockAccess blockAccess, int x, int y, int z, int side) {
		int neighborBlockID = blockAccess.getBlockId(x, y, z);
		return !this.graphicsLevel && neighborBlockID == this.blockID ? false : super.shouldSideBeRendered(blockAccess, x, y, z, side);
	}
}