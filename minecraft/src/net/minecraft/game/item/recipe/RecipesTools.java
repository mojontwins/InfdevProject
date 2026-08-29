package net.minecraft.game.item.recipe;

import net.minecraft.game.item.Item;
import net.minecraft.game.world.block.Block;

/**
 * Shaped recipes for the four tool shapes — pickaxe, shovel, axe and hoe — once per
 * material tier. Every tool shares the classic two-ingredient layout: the tier's
 * material in the head ({@code 'X'}), a {@link Item#stick} for the handle
 * ({@code '#'}).
 */
public final class RecipesTools extends RecipesTiered {

	/** The pickaxe, shovel, axe and hoe shapes, handle included. */
	private static final String[][] TOOL_PATTERNS = {
		{"XXX", " # ", " # "},
		{"X", "#", "#"},
		{"XX", "X#", " #"},
		{"XX", " #", " #"}
	};

	/** The five tool materials: wood, cobblestone, steel, diamond and gold. */
	private static final Object[] TOOL_MATERIALS = {
		Block.planks, Block.cobblestone, Item.ingotIron, Item.diamod, Item.ingotGold
	};

	/** Output item per (tool, material) pair. */
	private static final Item[][] TOOL_OUTPUTS = {
		{Item.pickaxeWood, Item.pickaxeStone, Item.pickaxeSteel, Item.pickaxeDiamond, Item.pickaxeGold},
		{Item.shovelWood, Item.shovelStone, Item.shovel, Item.shovelDiamond, Item.shovelGold},
		{Item.axeWood, Item.axeStone, Item.axeSteel, Item.axeDiamond, Item.axeGold},
		{Item.hoeWood, Item.hoeStone, Item.hoeSteel, Item.hoeDiamond, Item.hoeGold}
	};

	public RecipesTools() {
		super(TOOL_PATTERNS, TOOL_MATERIALS, TOOL_OUTPUTS);
	}

	/**
	 * Registers all four tools per material tier; each tool pairs the material head
	 * with a wooden stick handle ({@code '#'}).
	 */
	@Override
	public void addRecipe(CraftingManager manager) {
		super.addRecipe(manager, Item.stick);
	}
}