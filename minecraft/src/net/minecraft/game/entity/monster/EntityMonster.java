package net.minecraft.game.entity.monster;

import net.minecraft.game.entity.Entity;
import net.minecraft.game.entity.EntityCreature;
import net.minecraft.game.world.World;
import util.MathHelper;

/**
 * A hostile creature: chases the player up to 16 m away, strikes on contact
 * and only spawns in dark places. Zombies and skeletons catch fire at dawn
 * (see {@link #tryBurnInDaylight()}).
 */
public class EntityMonster extends EntityCreature {
	protected int attackStrength = 2;

	public EntityMonster(World world) {
		super(world);
		this.health = 20;
	}

	/** Daylight counts double against a monster's despawn timer. */
	public void onLivingUpdate() {
		float brightness = this.getEntityBrightness(1.0F);
		if(brightness > 0.5F) {
			this.entityAge += 2;
		}

		super.onLivingUpdate();
	}

	/** Peaceful difficulty wipes every monster out each tick. */
	public final void onUpdate() {
		super.onUpdate();
		if(this.worldObj.difficultySetting == 0) {
			this.isDead = true;
		}

	}

	protected Entity findPlayerToAttack() {
		Entity potentialTarget = this.worldObj.playerEntity;
		double distanceSq = potentialTarget.getDistanceSqToEntity(this);
		if(distanceSq < 256.0D && this.canEntityBeSeen(potentialTarget)) {
			// A sneaking player in a dim spot (light < 7) is invisible to the
			// monster's eye beyond six blocks — only detected up close.
			if(potentialTarget.isSneaking() && distanceSq > 36.0D && this.worldObj.getBlockLightValue(MathHelper.floor_double(potentialTarget.posX), MathHelper.floor_double(potentialTarget.posY), MathHelper.floor_double(potentialTarget.posZ)) < 7) {
				return null;
			}

			return potentialTarget;
		} else {
			return null;
		}
	}

	/** A monster that is struck turns on whoever dealt the blow. */
	public final boolean attackEntityFrom(Entity attacker, int damage) {
		if(super.attackEntityFrom(attacker, damage)) {
			if(attacker != this) {
				this.playerToAttack = attacker;
			}

			return true;
		} else {
			return false;
		}
	}

	/** Plain contact bite: close enough and vertically overlapping the target's body. */
	protected void attackEntity(Entity target, float distance) {
		if((double)distance < 2.5D && target.boundingBox.maxY > this.boundingBox.minY && target.boundingBox.minY < this.boundingBox.maxY) {
			this.attackTime = 20;
			target.attackEntityFrom(this, this.attackStrength);
		}

	}

	/** Monsters avoid bright, open spots when picking a wander goal. */
	protected float getBlockPathWeight(int x, int y, int z) {
		return 0.5F - this.worldObj.getBrightness(x, y, z);
	}

	/**
	 * Zombies and skeletons burn up on a sunny day when standing in the open;
	 * the brighter the sun, the more likely the fire catches.
	 */
	protected final void tryBurnInDaylight() {
		if(this.worldObj.isDaytime()) {
			float brightness = this.getEntityBrightness(1.0F);
			if(brightness > 0.5F && this.worldObj.canBlockSeeTheSky(MathHelper.floor_double(this.posX), MathHelper.floor_double(this.posY), MathHelper.floor_double(this.posZ)) && this.rand.nextFloat() * 30.0F < (brightness - 0.4F) * 2.0F) {
				this.fire = 300;
			}
		}

	}

	@Override
	public final boolean getCanSpawnHere(float x, float y, float z) {
		int brightness = this.worldObj.getBlockLightValue(MathHelper.floor_float(x), MathHelper.floor_float(y), MathHelper.floor_float(z));
		return brightness <= this.rand.nextInt(8) && super.getCanSpawnHere(x, y, z);
	}
}