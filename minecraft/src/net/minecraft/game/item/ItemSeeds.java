package net.minecraft.game.item;

import net.minecraft.game.world.World;
import net.minecraft.game.world.block.Block;

/**
 * Seeds are planted on the top face of a tilled field, replacing the block
 * above it with the growing crop and shrinking the stack by one.
 */
public final class ItemSeeds extends Item {
	private final int cropBlockID;

	public ItemSeeds(int itemID, int cropBlockID) {
		super(itemID);
		this.cropBlockID = cropBlockID;
	}

	@Override
	public final boolean onItemUse(ItemStack stack, World world, int x, int y, int z, int side) {
		if (side != 1) {
			return false;
		} else {
			int blockID = world.getBlockId(x, y, z);
			if (blockID == Block.tilledField.blockID) {
				world.setBlockWithNotify(x, y + 1, z, this.cropBlockID);
				--stack.stackSize;
				return true;
			} else {
				return false;
			}
		}
	}
}