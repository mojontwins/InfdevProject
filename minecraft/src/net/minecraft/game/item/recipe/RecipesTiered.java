package net.minecraft.game.item.recipe;

import net.minecraft.game.item.Item;
import net.minecraft.game.item.ItemStack;

/**
 * Base class for the "tiered" shaped recipes (armour, tools, weapons). Every
 * tiered set shares the same shape: a few patterns applied once per material tier,
 * producing one output item per (pattern, tier) pair. The subclasses differ only in
 * their shape table, their repertoire of materials and the matching output table, so
 * the whole registration loop lives here instead of being copy-pasted three times.
 *
 * <p>Shapes use the symbol {@code 'X'} for the tier's base material. Subclasses that
 * need a second ingredient — the wooden stick handle of tools and weapons — call
 * {@link #addRecipe(CraftingManager, Item)}; the registration loop then additionally
 * maps {@code '#'} to it. This reproduces the original recipes exactly, in the same
 * (material, pattern) registration order.
 */
public abstract class RecipesTiered {

	/** One entry per tier, in the order the materials appear in {@link #recipeMaterials}. */
	private final Item[][] recipeOutputs;

	/** The materials of the five tiers (leather, chain/wood, steel/stone, gold, diamond). */
	private final Object[] recipeMaterials;

	/** The shapes applied to each material tier. */
	private final String[][] recipePatterns;

	protected RecipesTiered(String[][] recipePatterns, Object[] recipeMaterials, Item[][] recipeOutputs) {
		this.recipePatterns = recipePatterns;
		this.recipeMaterials = recipeMaterials;
		this.recipeOutputs = recipeOutputs;
	}

	/**
	 * Registers every (material, pattern) combination as a shaped recipe whose sole
	 * non-empty ingredient symbol is {@code 'X'} — used by sets with no handle.
	 */
	public void addRecipe(CraftingManager manager) {
		this.addRecipe(manager, null);
	}

	/**
	 * Registers every (material, pattern) combination, mapping {@code 'X'} to the
	 * tier's material and {@code '#'} to {@code handleItem} (the stick).
	 */
	public final void addRecipe(CraftingManager manager, Item handleItem) {
		for (int materialTier = 0; materialTier < this.recipeMaterials.length; ++materialTier) {
			Object material = this.recipeMaterials[materialTier];
			for (int recipeKind = 0; recipeKind < this.recipePatterns.length; ++recipeKind) {
				manager.addRecipe(new ItemStack(this.recipeOutputs[recipeKind][materialTier]), this.ingredientArgs(recipeKind, material, handleItem));
			}
		}
	}

	/** Builds the varargs of one shaped recipe: the shape plus its symbol → ingredient pairs. */
	private Object[] ingredientArgs(int recipeKind, Object material, Item handleItem) {
		if (handleItem != null) {
			return new Object[]{this.recipePatterns[recipeKind], Character.valueOf('X'), material, Character.valueOf('#'), handleItem};
		}
		return new Object[]{this.recipePatterns[recipeKind], Character.valueOf('X'), material};
	}
}