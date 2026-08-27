package net.minecraft.game.world.block;

import net.minecraft.game.world.material.Material;

public final class BlockDirt extends Block {
	protected BlockDirt(int blockID, int textureIndex) {
		super(blockID, textureIndex, Material.ground);
	}
}