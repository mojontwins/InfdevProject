package net.minecraft.game.item.recipe;

import net.minecraft.game.item.ItemStack;

/**
 * One shaped crafting recipe: a compact {@code width × height} grid of item ids where
 * {@code -1} marks an empty cell, together with the stack it produces. A recipe is
 * matched against the 3×3 crafting matrix in every possible alignment and, because a
 * mirrored arrangement is still the same shape, also flipped horizontally.
 */
public final class CraftingRecipe {

	/** Grid width, i.e. the length of one pattern row. */
	private final int width;

	/** Grid height, i.e. the number of pattern rows. */
	private final int height;

	/** The recipe shape, row by row: item ids, {@code -1} = empty cell. */
	private final int[] ingredientGrid;

	/** The stack handed back every time the recipe is matched. */
	private final ItemStack recipeOutput;

	public CraftingRecipe(int width, int height, int[] ingredientGrid, ItemStack recipeOutput) {
		this.width = width;
		this.height = height;
		this.ingredientGrid = ingredientGrid;
		this.recipeOutput = recipeOutput;
	}

	/**
	 * Whether the given 3×3 crafting matrix contains this recipe's shape in any
	 * position and either horizontal orientation.
	 */
	public final boolean matchRecipe(int[] grid) {
		for (int xOffset = 0; xOffset <= 3 - this.width; ++xOffset) {
			for (int yOffset = 0; yOffset <= 3 - this.height; ++yOffset) {
				if (this.matches(grid, xOffset, yOffset, true) || this.matches(grid, xOffset, yOffset, false)) {
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
	private boolean matches(int[] grid, int xOffset, int yOffset, boolean flipped) {
		for (int gridColumn = 0; gridColumn < 3; ++gridColumn) {
			for (int gridRow = 0; gridRow < 3; ++gridRow) {
				int patternX = gridColumn - xOffset;
				int patternY = gridRow - yOffset;
				int expected = -1;
				if (patternX >= 0 && patternY >= 0 && patternX < this.width && patternY < this.height) {
					if (flipped) {
						expected = this.ingredientGrid[this.width - patternX - 1 + patternY * this.width];
					} else {
						expected = this.ingredientGrid[patternX + patternY * this.width];
					}
				}
				if (grid[gridColumn + gridRow * 3] != expected) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * A fresh stack of the recipe's result. A copy every call, never the shared
	 * template, so the recipe table can never be mutated by one of its consumers.
	 */
	public final ItemStack createResult() {
		return new ItemStack(this.recipeOutput.itemID, this.recipeOutput.stackSize);
	}

	/** Covered area of the shape, used to sort recipes so the largest pattern wins. */
	public final int getRecipeArea() {
		return this.width * this.height;
	}
}