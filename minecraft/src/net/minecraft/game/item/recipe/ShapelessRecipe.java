package net.minecraft.game.item.recipe;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.gui.container.InventoryCrafting;
import net.minecraft.game.item.ItemStack;

/**
 * A shapeless crafting recipe: an unordered set of ingredients (same item id
 * and damage, with {@code -1} damage again meaning "any") that produces one
 * output stack. Where a shaped recipe fixes the cells of a pattern, here only
 * the multiset of ingredients matters, mirroring the Beta recipe framework this
 * class is translated from.
 */
public final class ShapelessRecipe implements IRecipe {

	/** The stack this recipe produces, shared as a template — never to be mutated. */
	private final ItemStack recipeOutput;

	/** The ingredients, one entry per required item (duplicates mean multiples). */
	private final List<ItemStack> recipeItems;

	public ShapelessRecipe(ItemStack recipeOutput, List<ItemStack> recipeItems) {
		this.recipeOutput = recipeOutput;
		this.recipeItems = recipeItems;
	}

	/**
	 * Whether the matrix holds exactly the recipe's ingredients: each non-empty
	 * cell must claim one as-yet-unclaimed ingredient, and afterwards none may
	 * be left over. Where the shaped recipe lines up a pattern, here the order
	 * of the cells is irrelevant.
	 */
	@Override
	public final boolean matches(InventoryCrafting inventoryCrafting) {
		List<ItemStack> unclaimed = new ArrayList<>(this.recipeItems);
		for (int slot = 0; slot < inventoryCrafting.getInventorySize(); ++slot) {
			ItemStack placed = inventoryCrafting.getStackInSlot(slot);
			if (placed == null) {
				continue;
			}
			boolean claimed = false;
			for (Iterator<ItemStack> ingredientIterator = unclaimed.iterator(); ingredientIterator.hasNext();) {
				ItemStack ingredient = ingredientIterator.next();
				if (ingredient.itemID == placed.itemID && (ingredient.itemDamage == -1 || ingredient.itemDamage == placed.itemDamage)) {
					ingredientIterator.remove();
					claimed = true;
					break;
				}
			}
			if (!claimed) {
				return false;
			}
		}
		return unclaimed.isEmpty();
	}

	/** A fresh copy of the recipe's output. */
	@Override
	public final ItemStack getCraftingResult(InventoryCrafting inventoryCrafting) {
		return this.recipeOutput.copy();
	}

	/** Number of ingredients, used to order the recipe table. */
	@Override
	public final int getRecipeSize() {
		return this.recipeItems.size();
	}

	@Override
	public final ItemStack getRecipeOutput() {
		return this.recipeOutput;
	}
}