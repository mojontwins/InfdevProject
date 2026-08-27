package net.minecraft.game.world.block;

import java.util.Random;
import net.minecraft.game.world.material.Material;

public final class BlockBookshelf extends Block {
	public BlockBookshelf(int blockID, int textureIndex) {
		super(blockID, textureIndex, Material.wood);
	}

	@Override
	public final int getBlockTextureFromSide(int side) {
		return side <= 1 ? 4 : this.blockIndexInTexture;
	}

	@Override
	public final int quantityDropped(Random random) {
		return 0;
	}
}