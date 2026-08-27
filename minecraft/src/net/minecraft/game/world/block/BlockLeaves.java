package net.minecraft.game.world.block;

import java.util.Random;
import net.minecraft.game.world.material.Material;

public final class BlockLeaves extends BlockLeavesBase {
	protected BlockLeaves(int var1, int var2) {
		super(18, 52, Material.leaves, true);
		this.setTickOnLoad(true);
	}

	public final int quantityDropped(Random var1) {
		return var1.nextInt(10) == 0 ? 1 : 0;
	}

	public final int idDropped(int var1, Random var2) {
		return Block.sapling.blockID;
	}
}
