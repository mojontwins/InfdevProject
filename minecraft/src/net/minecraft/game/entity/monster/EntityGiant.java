package net.minecraft.game.entity.monster;

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
	}

	protected final float getBlockPathWeight(int x, int y, int z) {
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