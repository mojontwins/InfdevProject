package net.minecraft.game.item;

import net.minecraft.game.entity.player.EntityPlayer;
import net.minecraft.game.entity.projectile.EntityArrow;
import net.minecraft.game.world.World;

/**
 * The bow: fires an arrow (spawned with a bit of downhill pitch) whenever the
 * player has an arrow in the inventory, which the shot consumes.
 */
public final class ItemBow extends Item {
	public ItemBow(int itemID) {
		super(itemID);
		this.maxStackSize = 1;
	}

	@Override
	public final ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
		if (player.inventory.consumeInventoryItem(Item.arrow.shiftedIndex)) {
			world.playSoundAtEntity(player, "random.bow", 1.0F, 1.0F / (itemRand.nextFloat() * 0.4F + 0.8F));
			world.spawnEntityInWorld(new EntityArrow(world, player));
		}

		return stack;
	}
}