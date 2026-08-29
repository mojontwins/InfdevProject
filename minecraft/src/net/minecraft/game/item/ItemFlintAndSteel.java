package net.minecraft.game.item;

import net.minecraft.game.world.World;
import net.minecraft.game.world.block.Block;

/**
 * Flint and steel: right-clicking the face of any block lights a fire on the
 * neighbouring cell — but only where the air is empty — and wears the tool.
 */
public final class ItemFlintAndSteel extends Item {
	public ItemFlintAndSteel(int itemID) {
		super(itemID);
		this.maxStackSize = 1;
		this.maxDamage = 64;
	}

	@Override
	public final boolean onItemUse(ItemStack stack, World world, int x, int y, int z, int side) {
		int[] target = neighbourAcrossFace(side, x, y, z);
		x = target[0];
		y = target[1];
		z = target[2];
		if (world.getBlockId(x, y, z) == 0) {
			world.playSoundEffect((double) x + 0.5D, (double) y + 0.5D, (double) z + 0.5D, "fire.ignite", 1.0F, itemRand.nextFloat() * 0.4F + 0.8F);
			world.setBlockWithNotify(x, y, z, Block.fire.blockID);
		}

		stack.damageItem(1);
		return true;
	}
}