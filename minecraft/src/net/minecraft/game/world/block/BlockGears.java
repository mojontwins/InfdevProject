package net.minecraft.game.world.block;

import net.minecraft.game.physics.AxisAlignedBB;
import net.minecraft.game.world.material.Material;

public final class BlockGears extends Block {
	protected BlockGears(int blockID, int textureIndex) {
		super(blockID, textureIndex, Material.circuits);
	}

	@Override
	public final AxisAlignedBB getCollisionBoundingBoxFromPool(int x, int y, int z) {
		return null;
	}

	@Override
	public final boolean isOpaqueCube() {
		return false;
	}

	@Override
	public final boolean renderAsNormalBlock() {
		return false;
	}

	@Override
	public final int getRenderType() {
		return 5;
	}

	@Override
	public final boolean isCollidable() {
		return false;
	}
}