package net.minecraft.game.entity.monster;

import net.minecraft.game.entity.ai.EntityAIAttackOnCollide;
import net.minecraft.game.entity.ai.EntityAIHurtByTarget;
import net.minecraft.game.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.game.entity.ai.EntityAISwimming;
import net.minecraft.game.entity.ai.EntityAIWander;
import net.minecraft.game.entity.ai.EntityAIWatchClosest;
import net.minecraft.game.entity.ai.EntityAILookIdle;
import net.minecraft.game.entity.player.EntityPlayer;
import net.minecraft.game.world.World;

/**
 * A zombie scaled up sixfold — a debug-era monstrosity with a huge hit pool
 * and a near-fatal swipe. Prefers bright spots to dark ones, so it never
 * passes the standard monster spawn check.
 */
public class EntityGiant extends EntityMonster {
	public EntityGiant(World world) {
		super(world);
		this.texture = "/mob/zombie.png";
		this.moveSpeed = 0.5F;
		this.attackStrength = 50;
		this.health *= 10;
		this.yOffset *= 6.0F;
		this.setSize(this.width * 6.0F, this.height * 6.0F);

		this.tasks.addTask(0, new EntityAISwimming(this));
		this.tasks.addTask(1, new EntityAIAttackOnCollide(this, EntityPlayer.class, this.moveSpeed, false));
		this.tasks.addTask(2, new EntityAIWander(this, this.moveSpeed));
		this.tasks.addTask(3, new EntityAIWatchClosest(this, EntityPlayer.class, 8.0F));
		this.tasks.addTask(4, new EntityAILookIdle(this));
		this.targetTasks.addTask(1, new EntityAIHurtByTarget(this, false));
		this.targetTasks.addTask(2, new EntityAINearestAttackableTarget(this, EntityPlayer.class, 16.0F, 0, true));
	}

	@Override
	public final float getBlockPathWeight(int x, int y, int z) {
		return this.worldObj.getBrightness(x, y, z) - 0.5F;
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
}
