package net.minecraft.game.world.block;

import net.minecraft.game.world.material.Material;

/** Wool / cloth block. Standalone placeholder — colour logic will live here in a future pass. */
public final class BlockCloth extends Block {
	protected BlockCloth(int blockID, int textureIndex) {
		super(blockID, textureIndex, Material.cloth);
	}
}
