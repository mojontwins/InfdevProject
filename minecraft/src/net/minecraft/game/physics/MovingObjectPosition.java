package net.minecraft.game.physics;

import net.minecraft.game.entity.Entity;

/**
 * The result of a game-sight ray cast: either the block face that was pierced or the
 * entity that was hit. {@link AxisAlignedBB#calculateIntercept} and
 * {@link net.minecraft.game.world.block.Block#rayTrace} fill the block form, while
 * arrow collision fills the entity form; the client consumes it for the highlighted
 * block and for click targeting. The side codes are the standard block faces,
 * 0 = −Y, 1 = +Y, 2 = −Z, 3 = +Z, 4 = −X, 5 = +X.
 */
public final class MovingObjectPosition {

	/** Ray hit a block: {@link #blockX}, {@link #blockY}, {@link #blockZ} and {@link #sideHit} are set. */
	public static final int HIT_BLOCK = 0;

	/** Ray hit an entity: {@link #entityHit} is set. */
	public static final int HIT_ENTITY = 1;

	public int typeOfHit;
	public int blockX;
	public int blockY;
	public int blockZ;
	public int sideHit;
	public Vec3D hitVec;
	public Entity entityHit;

	/** The block form: the coordinates of the struck block, the face that was pierced and the exact hit point. */
	public MovingObjectPosition(int blockX, int blockY, int blockZ, int sideHit, Vec3D hitVector) {
		this.typeOfHit = HIT_BLOCK;
		this.blockX = blockX;
		this.blockY = blockY;
		this.blockZ = blockZ;
		this.sideHit = sideHit;
		this.hitVec = new Vec3D(hitVector.xCoord, hitVector.yCoord, hitVector.zCoord);
	}

	/** The entity form: the struck entity gives up its position as the hit point. */
	public MovingObjectPosition(Entity hitEntity) {
		this.typeOfHit = HIT_ENTITY;
		this.entityHit = hitEntity;
		this.hitVec = new Vec3D(hitEntity.posX, hitEntity.posY, hitEntity.posZ);
	}
}