package net.minecraft.game.entity.monster;

import net.minecraft.game.entity.ai.EntityAIAttackOnCollide;
import net.minecraft.game.entity.ai.EntityAIHurtByTarget;
import net.minecraft.game.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.game.entity.ai.EntityAISwimming;
import net.minecraft.game.entity.ai.EntityAIWander;
import net.minecraft.game.entity.ai.EntityAIWatchClosest;
import net.minecraft.game.entity.ai.EntityAILookIdle;
import net.minecraft.game.entity.player.EntityPlayer;
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

		this.tasks.addTask(0, new EntityAISwimming(this));
		this.tasks.addTask(1, new EntityAIAttackOnCollide(this, EntityPlayer.class, this.moveSpeed, false));
		this.tasks.addTask(2, new EntityAIWander(this, this.moveSpeed));
		this.tasks.addTask(3, new EntityAIWatchClosest(this, EntityPlayer.class, 8.0F));
		this.tasks.addTask(4, new EntityAILookIdle(this));
		this.targetTasks.addTask(1, new EntityAIHurtByTarget(this, false));
		this.targetTasks.addTask(2, new EntityAINearestAttackableTarget(this, EntityPlayer.class, 16.0F, 0, true));
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
