package net.minecraft.game.item.recipe;

import net.minecraft.game.item.Item;
import net.minecraft.game.item.ItemStack;
import net.minecraft.game.world.block.Block;

/**
 * Shaped recipes converting between the three metal blocks and their ingots: a
 * block is smelted from a 3×3 square of its ingots, and a block melts back down
 * into nine ingots. Both conversions exist for gold, steel (iron) and diamond.
 */
public final class RecipesIngots {

	/** The compression blocks, one per material tier. */
	private static final Block[] INGOT_BLOCKS = {
		Block.blockGold, Block.blockSteel, Block.blockDiamond
	};

	/** The matching ingot items. */
	private static final Item[] INGOT_ITEMS = {
		Item.ingotGold, Item.ingotIron, Item.diamond
	};

	public final void addRecipe(CraftingManager manager) {
		for (int material = 0; material < INGOT_BLOCKS.length; ++material) {
			manager.addRecipe(new ItemStack(INGOT_BLOCKS[material]), "###", "###", "###", '#', INGOT_ITEMS[material]);
			manager.addRecipe(new ItemStack(INGOT_ITEMS[material], 9), "#", '#', INGOT_BLOCKS[material]);
		}
	}
}