package net.minecraft.game.world.block;

import net.minecraft.game.world.material.Material;

public final class BlockLog extends Block {
	protected BlockLog(int blockID) {
		super(blockID, Material.wood);
		this.blockIndexInTexture = 20;
	}

	@Override
	public final int getBlockTextureFromSide(int side) {
		return side == 1 ? this.blockIndexInTexture + 1 : (side == 0 ? this.blockIndexInTexture + 1 : this.blockIndexInTexture);
	}
}