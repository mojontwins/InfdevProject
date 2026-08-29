package net.minecraft.game.item;

import net.minecraft.game.world.block.Block;

/**
 * The shovel digs loose ground: grass, dirt, farmland, sand and gravel without
 * effort.
 */
public final class ItemSpade extends ItemTool {
	private static final ItemStack[] blocksEffectiveAgainst = new ItemStack[]{
			blockStack(Block.grass, -1),
			blockStack(Block.dirt, -1),
			blockStack(Block.tilledField, -1),
			blockStack(Block.sand, -1),
			blockStack(Block.gravel, -1)
	};

	public ItemSpade(int itemID, int materialTier) {
		super(itemID, 1, materialTier, blocksEffectiveAgainst);
	}
}