package net.minecraft.game.item.recipe;

import net.minecraft.game.item.Item;
import net.minecraft.game.world.block.Block;

/**
 * Shaped recipes for all four armour pieces, once per material tier. The shapes cover
 * helmet, chest plate, leggings and boots; the results fill the armour slots of an
 * entity's inventory ({@link net.minecraft.game.entity.EntityLiving}).
 *
 * <p>This pre-release version knows only five materials per slot — and it predates the
 * modern "chainmail needs iron" recipe: chain armour is forged out of literal
 * <em>fire</em> ({@link Block#fire}). A genuine Infdev quirk, preserved faithfully.
 */
public final class RecipesArmor extends RecipesTiered {

	/** The helmet, chest plate, leggings and boots shapes. */
	private static final String[][] ARMOUR_PATTERNS = {
		{"XXX", "X X"},
		{"X X", "XXX", "XXX"},
		{"XXX", "X X", "X X"},
		{"X X", "X X"}
	};

	/** The five armour materials: leather, chain (fire!), steel, diamond and gold. */
	private static final Object[] ARMOUR_MATERIALS = {
		Block.cloth, Block.fire, Item.ingotIron, Item.diamond, Item.ingotGold
	};

	/** Output item per (armour piece, material) pair. */
	private static final Item[][] ARMOUR_OUTPUTS = {
		{Item.helmetLeather, Item.helmetChain, Item.helmetSteel, Item.helmetDiamond, Item.helmetGold},
		{Item.plateLeather, Item.plateChain, Item.plateSteel, Item.plateDiamond, Item.plateGold},
		{Item.legsLeather, Item.legsChain, Item.legsSteel, Item.legsDiamond, Item.legsGold},
		{Item.bootsLeather, Item.bootsChain, Item.bootsSteel, Item.bootsDiamond, Item.bootsGold}
	};

	public RecipesArmor() {
		super(ARMOUR_PATTERNS, ARMOUR_MATERIALS, ARMOUR_OUTPUTS);
	}
}