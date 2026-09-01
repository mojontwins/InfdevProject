package net.minecraft.game.world.block;

import java.util.Random;
import net.minecraft.game.item.Item;

/**
 * Diamond ore: drops a diamond gem rather than the ore block itself.
 */
public final class BlockOreDiamond extends BlockOre {
	public BlockOreDiamond(int blockID, int textureIndex) {
		super(blockID, textureIndex);
	}

	@Override
	public final int idDropped(int metadata, Random random) {
		return Item.diamond.shiftedIndex;
	}
}
