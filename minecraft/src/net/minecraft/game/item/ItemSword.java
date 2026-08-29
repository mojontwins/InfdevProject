package net.minecraft.game.item;

import net.minecraft.game.world.block.Block;

/**
 * The sword: a dedicated melee weapon that wears a little on each hit and breaks
 * blocks slightly faster than bare hands, but is no digging tool.
 */
public final class ItemSword extends Item {
	private final int weaponDamage;

	public ItemSword(int itemID, int materialTier) {
		super(itemID);
		this.maxStackSize = 1;
		this.maxDamage = 32 << materialTier;
		this.weaponDamage = 4 + (materialTier << 1);
	}

	@Override
	public final float getStrVsBlock(Block block) {
		return 1.5F;
	}

	@Override
	public final void hitEntity(ItemStack stack) {
		stack.damageItem(1);
	}

	@Override
	public final void onBlockDestroyed(ItemStack stack) {
		stack.damageItem(2);
	}

	@Override
	public final int getDamageVsEntity() {
		return this.weaponDamage;
	}
}