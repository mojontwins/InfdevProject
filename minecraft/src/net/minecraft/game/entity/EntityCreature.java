package net.minecraft.game.entity;

import net.minecraft.game.physics.Vec3D;
import net.minecraft.game.world.World;
import net.minecraft.game.world.path.PathEntity;
import util.MathHelper;

/**
 * A creature that can chase a target along a precomputed path or, left to its
 * own devices, wander toward the spot with the best "block weight". This is
 * the base of both the passive animals and the hostile monsters.
 */
public class EntityCreature extends EntityLiving {
	/** The current path toward the target; (re)computed whenever the target or goal changes. */
	private PathEntity pathToEntity;
	/** The entity this creature is actively trying to reach. */
	protected Entity playerToAttack;
	/** Set when {@link #attackEntity} actually dealt a blow; halts all movement for the rest of the tick. */
	protected boolean hasAttacked;

	public EntityCreature(World world) {
		super(world);
	}

	/** True when a straight ray from this creature's eye to the target's eye passes through no block. */
	protected final boolean canEntityBeSeen(Entity target) {
		return this.worldObj.rayTraceBlocks(new Vec3D(this.posX, this.posY + (double)this.getEyeHeight(), this.posZ), new Vec3D(target.posX, target.posY + (double)target.getEyeHeight(), target.posZ)) == null;
	}

	/**
	 * Steers the creature for one tick: pick up or drop the current target,
	 * attack it when it comes within reach, otherwise follow the path — or,
	 * with neither target nor path, fall through to the base wander behaviour.
	 */
	@Override
	protected void updateEntityActionState() {
		this.hasAttacked = false;
		if(this.playerToAttack == null) {
			this.playerToAttack = this.findEntityToAttack();
			if(this.playerToAttack != null) {
				this.pathToEntity = this.worldObj.pathFinder.createEntityPathTo(this, this.playerToAttack, 16.0F);
			}
		} else if(!this.playerToAttack.isEntityAlive()) {
			this.playerToAttack = null;
		} else {
			Entity target = this.playerToAttack;
			float deltaX = (float)(target.posX - this.posX);
			float deltaY = (float)(target.posY - this.posY);
			float deltaZ = (float)(target.posZ - this.posZ);
			float targetDistance = MathHelper.sqrt_float(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
			if(this.canEntityBeSeen(this.playerToAttack)) {
				this.attackEntity(this.playerToAttack, targetDistance);
			}
		}

		if(this.hasAttacked) {
			this.moveStrafing = 0.0F;
			this.moveForward = 0.0F;
			this.isJumping = false;
		} else {
			if(this.playerToAttack == null || this.pathToEntity != null && this.rand.nextInt(20) != 0) {
				// Idling (or already ticking a path): occasionally pick a fresh
				// wander goal — sample 200 random nearby spots, keep the best.
				if(this.pathToEntity == null || this.rand.nextInt(100) == 0) {
					float bestScore = -99999.0F;
					int bestX = -1;
					int bestY = -1;
					int bestZ = -1;

					for(int sample = 0; sample < 200; ++sample) {
						int sampleX = MathHelper.floor_double(this.posX + (double)this.rand.nextInt(21) - 10.0D);
						int sampleY = MathHelper.floor_double(this.posY + (double)this.rand.nextInt(9) - 4.0D);
						int sampleZ = MathHelper.floor_double(this.posZ + (double)this.rand.nextInt(21) - 10.0D);
						float score = this.getBlockPathWeight(sampleX, sampleY, sampleZ);
						if(score > bestScore) {
							bestScore = score;
							bestX = sampleX;
							bestY = sampleY;
							bestZ = sampleZ;
						}
					}

					if(bestX > 0) {
						this.pathToEntity = this.worldObj.pathFinder.createEntityPathToXYZ(this, bestX, bestY, bestZ, 16.0F);
					}
				}
			} else {
				// A live target without a valid path: trace a fresh one to it.
				this.pathToEntity = this.worldObj.pathFinder.createEntityPathTo(this, this.playerToAttack, 16.0F);
			}

			boolean inWater = this.handleWaterMovement();
			boolean inLava = this.handleLavaMovement();
			if(this.pathToEntity != null && this.rand.nextInt(100) != 0) {
				Vec3D pathNode = this.pathToEntity.getPosition(this);
				float reachDistance = this.width * 2.0F;

				// Walk along the path: skip straight past any node we are already
				// close enough to (unless it sits above us and we must climb).
				while(pathNode != null) {
					double nodeX = this.posX - pathNode.xCoord;
					double nodeY = this.posY - pathNode.yCoord;
					double nodeZ = this.posZ - pathNode.zCoord;
					if(nodeX * nodeX + nodeY * nodeY + nodeZ * nodeZ >= (double)(reachDistance * reachDistance) || pathNode.yCoord > this.posY) {
						break;
					}

					this.pathToEntity.incrementPathIndex();
					if(this.pathToEntity.isFinished()) {
						pathNode = null;
						this.pathToEntity = null;
					} else {
						pathNode = this.pathToEntity.getPosition(this);
					}
				}

				this.isJumping = false;
				if(pathNode != null) {
					double deltaX = pathNode.xCoord - this.posX;
					double deltaZ = pathNode.zCoord - this.posZ;
					double deltaY = pathNode.yCoord - this.posY;
					this.rotationYaw = (float)(Math.atan2(deltaZ, deltaX) * 180.0D / (double)((float)Math.PI)) - 90.0F;
					this.moveForward = this.moveSpeed;
					if(deltaY > 0.0D) {
						this.isJumping = true;
					}
				}

				// Flailing in water: creatures hop restlessly instead of sinking.
				if(this.rand.nextFloat() < 0.8F && (inWater || inLava)) {
					this.isJumping = true;
				}

			} else {
				super.updateEntityActionState();
				this.pathToEntity = null;
			}
		}
	}

	/**
	 * Gives the subclass the chance to land a hit when the target is within
	 * reach; {@code distance} is the centre-to-centre distance to the target.
	 */
	protected void attackEntity(Entity target, float distance) {
	}

	/**
	 * How attractive a block is as a walk/drop goal. Creatures prefer higher
	 * weights while wandering and require a non-negative one to spawn here.
	 */
	protected float getBlockPathWeight(int x, int y, int z) {
		return 0.0F;
	}

	protected Entity findEntityToAttack() {
		return null;
	}

	@Override
	public boolean getCanSpawnHere(float x, float y, float z) {
		return super.getCanSpawnHere(x, y, z) && this.getBlockPathWeight((int)x, (int)y, (int)z) >= 0.0F;
	}
}