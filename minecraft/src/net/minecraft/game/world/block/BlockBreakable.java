package net.minecraft.game.world.block;

import net.minecraft.game.world.IBlockAccess;
import net.minecraft.game.world.material.Material;

public class BlockBreakable extends Block {
	private boolean renderAllFaces;

	protected BlockBreakable(int blockID, int textureIndex, Material material, boolean renderAllFaces) {
		super(blockID, textureIndex, material);
		this.renderAllFaces = renderAllFaces;
	}

	@Override
	public final boolean isOpaqueCube() {
		return false;
	}

	@Override
	public final boolean shouldSideBeRendered(IBlockAccess blockAccess, int x, int y, int z, int side) {
		int neighborBlockID = blockAccess.getBlockId(x, y, z);
		return !this.renderAllFaces && neighborBlockID == this.blockID ? false : super.shouldSideBeRendered(blockAccess, x, y, z, side);
	}
}