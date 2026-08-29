package net.minecraft.game.item.recipe;

import net.minecraft.client.gui.container.InventoryCrafting;
import net.minecraft.game.item.ItemStack;

/**
 * The contract every crafting recipe honours: a recipe can tell whether the
 * contents of a craft matrix match it, and hand out the stack it produces.
 * Crafting in this early client-only version happens entirely in the GUI layer,
 * so the matrix is the {@link InventoryCrafting} those GUIs share — exactly like
 * the recipe framework of the later Beta sources.
 *
 * <p>Both {@link ShapedRecipe} and {@link ShapelessRecipe} implement this
 * interface; {@link CraftingManager} keeps a flat catalogue of them and simply
 * returns the first match.
 */
public interface IRecipe {

	/**
	 * Whether the given craft matrix holds this recipe's ingredients. The
	 * matrix may be as small as the 2×2 one on the survival screen or the full
	 * 3×3 of the crafting table; a shaped recipe lines its pattern up within it.
	 */
	boolean matches(InventoryCrafting inventoryCrafting);

	/** A fresh copy of the stack this recipe produces, never the shared template. */
	ItemStack getCraftingResult(InventoryCrafting inventoryCrafting);

	/** Number of cells or ingredients the recipe occupies, used to order the recipe table. */
	int getRecipeSize();

	/** The stack this recipe produces, shared as a template — never to be mutated. */
	ItemStack getRecipeOutput();
}