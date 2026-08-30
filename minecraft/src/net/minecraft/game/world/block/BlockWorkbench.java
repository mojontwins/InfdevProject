package net.minecraft.game.world.block;

import net.minecraft.game.entity.player.EntityPlayer;
import net.minecraft.game.world.World;
import net.minecraft.game.world.material.Material;

/** Crafting table: fires the 3x3 crafting GUI on activation. */
public final class BlockWorkbench extends Block {
	protected BlockWorkbench(int blockID) {
		super(blockID, Material.wood);
		this.blockIndexInTexture = 59;
	}

	@Override
	public final int getBlockTextureFromSide(int side) {
		return side == 1 ? this.blockIndexInTexture - 16 : (side == 0 ? Block.planks.getBlockTextureFromSide(0) : (side != 2 && side != 4 ? this.blockIndexInTexture : this.blockIndexInTexture + 1));
	}

	@Override
	public final boolean blockActivated(World world, int x, int y, int z, EntityPlayer player) {
		player.displayWorkbenchGUI();
		return true;
	}
}