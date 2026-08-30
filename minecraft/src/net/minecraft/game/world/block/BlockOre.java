package net.minecraft.game.world.block;

import java.util.Random;
import net.minecraft.game.item.Item;
import net.minecraft.game.world.material.Material;

/**
 * Ore blocks: the gem ore drops its gem (diamond / coal) and the plain metal
 * ores drop themselves (as raw ore, smelted later).
 */
public final class BlockOre extends Block {
	public BlockOre(int blockID, int textureIndex) {
		super(blockID, textureIndex, Material.rock);
	}

	@Override
	public final int idDropped(int metadata, Random random) {
		return this.blockID == Block.oreCoal.blockID ? Item.coal.shiftedIndex : (this.blockID == Block.oreDiamond.blockID ? Item.diamod.shiftedIndex : this.blockID);
	}
}