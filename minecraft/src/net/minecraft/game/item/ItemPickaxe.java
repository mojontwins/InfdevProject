package net.minecraft.game.item;

import net.minecraft.game.world.block.Block;
import net.minecraft.game.world.material.Material;

/**
 * The pickaxe for hard stone and ores. Every tier digs rock and iron, but only
 * higher tiers can handle the top materials: gold needs tier 2, diamond needs
 * tier 2 as well, and obsidian specifically needs the diamond pickaxe (tier 3).
 */
public final class ItemPickaxe extends ItemTool {
	private static final ItemStack[] blocksEffectiveAgainst = new ItemStack[]{
			blockStack(Block.cobblestone, -1),
			blockStack(Block.stairDouble, -1),
			blockStack(Block.stairSingle, -1),
			blockStack(Block.stone, -1),
			blockStack(Block.cobblestoneMossy, -1),
			blockStack(Block.brick, -1),
			blockStack(Block.oreIron, -1),
			blockStack(Block.blockSteel, -1),
			blockStack(Block.oreCoal, -1),
			blockStack(Block.blockGold, -1),
			blockStack(Block.oreGold, -1),
			blockStack(Block.oreDiamond, -1),
			blockStack(Block.blockDiamond, -1),
			blockStack(Block.obsidian, -1),
			blockStack(Block.stoneOvenIdle, -1),
			blockStack(Block.stoneOvenActive, -1)
	};
	private final int harvestLevel;

	public ItemPickaxe(int itemID, int materialTier) {
		super(itemID, 2, materialTier, blocksEffectiveAgainst);
		this.harvestLevel = materialTier;
	}

	@Override
	public final boolean canHarvestBlock(Block block) {
		if (block == Block.obsidian) {
			return this.harvestLevel == 3;
		}
		if (block == Block.blockDiamond || block == Block.oreDiamond || block == Block.blockGold || block == Block.oreGold) {
			return this.harvestLevel >= 2;
		}
		if (block == Block.blockSteel || block == Block.oreIron) {
			return this.harvestLevel > 0;
		}
		return block.blockMaterial == Material.rock || block.blockMaterial == Material.iron;
	}
}