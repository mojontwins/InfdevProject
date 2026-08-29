package net.minecraft.game.item.recipe;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.game.item.Item;
import net.minecraft.game.item.ItemStack;
import net.minecraft.game.world.block.Block;

/**
 * The singleton catalogue of every shaped recipe the game knows. A recipe is just a
 * pattern of item ids; the 3×3 crafting matrix of the crafting table
 * ({@link net.minecraft.client.gui.container.GuiCrafting}) as well as the small 2×2
 * one on the survival screen are both matched against this catalogue via
 * {@link #findMatchingRecipe(int[])}.
 */
public final class CraftingManager {

	private static final CraftingManager instance = new CraftingManager();

	private final List<CraftingRecipe> recipes = new ArrayList<>();

	public static final CraftingManager getInstance() {
		return instance;
	}

	/**
	 * Builds the whole recipe table, registering the recipes in their original
	 * order and then sorting the list biggest-pattern-first. The sort matters:
	 * {@link #findMatchingRecipe(int[])} returns the first match, so a large shape
	 * must win over a small one that happens to also fit.
	 */
	private CraftingManager() {
		new RecipesTools().addRecipe(this);
		new RecipesWeapons().addRecipe(this);
		new RecipesIngots().addRecipe(this);

		// Mushroom soup — the mushrooms may sit in either order.
		this.addRecipe(new ItemStack(Item.bowlSoup), "Y", "X", "#", 'Y', Block.mushroomRed, 'X', Block.mushroomBrown, '#', Item.bowlEmpty);
		this.addRecipe(new ItemStack(Item.bowlSoup), "Y", "X", "#", 'Y', Block.mushroomBrown, 'X', Block.mushroomRed, '#', Item.bowlEmpty);

		// The storage and craft blocks.
		this.addRecipe(new ItemStack(Block.chest), "###", "# #", "###", '#', Block.planks);
		this.addRecipe(new ItemStack(Block.stoneOvenIdle), "###", "# #", "###", '#', Block.cobblestone);
		this.addRecipe(new ItemStack(Block.workbench), "##", "##", '#', Block.planks);
		new RecipesArmor().addRecipe(this);

		// The rest, in their original registration order.
		this.addRecipe(new ItemStack(Block.clothGray, 1), "###", "###", "###", '#', Item.silk);
		this.addRecipe(new ItemStack(Block.tnt, 1), "X#X", "#X#", "X#X", 'X', Item.gunpowder, '#', Block.sand);
		this.addRecipe(new ItemStack(Block.stairSingle, 3), "###", '#', Block.cobblestone);
		this.addRecipe(new ItemStack(Block.planks, 4), "#", '#', Block.wood);
		this.addRecipe(new ItemStack(Item.stick, 4), "#", "#", '#', Block.planks);
		this.addRecipe(new ItemStack(Block.torch, 4), "X", "#", 'X', Item.coal, '#', Item.stick);
		this.addRecipe(new ItemStack(Item.bowlEmpty, 4), "# #", " # ", '#', Block.planks);
		this.addRecipe(new ItemStack(Item.flintAndSteel, 1), "A ", " B", 'A', Item.ingotIron, 'B', Item.flint);
		this.addRecipe(new ItemStack(Item.bread, 1), "###", '#', Item.wheat);
		this.addRecipe(new ItemStack(Item.painting, 1), "###", "#X#", "###", '#', Item.stick, 'X', Block.clothGray);
		this.addRecipe(new ItemStack(Item.appleGold, 1), "###", "#X#", "###", '#', Block.blockGold, 'X', Item.apple);

		this.recipes.sort(Comparator.comparingInt(CraftingRecipe::getRecipeArea).reversed());
		System.out.println(this.recipes.size() + " recipes");
	}

	/**
	 * Registers one shaped recipe. The varargs {@code parts} read in two sections:
	 * <ol>
	 * <li><b>the shape</b> — either a single {@code String[]} of equal-length rows,
	 * or a run of consecutive {@code String} rows (one character per cell, any
	 * character serves as a symbol);</li>
	 * <li><b>one {@code Character} + ingredient pair per symbol</b>, where an
	 * ingredient may be an {@link Item} or a {@link Block}. A symbol without a
	 * matching pair simply means "empty cell".</li>
	 * </ol>
	 * Every row must have the same length; the shape's bounding box becomes the
	 * recipe's width and height.
	 */
	final void addRecipe(ItemStack recipeOutput, Object... parts) {
		int width = 0;
		int height = 0;
		String shapeFlat = "";
		int cursor = 0;

		if (parts[0] instanceof String[]) {
			String[] rows = (String[]) parts[0];
			cursor = 1;
			for (String row : rows) {
				width = row.length();
				++height;
				shapeFlat += row;
			}
		} else {
			while (parts[cursor] instanceof String) {
				String row = (String) parts[cursor++];
				width = row.length();
				++height;
				shapeFlat += row;
			}
		}

		Map<Character, Integer> symbolToItemId = new HashMap<>();
		for (; cursor < parts.length; cursor += 2) {
			Character symbol = (Character) parts[cursor];
			symbolToItemId.put(symbol, ingredientId(parts[cursor + 1]));
		}

		int cellCount = width * height;
		int[] ingredientGrid = new int[cellCount];
		for (int cell = 0; cell < cellCount; ++cell) {
			char symbol = shapeFlat.charAt(cell);
			ingredientGrid[cell] = symbolToItemId.containsKey(symbol) ? symbolToItemId.get(symbol) : -1;
		}

		this.recipes.add(new CraftingRecipe(width, height, ingredientGrid, recipeOutput));
	}

	/**
	 * The grid id of one ingredient: items use their shifted index, blocks their
	 * block id, anything else maps to id 0.
	 */
	private static int ingredientId(Object ingredient) {
		if (ingredient instanceof Item) {
			return ((Item) ingredient).shiftedIndex;
		}
		if (ingredient instanceof Block) {
			return ((Block) ingredient).blockID;
		}
		return 0;
	}

	/**
	 * The result of the first recipe matching the given 3×3 crafting matrix, or
	 * {@code null} when nothing matches. Sorting biggest-pattern-first makes sure
	 * an overlapping small recipe never shadows a larger one.
	 */
	public final ItemStack findMatchingRecipe(int[] craftingMatrix) {
		return this.recipes.stream()
			.filter(recipe -> recipe.matchRecipe(craftingMatrix))
			.map(CraftingRecipe::createResult)
			.findFirst()
			.orElse(null);
	}
}