package net.minecraft.game.item;

import net.minecraft.game.entity.player.EntityPlayer;
import net.minecraft.game.world.World;

/**
 * Edible items (apple, bread, pork, ...): a right click eats one unit, healing
 * the player by the item's fixed amount. Nothing checks hunger here — in this
 * build food just restores health directly.
 */
public class ItemFood extends Item {
	private final int healAmount;

	public ItemFood(int itemID, int healAmount) {
		super(itemID);
		this.healAmount = healAmount;
		this.maxStackSize = 1;
	}

	@Override
	public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
		--stack.stackSize;
		player.heal(this.healAmount);
		return stack;
	}
}