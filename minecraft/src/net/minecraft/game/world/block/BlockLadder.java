package net.minecraft.game.world.block;

import java.util.Random;
import net.minecraft.game.physics.AxisAlignedBB;
import net.minecraft.game.physics.MovingObjectPosition;
import net.minecraft.game.physics.Vec3D;
import net.minecraft.game.world.World;
import net.minecraft.game.world.material.Material;

/**
 * A wall-mounted ladder that entities can climb. The metadata encodes which
 * wall face the ladder is attached to: 2 = south (+Z), 3 = north (-Z),
 * 4 = east (+X), 5 = west (-X). The block is a thin panel (2/16 of a
 * block thick) pressed against the supporting wall.
 *
 * <p>Derived from the Minecraft Alpha 1.1.2 {@code BlockLadder}, modernised
 * to Java 8 and adapted to this codebase's naming conventions.
 */
public final class BlockLadder extends Block {
	protected BlockLadder(int blockID, int textureIndex) {
		super(blockID, textureIndex, Material.circuits);
	}

	@Override
	public final AxisAlignedBB getCollisionBoundingBoxFromPool(int x, int y, int z) {
		// Ladders are climbable but have no collision box — entities pass through
		return null;
	}

	@Override
	public final MovingObjectPosition collisionRayTrace(World world, int x, int y, int z, Vec3D startVector, Vec3D endVector) {
		this.setBoundsFromMetadata(world.getBlockMetadata(x, y, z));
		return super.collisionRayTrace(world, x, y, z, startVector, endVector);
	}

	/**
	 * Sets the block bounds to a thin panel against the wall indicated by
	 * the given metadata. The panel is 2/16 of a block thick.
	 */
	private void setBoundsFromMetadata(int metadata) {
		float thickness = 2.0F / 16.0F;
		this.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
		if(metadata == 2) {
			this.setBlockBounds(0.0F, 0.0F, 1.0F - thickness, 1.0F, 1.0F, 1.0F);
		} else if(metadata == 3) {
			this.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, thickness);
		} else if(metadata == 4) {
			this.setBlockBounds(1.0F - thickness, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
		} else if(metadata == 5) {
			this.setBlockBounds(0.0F, 0.0F, 0.0F, thickness, 1.0F, 1.0F);
		}
	}

	@Override
	public final boolean isOpaqueCube() {
		return false;
	}

	@Override
	public final boolean renderAsNormalBlock() {
		return false;
	}

	@Override
	public final int getRenderType() {
		return 8;
	}

	@Override
	public final boolean canPlaceBlockAt(World world, int x, int y, int z) {
		return world.isSolid(x - 1, y, z) || world.isSolid(x + 1, y, z)
			|| world.isSolid(x, y, z - 1) || world.isSolid(x, y, z + 1);
	}

	@Override
	public final void onBlockPlaced(World world, int x, int y, int z, int side) {
		int metadata = world.getBlockMetadata(x, y, z);
		if((metadata == 0 || side == 2) && world.isSolid(x, y, z + 1)) {
			metadata = 2;
		}
		if((metadata == 0 || side == 3) && world.isSolid(x, y, z - 1)) {
			metadata = 3;
		}
		if((metadata == 0 || side == 4) && world.isSolid(x + 1, y, z)) {
			metadata = 4;
		}
		if((metadata == 0 || side == 5) && world.isSolid(x - 1, y, z)) {
			metadata = 5;
		}
		world.setBlockMetadataWithNotify(x, y, z, metadata);
	}

	@Override
	public final void onNeighborBlockChange(World world, int x, int y, int z, int neighborID) {
		int metadata = world.getBlockMetadata(x, y, z);
		boolean supported = false;
		if(metadata == 2 && world.isSolid(x, y, z + 1)) {
			supported = true;
		}
		if(metadata == 3 && world.isSolid(x, y, z - 1)) {
			supported = true;
		}
		if(metadata == 4 && world.isSolid(x + 1, y, z)) {
			supported = true;
		}
		if(metadata == 5 && world.isSolid(x - 1, y, z)) {
			supported = true;
		}
		if(!supported) {
			this.dropBlockAsItem(world, x, y, z, metadata);
			world.setBlockWithNotify(x, y, z, 0);
		}
		super.onNeighborBlockChange(world, x, y, z, neighborID);
	}

	@Override
	public final boolean isClimbable() {
		return true;
	}

	@Override
	public final int quantityDropped(Random random) {
		return 1;
	}

	@Override
	public final int idDropped(int metadata, Random random) {
		return this.blockID;
	}
}
