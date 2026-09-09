package net.minecraft.game.entity.ai;

import net.minecraft.game.entity.EntityLiving;
import net.minecraft.game.entity.projectile.EntityArrow;
import net.minecraft.game.world.World;
import util.MathHelper;

/**
 * Ranged attack: tracks the attack target, closes to within firing range and
 * looses an arrow every {@code maxAttackTime} ticks.  The infdev archer (the
 * skeleton) lives at range, so this both aims the head and fires.
 */
public class EntityAIArrowAttack extends EntityAIBase {
	private final World worldObj;
	private final EntityLiving entityHost;
	private EntityLiving attackTarget;
	private int rangedAttackTime = 0;
	private final float moveSpeed;
	private int seeTime = 0;
	private final int rangedAttackID;
	private final int maxRangedAttackTime;

	public EntityAIArrowAttack(EntityLiving host, float moveSpeed, int rangedAttackID, int maxRangedAttackTime) {
		this.entityHost = host;
		this.worldObj = host.worldObj;
		this.moveSpeed = moveSpeed;
		this.rangedAttackID = rangedAttackID;
		this.maxRangedAttackTime = maxRangedAttackTime;
		this.setMutexBits(3);
	}

	@Override
	public boolean shouldExecute() {
		EntityLiving target = this.entityHost.getAttackTarget();
		if (target == null) {
			return false;
		}
		this.attackTarget = target;
		return true;
	}

	@Override
	public boolean continueExecuting() {
		return this.shouldExecute() || !this.entityHost.getNavigator().noPath();
	}

	@Override
	public void resetTask() {
		this.attackTarget = null;
		this.seeTime = 0;
		this.rangedAttackTime = 0;
	}

	@Override
	public void updateTask() {
		double d3 = this.entityHost.getDistanceSqToEntity(this.attackTarget);
		boolean canSee = this.entityHost.canEntityBeSeen(this.attackTarget);
		if (canSee) {
			++this.seeTime;
		} else {
			this.seeTime = 0;
		}

		if (d3 <= 100.0D && this.seeTime >= 20) {
			this.entityHost.getNavigator().clearPathEntity();
		} else {
			this.entityHost.getNavigator().tryMoveToEntityLiving(this.attackTarget, this.moveSpeed);
		}

		this.entityHost.getLookHelper().setLookPositionWithEntity(this.attackTarget, 30.0F, 30.0F);
		this.rangedAttackTime = Math.max(this.rangedAttackTime - 1, 0);
		if (this.rangedAttackTime <= 0 && d3 <= 100.0D && canSee) {
			this.doRangedAttack();
			this.rangedAttackTime = this.maxRangedAttackTime;
		}
	}

	private void doRangedAttack() {
		if (this.rangedAttackID == 1) {
			EntityArrow arrow = new EntityArrow(this.worldObj, this.entityHost);
			arrow.posY += (double)1.4F;
			double deltaX = this.attackTarget.posX - this.entityHost.posX;
			double deltaZ = this.attackTarget.posZ - this.entityHost.posZ;
			double deltaY = this.attackTarget.posY - (double)0.2F - arrow.posY;
			float drop = MathHelper.sqrt_double(deltaX * deltaX + deltaZ * deltaZ) * 0.2F;
			this.worldObj.playSoundAtEntity(this.entityHost, "random.bow", 1.0F, 1.0F / (this.entityHost.getRNG().nextFloat() * 0.4F + 0.8F));
			this.worldObj.spawnEntityInWorld(arrow);
			arrow.setArrowHeading(deltaX, deltaY + (double)drop, deltaZ, 0.6F, 12.0F);
		}
	}
}
