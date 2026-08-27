package net.minecraft.game.world.block;

import java.util.Random;
import net.minecraft.game.world.material.Material;

public final class BlockLeaves extends BlockLeavesBase {
	protected BlockLeaves(int blockID, int textureIndex) {
		super(blockID, textureIndex, Material.leaves);
		this.setTickOnLoad(true);
	}

	@Override
	public final int quantityDropped(Random random) {
		return random.nextInt(10) == 0 ? 1 : 0;
	}

	@Override
	public final int idDropped(int metadata, Random random) {
		return Block.sapling.blockID;
	}
}