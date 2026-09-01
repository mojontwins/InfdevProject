package net.minecraft.game.world.block;

import net.minecraft.game.world.material.Material;

/** Plain dirt — a valid plant base. */
public final class BlockDirt extends Block {
	protected BlockDirt(int blockID, int textureIndex) {
		super(blockID, textureIndex, Material.ground);
	}

	@Override
	public boolean canGrowPlants(int metadata) {
		return true;
	}
}