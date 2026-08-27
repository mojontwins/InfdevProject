package net.minecraft.game.item.recipe;

import java.util.Comparator;

final class RecipeSorter implements Comparator<CraftingRecipe> {
	RecipeSorter(CraftingManager var1) {
	}

	public final int compare(CraftingRecipe var1, CraftingRecipe var2) {
		CraftingRecipe var3 = var1;
		CraftingRecipe var4 = var2;
		return var4.b() < var3.b() ? -1 : (var4.b() > var3.b() ? 1 : 0);
	}
}
