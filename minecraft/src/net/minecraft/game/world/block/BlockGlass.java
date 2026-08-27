package net.minecraft.game.world.block;

import java.util.Random;
import net.minecraft.game.world.material.Material;

public final class BlockGlass extends BlockBreakable {
	public BlockGlass(int blockID, int textureIndex, Material material, boolean allBounds) {
		super(blockID, textureIndex, material, false);
	}

	@Override
	public final int quantityDropped(Random random) {
		return 0;
	}
}