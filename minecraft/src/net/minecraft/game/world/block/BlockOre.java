package net.minecraft.game.world.block;

import java.util.Random;
import net.minecraft.game.world.material.Material;

/**
 * Ore blocks. The plain metal ores drop themselves (raw ore, smelted later);
 * the gem ores ({@link BlockOreCoal}, {@link BlockOreDiamond}) override
 * {@link #idDropped} to drop their gem.
 */
public class BlockOre extends Block {
	public BlockOre(int blockID, int textureIndex) {
		super(blockID, textureIndex, Material.rock);
	}

	@Override
	public int idDropped(int metadata, Random random) {
		return this.blockID;
	}
}