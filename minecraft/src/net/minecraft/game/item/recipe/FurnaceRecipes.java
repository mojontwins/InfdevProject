package net.minecraft.game.item.recipe;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.game.item.Item;
import net.minecraft.game.item.ItemStack;
import net.minecraft.game.world.block.Block;

/**
 * The singleton catalogue of every smelting recipe the furnace knows. A recipe
 * pairs a complete input stack (item id and damage) with the complete output
 * stack it produces; a damage of {@code -1} on the input means "matches any
 * damage". The furnace queries it via {@link #getSmeltingResult(ItemStack)}
 * instead of hard-coding its smelting table, mirroring the later Beta sources.
 *
 * <p>The entries are this version's original smelting table, reproduced exactly:
 * the three ores, sand to glass, raw pork to its cooked form and cobblestone
 * back to stone.
 */
public final class FurnaceRecipes {

	private static final FurnaceRecipes SMELTING_BASE = new FurnaceRecipes();

	/** The smelting table: recipe key → complete output stack. */
	private final Map<Integer, ItemStack> smeltingList = new HashMap<>();

	public static final FurnaceRecipes smelting() {
		return SMELTING_BASE;
	}

	private FurnaceRecipes() {
		this.addSmelting(new ItemStack(Block.oreIron), new ItemStack(Item.ingotIron));
		this.addSmelting(new ItemStack(Block.oreGold), new ItemStack(Item.ingotGold));
		this.addSmelting(new ItemStack(Block.oreDiamond), new ItemStack(Item.diamod));
		this.addSmelting(new ItemStack(Block.sand), new ItemStack(Block.glass));
		this.addSmelting(new ItemStack(Item.porkRaw), new ItemStack(Item.porkCooked));
		this.addSmelting(new ItemStack(Block.cobblestone), new ItemStack(Block.stone));
	}

	/**
	 * Registers one smelting recipe. The recipe keeps both stacks whole: an
	 * input damage of {@code -1} declares the entry valid for any damage of the
	 * input item.
	 */
	public void addSmelting(ItemStack smeltedItem, ItemStack smeltingResult) {
		this.smeltingList.put(recipeKey(smeltedItem.itemID, smeltedItem.itemDamage), smeltingResult);
	}

	/**
	 * The complete stack this input smelts into, or {@code null} when it does
	 * not smelt. An exact (item id, damage) entry wins; otherwise a "any damage"
	 * entry ({@code -1}) for the same item id is consulted.
	 */
	public ItemStack getSmeltingResult(ItemStack smeltedItem) {
		ItemStack result = this.smeltingList.get(recipeKey(smeltedItem.itemID, smeltedItem.itemDamage));
		if (result == null) {
			result = this.smeltingList.get(recipeKey(smeltedItem.itemID, -1));
		}
		return result;
	}

	public Map<Integer, ItemStack> getSmeltingList() {
		return this.smeltingList;
	}

	/** Packs item id and damage into one key; the {@code -1} "any" damage lands in the high slot {@code 0xFFFF}. */
	private static int recipeKey(int itemID, int itemDamage) {
		return itemID << 16 | itemDamage & 65535;
	}
}