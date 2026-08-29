package net.minecraft.game.item;

import net.minecraft.game.world.block.Block;

/**
 * The axe chews through wood: planks, bookshelves, logs, chests and the
 * crafting table.
 */
public final class ItemAxe extends ItemTool {
	private static final ItemStack[] blocksEffectiveAgainst = new ItemStack[]{
			blockStack(Block.planks, -1),
			blockStack(Block.bookshelf, -1),
			blockStack(Block.wood, -1),
			blockStack(Block.chest, -1),
			blockStack(Block.workbench, -1)
	};

	public ItemAxe(int itemID, int materialTier) {
		super(itemID, 3, materialTier, blocksEffectiveAgainst);
	}
}