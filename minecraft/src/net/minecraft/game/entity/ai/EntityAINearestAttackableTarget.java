package net.minecraft.game.entity.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.game.entity.Entity;
import net.minecraft.game.entity.EntityLiving;
import net.minecraft.game.entity.player.EntityPlayer;

/**
 * Scans for the closest living entity of a given class within the search
 * radius and sets it as the owner's attack target.
 */
public class EntityAINearestAttackableTarget extends EntityAITarget {
	private final Class<?> targetClass;
	private final int chanceDivisor;
	private EntityLiving nearestTarget;

	public EntityAINearestAttackableTarget(EntityLiving taskOwner, Class<?> targetClass, float searchRadius, int chanceDivisor, boolean checkSight) {
		super(taskOwner, searchRadius, checkSight);
		this.targetClass = targetClass;
		this.chanceDivisor = chanceDivisor;
		this.setMutexBits(1);
	}

	@Override
	public boolean shouldExecute() {
		if (this.chanceDivisor > 0 && this.taskOwner.getRNG().nextInt(this.chanceDivisor) != 0) {
			return false;
		}

		List<Entity> candidates = new ArrayList<>();
		if (this.targetClass == EntityPlayer.class) {
			Entity player = this.taskOwner.worldObj.getPlayerEntity();
			if (player instanceof EntityLiving
				&& this.taskOwner.getDistanceSqToEntity(player) <= (double)(this.getTargetSearchRadius() * this.getTargetSearchRadius())) {
				candidates.add(player);
			}
		} else {
			double searchDist = (double)this.getTargetSearchRadius();
			List<Entity> nearby = this.taskOwner.worldObj.getEntitiesWithinAABBExcludingEntity(null,
				this.taskOwner.boundingBox.expand(searchDist, searchDist, searchDist));
			for (Entity entity : nearby) {
				if (this.targetClass.isInstance(entity)) {
					candidates.add(entity);
				}
			}
		}

		if (candidates.isEmpty()) {
			return false;
		}

		Collections.sort(candidates, new Sorter(this.taskOwner));
		this.nearestTarget = (EntityLiving)candidates.get(0);
		return this.isValidTarget(this.nearestTarget, this.targetClass == EntityPlayer.class);
	}

	@Override
	public void startExecuting() {
		this.taskOwner.setAttackTarget(this.nearestTarget);
		super.startExecuting();
	}

	/** Sorts entities by distance to a reference entity. */
	private static class Sorter implements java.util.Comparator<Entity> {
		private final EntityLiving theOwner;

		Sorter(EntityLiving owner) {
			this.theOwner = owner;
		}

		public int compare(Entity a, Entity b) {
			double distA = this.theOwner.getDistanceSqToEntity(a);
			double distB = this.theOwner.getDistanceSqToEntity(b);
			return Double.compare(distA, distB);
		}
	}
}
