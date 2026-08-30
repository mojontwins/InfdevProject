package net.minecraft.game.world.block;

import net.minecraft.game.world.material.Material;

/** Plain dirt - a material stand-in kept as a named class so {@link Block#dirt} reads clearly. */
public final class BlockDirt extends Block {
	protected BlockDirt(int blockID, int textureIndex) {
		super(blockID, textureIndex, Material.ground);
	}
}