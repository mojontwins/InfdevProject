package net.minecraft.game.world.block;

import java.util.Random;
import net.minecraft.game.world.material.Material;

/** Stone - and, reusing this class, obsidian: both drop cobblestone rather than themselves. */
public final class BlockStone extends Block {
	public BlockStone(int blockID, int textureIndex) {
		super(blockID, textureIndex, Material.rock);
	}

	@Override
	public final int idDropped(int metadata, Random random) {
		return Block.cobblestone.blockID;
	}
}