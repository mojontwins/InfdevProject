package net.minecraft.game.entity.monster;

import net.minecraft.game.item.Item;
import net.minecraft.game.world.World;

/**
 * The shambling undead: slow, tough and prone to burning in daylight.
 * Drops feathers, oddly enough.
 */
public class EntityZombie extends EntityMonster {
	public EntityZombie(World world) {
		super(world);
		this.texture = "/mob/zombie.png";
		this.moveSpeed = 0.5F;
		this.attackStrength = 5;
	}

	public final void onLivingUpdate() {
		this.tryBurnInDaylight();
		super.onLivingUpdate();
	}

	protected final String getLivingSound() {
		return "mob.zombie";
	}

	protected final String getHurtSound() {
		return "mob.zombiehurt";
	}

	protected final String getDeathSound() {
		return "mob.zombiedeath";
	}

	protected final int getDroppedItem() {
		return Item.feather.shiftedIndex;
	}
}