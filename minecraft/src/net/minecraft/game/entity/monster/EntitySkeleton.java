package net.minecraft.game.entity.monster;

import net.minecraft.game.entity.ai.EntityAIArrowAttack;
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
 * The undead archer: tracks the target up to 10 m away and looses a
 * fully-aimed arrow every 30 ticks. Burns up in the daylight.
 */
public class EntitySkeleton extends EntityMonster {
	public EntitySkeleton(World world) {
		super(world);
		this.texture = "/mob/skeleton.png";

		this.tasks.addTask(0, new EntityAISwimming(this));
		this.tasks.addTask(1, new EntityAIArrowAttack(this, this.moveSpeed, 1, 30));
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
		return "mob.skeleton";
	}

	protected final String getHurtSound() {
		return "mob.skeletonhurt";
	}

	protected final String getDeathSound() {
		return "mob.skeletonhurt";
	}

	protected final int getDroppedItem() {
		return Item.arrow.shiftedIndex;
	}
}
