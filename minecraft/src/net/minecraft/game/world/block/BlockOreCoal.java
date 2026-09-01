package net.minecraft.game.world.block;

import java.util.Random;
import net.minecraft.game.item.Item;

/**
 * Coal ore: drops a lump of coal rather than the ore block itself.
 */
public final class BlockOreCoal extends BlockOre {
	public BlockOreCoal(int blockID, int textureIndex) {
		super(blockID, textureIndex);
	}

	@Override
	public final int idDropped(int metadata, Random random) {
		return Item.coal.shiftedIndex;
	}
}
