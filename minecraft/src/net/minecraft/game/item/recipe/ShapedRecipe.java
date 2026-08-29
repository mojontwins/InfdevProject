package net.minecraft.game.item.recipe;

import net.minecraft.client.gui.container.InventoryCrafting;
import net.minecraft.game.item.ItemStack;

/**
 * One shaped crafting recipe: a compact {@code width × height} grid of item
 * stacks where {@code null} marks an empty cell, together with the stack it
 * produces. Ingredients are matched by item id and, unless the ingredient's
 * damage is the special {@code -1} wildcard meaning "any", by damage as well.
 *
 * <p>A recipe is matched against the craft matrix in every possible alignment
 * and, because a mirrored arrangement is still the same shape, also flipped
 * horizontally. Both the 2×2 matrix of the survival screen and the crafting
 * table's 3×3 are supported, since the pattern only needs to fit.
 */
public final class ShapedRecipe implements IRecipe {

	/** Pattern width, i.e. the length of one recipe row. */
	private final int width;

	/** Pattern height, i.e. the number of recipe rows. */
	private final int height;

	/**
	 * The recipe shape, row by row: the ingredient {@link ItemStack} in each
	 * cell, {@code null} = empty cell, damage {@code -1} = "any damage".
	 */
	private final ItemStack[] ingredientGrid;

	/** The stack handed back every time the recipe is matched. */
	private final ItemStack recipeOutput;

	public ShapedRecipe(int width, int height, ItemStack[] ingredientGrid, ItemStack recipeOutput) {
		this.width = width;
		this.height = height;
		this.ingredientGrid = ingredientGrid;
		this.recipeOutput = recipeOutput;
	}

	/**
	 * Whether the given craft matrix contains this recipe's shape in any
	 * position and either horizontal orientation.
	 */
	@Override
	public final boolean matches(InventoryCrafting inventoryCrafting) {
		int matrixWidth = inventoryCrafting.getInventoryWidth();
		int matrixHeight = inventoryCrafting.getInventoryHeight();
		for (int yOffset = 0; yOffset <= matrixHeight - this.height; ++yOffset) {
			for (int xOffset = 0; xOffset <= matrixWidth - this.width; ++xOffset) {
				if (this.checkMatch(inventoryCrafting, xOffset, yOffset, true) || this.checkMatch(inventoryCrafting, xOffset, yOffset, false)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Whether the shape lines up on the matrix when placed at the given offset,
	 * either upright or mirrored horizontally. Cells outside the shape must be
	 * empty, cells inside must hold the recipe's ingredient.
	 */
	private boolean checkMatch(InventoryCrafting inventoryCrafting, int xOffset, int yOffset, boolean flipped) {
		int matrixWidth = inventoryCrafting.getInventoryWidth();
		int matrixHeight = inventoryCrafting.getInventoryHeight();
		for (int matrixRow = 0; matrixRow < matrixHeight; ++matrixRow) {
			for (int matrixColumn = 0; matrixColumn < matrixWidth; ++matrixColumn) {
				int patternX = matrixColumn - xOffset;
				int patternY = matrixRow - yOffset;
				ItemStack required = null;
				if (patternX >= 0 && patternY >= 0 && patternX < this.width && patternY < this.height) {
					if (flipped) {
						required = this.ingredientGrid[this.width - patternX - 1 + patternY * this.width];
					} else {
						required = this.ingredientGrid[patternX + patternY * this.width];
					}
				}
				if (!this.stacksMatch(inventoryCrafting.getStackInRowAndColumn(matrixColumn, matrixRow), required)) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * Whether the placed stack is the ingredient the recipe expects: same item
	 * id and, unless the ingredient declares "any" with a {@code -1} damage, the
	 * same damage. Two nulls are a match, a null and a stack never are.
	 */
	private static boolean stacksMatch(ItemStack placed, ItemStack required) {
		if (placed == null || required == null) {
			return placed == required;
		}
		return required.itemID == placed.itemID && (required.itemDamage == -1 || required.itemDamage == placed.itemDamage);
	}

	/**
	 * A fresh stack of the recipe's result, carrying the template's damage. A
	 * copy every call, never the template itself, so the recipe table can never
	 * be mutated by one of its consumers.
	 */
	@Override
	public final ItemStack getCraftingResult(InventoryCrafting inventoryCrafting) {
		return new ItemStack(this.recipeOutput.itemID, this.recipeOutput.stackSize, this.recipeOutput.itemDamage);
	}

	/** Covered area of the shape, used to sort recipes so the largest pattern wins. */
	@Override
	public final int getRecipeSize() {
		return this.width * this.height;
	}

	@Override
	public final ItemStack getRecipeOutput() {
		return this.recipeOutput;
	}
}