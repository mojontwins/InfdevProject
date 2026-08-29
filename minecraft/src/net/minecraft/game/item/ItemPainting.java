package net.minecraft.game.item;

import net.minecraft.game.entity.EntityPainting;
import net.minecraft.game.world.World;

/**
 * The painting item: on use it builds an {@link EntityPainting} on the clicked
 * wall face (only horizontal sides are walls for this purpose), and only spends
 * the item if the painting actually fits and hangs.
 */
public final class ItemPainting extends Item {
	public ItemPainting(int itemID) {
		super(itemID);
		this.maxDamage = 64;
	}

	@Override
	public final boolean onItemUse(ItemStack stack, World world, int x, int y, int z, int side) {
		if (side == 0 || side == 1) {
			return false;
		} else {
			// Convert the clicked block face into the painting's facing direction
			// (0 = -Z, 1 = -X, 2 = +Z, 3 = +X); side 2 (-Z) is the natural default.
			int direction = 0;
			switch (side) {
				case 4:
					direction = 1;
					break;
				case 3:
					direction = 2;
					break;
				case 5:
					direction = 3;
			}

			EntityPainting painting = new EntityPainting(world, x, y, z, direction);
			if (painting.onValidSurface()) {
				world.spawnEntityInWorld(painting);
				--stack.stackSize;
			}

			return true;
		}
	}
}