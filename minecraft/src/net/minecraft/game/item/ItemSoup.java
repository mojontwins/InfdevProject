package net.minecraft.game.item;

import net.minecraft.game.entity.player.EntityPlayer;
import net.minecraft.game.world.World;

/**
 * Mushroom soup: it heals like any food, but handing back the empty bowl is its
 * point — the bowl is the reusable part, so the soup stack is consumed and
 * replaced by a bowl item in the hand.
 */
public final class ItemSoup extends ItemFood {
	public ItemSoup(int itemID, int healAmount) {
		super(itemID, healAmount);
	}

	@Override
	public final ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
		super.onItemRightClick(stack, world, player);
		return new ItemStack(Item.bowlEmpty);
	}
}