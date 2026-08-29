package net.minecraft.game.item.recipe;

import net.minecraft.game.item.Item;
import net.minecraft.game.item.ItemStack;
import net.minecraft.game.world.block.Block;

/**
 * Shaped recipes for the melee sword (one per material tier) plus the two fixed
 * ranged weapons — bow and a four-pack of arrows — that use no material tiers.
 */
public final class RecipesWeapons extends RecipesTiered {

	/** The sword shape: a blade over a handle. */
	private static final String[][] SWORD_PATTERNS = {
		{"X", "X", "#"}
	};

	/** The five sword materials: wood, cobblestone, steel, diamond and gold. */
	private static final Object[] SWORD_MATERIALS = {
		Block.planks, Block.cobblestone, Item.ingotIron, Item.diamod, Item.ingotGold
	};

	/** Output sword per material tier. */
	private static final Item[][] SWORD_OUTPUTS = {
		{Item.swordWood, Item.swordStone, Item.swordSteel, Item.swordDiamond, Item.swordGold}
	};

	public RecipesWeapons() {
		super(SWORD_PATTERNS, SWORD_MATERIALS, SWORD_OUTPUTS);
	}

	/**
	 * Registers the tiered swords (a blade {@code 'X'} over a stick handle
	 * {@code '#'}), then the two weapons with a fixed recipe: a bow strung with
	 * silk, and a four-pack of arrows — iron-tipped, stick-shafted and fletched
	 * with a feather.
	 */
	@Override
	public void addRecipe(CraftingManager manager) {
		super.addRecipe(manager, Item.stick);
		manager.addRecipe(new ItemStack(Item.bow, 1), " #X", "# X", " #X", 'X', Item.silk, '#', Item.stick);
		manager.addRecipe(new ItemStack(Item.arrow, 4), "X", "#", "Y", 'Y', Item.feather, 'X', Item.ingotIron, '#', Item.stick);
	}
}