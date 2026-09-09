package net.minecraft.game.entity.ai;

import net.minecraft.game.entity.EntityLiving;
import net.minecraft.game.physics.Vec3D;
import net.minecraft.game.world.World;
import net.minecraft.game.world.path.PathEntity;
import util.MathHelper;

/**
 * Wraps the existing {@link net.minecraft.game.world.path.Pathfinder} to
 * provide the r1.2.5 navigation API.  Every mob owns one of these; the
 * entity's {@code moveForward}/{@code moveStrafing} fields are set from
 * the path-following loop in {@link #onUpdateNavigation()}.
 */
public class PathNavigate {
	private final EntityLiving theEntity;
	private final World worldObj;
	private PathEntity currentPath;
	private float speed;
	private float pathSearchRange = 16.0F;
	private boolean avoidsWater;
	private boolean canSwim;

	public PathNavigate(EntityLiving entity, World world) {
		this.theEntity = entity;
		this.worldObj = world;
	}

	public void setAvoidsWater(boolean avoids) {
		this.avoidsWater = avoids;
	}

	public boolean getAvoidsWater() {
		return this.avoidsWater;
	}

	public void setCanSwim(boolean can) {
		this.canSwim = can;
	}
	
	public boolean getCanSwim() {
		return this.canSwim;
	}

	/** Builds a path to a static world position. */
	public PathEntity getPathToXYZ(double x, double y, double z) {
		return this.worldObj.pathFinder.createEntityPathToXYZ(this.theEntity,
			MathHelper.floor_double(x), MathHelper.floor_double(y), MathHelper.floor_double(z), this.pathSearchRange);
	}

	/** Builds a path to a moving entity. */
	public PathEntity getPathToEntityLiving(EntityLiving target) {
		return this.worldObj.pathFinder.createEntityPathTo(this.theEntity, target, this.pathSearchRange);
	}

	/** Build and start following a path to the given position. */
	public boolean tryMoveToXYZ(double x, double y, double z, float speed) {
		PathEntity path = this.getPathToXYZ(x, y, z);
		return this.setPath(path, speed);
	}

	/** Build and start following a path to the given entity. */
	public boolean tryMoveToEntityLiving(EntityLiving target, float speed) {
		PathEntity path = this.getPathToEntityLiving(target);
		return this.setPath(path, speed);
	}

	/** Adopt a pre-built path.  Returns false on null. */
	public boolean setPath(PathEntity path, float speed) {
		if (path == null) {
			this.currentPath = null;
			return false;
		}
		this.currentPath = path;
		this.speed = speed;
		return true;
	}

	public PathEntity getPath() {
		return this.currentPath;
	}

	public boolean noPath() {
		return this.currentPath == null || this.currentPath.isFinished();
	}

	public void clearPathEntity() {
		this.currentPath = null;
	}

	/** Called every tick from {@code EntityLiving.onLivingUpdate()}. */
	public void onUpdateNavigation() {
		if (this.currentPath == null) {
			return;
		}
		this.followPath();
	}

	private void followPath() {
		if (this.currentPath == null) {
			return;
		}
		Vec3D nextNode = this.currentPath.getPosition(this.theEntity);
		if (nextNode == null) {
			this.currentPath = null;
			return;
		}

		double dx = nextNode.xCoord - this.theEntity.posX;
		double dy = nextNode.yCoord - this.theEntity.posY;
		double dz = nextNode.zCoord - this.theEntity.posZ;
		double distSq = dx * dx + dy * dy + dz * dz;
		double reachDist = (double)(this.theEntity.width * this.theEntity.width);

		// Skip nodes we're already close enough to.
		while (distSq < reachDist) {
			this.currentPath.incrementPathIndex();
			if (this.currentPath.isFinished()) {
				this.currentPath = null;
				return;
			}
			nextNode = this.currentPath.getPosition(this.theEntity);
			if (nextNode == null) {
				this.currentPath = null;
				return;
			}
			dx = nextNode.xCoord - this.theEntity.posX;
			dy = nextNode.yCoord - this.theEntity.posY;
			dz = nextNode.zCoord - this.theEntity.posZ;
			distSq = dx * dx + dy * dy + dz * dz;
		}

		// Face the next node and walk toward it.
		float yaw = (float)(Math.atan2(dz, dx) * 180.0D / Math.PI) - 90.0F;
		this.theEntity.rotationYaw = yaw;
		this.theEntity.moveForward = this.speed;

		if (dy > (double)this.theEntity.stepHeight) {
			this.theEntity.isJumping = true;
		}
	}
}
