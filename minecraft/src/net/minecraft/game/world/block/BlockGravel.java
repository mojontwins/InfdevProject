package net.minecraft.game.world.block;

import java.util.Random;
import net.minecraft.game.item.Item;

public final class BlockGravel extends BlockSand {
	public BlockGravel(int blockID, int textureIndex) {
		super(blockID, textureIndex);
	}

	@Override
	public final int idDropped(int metadata, Random random) {
		return random.nextInt(10) == 0 ? Item.flint.shiftedIndex : this.blockID;
	}
}