package net.minecraft.game.world.block;

import java.util.Random;
import net.minecraft.game.physics.AxisAlignedBB;
import net.minecraft.game.physics.MovingObjectPosition;
import net.minecraft.game.physics.Vec3D;
import net.minecraft.game.world.World;
import net.minecraft.game.world.material.Material;

/**
 * Wall/floor torch. Its metadata records which face it sticks to (1 = -X, 2 =
 * +X, 3 = -Z, 4 = +Z, 5 = floor); when the support block disappears the torch
 * drops. The ray-trace box and the smoke/flame particles are shifted to match
 * that orientation.
 */
public final class BlockTorch extends Block {
	protected BlockTorch(int blockID, int textureIndex) {
		super(blockID, textureIndex, Material.circuits);
		this.setTickOnLoad(true);
	}

	@Override
	public final AxisAlignedBB getCollisionBoundingBoxFromPool(int x, int y, int z) {
		return null;
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
		return 2;
	}

	@Override
	public final boolean canPlaceBlockAt(World world, int x, int y, int z) {
		return world.isSolid(x - 1, y, z) || world.isSolid(x + 1, y, z) || world.isSolid(x, y, z - 1) || world.isSolid(x, y, z + 1) || world.isSolid(x, y - 1, z);
	}

	@Override
	public final void onBlockPlaced(World world, int x, int y, int z, int side) {
		int metadata = world.getBlockMetadata(x, y, z);
		if(side == 1 && world.isSolid(x, y - 1, z)) {
			metadata = 5;
		}
		if(side == 2 && world.isSolid(x, y, z + 1)) {
			metadata = 4;
		}
		if(side == 3 && world.isSolid(x, y, z - 1)) {
			metadata = 3;
		}
		if(side == 4 && world.isSolid(x + 1, y, z)) {
			metadata = 2;
		}
		if(side == 5 && world.isSolid(x - 1, y, z)) {
			metadata = 1;
		}
		world.setBlockMetadataWithNotify(x, y, z, metadata);
	}

	@Override
	public final void updateTick(World world, int x, int y, int z, Random random) {
		if(world.getBlockMetadata(x, y, z) == 0) {
			this.onBlockAdded(world, x, y, z);
		}
	}

	@Override
	public final void onBlockAdded(World world, int x, int y, int z) {
		if(world.isSolid(x - 1, y, z)) {
			world.setBlockMetadataWithNotify(x, y, z, 1);
		} else if(world.isSolid(x + 1, y, z)) {
			world.setBlockMetadataWithNotify(x, y, z, 2);
		} else if(world.isSolid(x, y, z - 1)) {
			world.setBlockMetadataWithNotify(x, y, z, 3);
		} else if(world.isSolid(x, y, z + 1)) {
			world.setBlockMetadataWithNotify(x, y, z, 4);
		} else if(world.isSolid(x, y - 1, z)) {
			world.setBlockMetadataWithNotify(x, y, z, 5);
		}
		this.dropTorchIfCantStay(world, x, y, z);
	}

	@Override
	public final void onNeighborBlockChange(World world, int x, int y, int z, int neighborID) {
		if(this.dropTorchIfCantStay(world, x, y, z)) {
			int metadata = world.getBlockMetadata(x, y, z);
			boolean unsupported = false;
			if(!world.isSolid(x - 1, y, z) && metadata == 1) {
				unsupported = true;
			}
			if(!world.isSolid(x + 1, y, z) && metadata == 2) {
				unsupported = true;
			}
			if(!world.isSolid(x, y, z - 1) && metadata == 3) {
				unsupported = true;
			}
			if(!world.isSolid(x, y, z + 1) && metadata == 4) {
				unsupported = true;
			}
			if(!world.isSolid(x, y - 1, z) && metadata == 5) {
				unsupported = true;
			}
			if(unsupported) {
				this.dropBlockAsItem(world, x, y, z, world.getBlockMetadata(x, y, z));
				world.setBlockWithNotify(x, y, z, 0);
			}
		}
	}

	private boolean dropTorchIfCantStay(World world, int x, int y, int z) {
		if(!this.canPlaceBlockAt(world, x, y, z)) {
			this.dropBlockAsItem(world, x, y, z, world.getBlockMetadata(x, y, z));
			world.setBlockWithNotify(x, y, z, 0);
			return false;
		}
		return true;
	}

	@Override
	public final MovingObjectPosition collisionRayTrace(World world, int x, int y, int z, Vec3D startVector, Vec3D endVector) {
		int metadata = world.getBlockMetadata(x, y, z);
		if(metadata == 1) {
			this.setBlockBounds(0.0F, 0.2F, 0.35F, 0.3F, 0.8F, 0.65F);
		} else if(metadata == 2) {
			this.setBlockBounds(0.7F, 0.2F, 0.35F, 1.0F, 0.8F, 0.65F);
		} else if(metadata == 3) {
			this.setBlockBounds(0.35F, 0.2F, 0.0F, 0.65F, 0.8F, 0.3F);
		} else if(metadata == 4) {
			this.setBlockBounds(0.35F, 0.2F, 0.7F, 0.65F, 0.8F, 1.0F);
		} else {
			this.setBlockBounds(0.4F, 0.0F, 0.4F, 0.6F, 0.6F, 0.6F);
		}
		return super.collisionRayTrace(world, x, y, z, startVector, endVector);
	}

	@Override
	public final void randomDisplayTick(World world, int x, int y, int z, Random random) {
		int metadata = world.getBlockMetadata(x, y, z);
		float centerX = (float)x + 0.5F;
		float centerY = (float)y + 0.7F;
		float centerZ = (float)z + 0.5F;
		if(metadata == 1) {
			world.spawnParticle("smoke", (double)(centerX - 0.27F), (double)(centerY + 0.22F), (double)centerZ, 0.0D, 0.0D, 0.0D);
			world.spawnParticle("flame", (double)(centerX - 0.27F), (double)(centerY + 0.22F), (double)centerZ, 0.0D, 0.0D, 0.0D);
		} else if(metadata == 2) {
			world.spawnParticle("smoke", (double)(centerX + 0.27F), (double)(centerY + 0.22F), (double)centerZ, 0.0D, 0.0D, 0.0D);
			world.spawnParticle("flame", (double)(centerX + 0.27F), (double)(centerY + 0.22F), (double)centerZ, 0.0D, 0.0D, 0.0D);
		} else if(metadata == 3) {
			world.spawnParticle("smoke", (double)centerX, (double)(centerY + 0.22F), (double)(centerZ - 0.27F), 0.0D, 0.0D, 0.0D);
			world.spawnParticle("flame", (double)centerX, (double)(centerY + 0.22F), (double)(centerZ - 0.27F), 0.0D, 0.0D, 0.0D);
		} else if(metadata == 4) {
			world.spawnParticle("smoke", (double)centerX, (double)(centerY + 0.22F), (double)(centerZ + 0.27F), 0.0D, 0.0D, 0.0D);
			world.spawnParticle("flame", (double)centerX, (double)(centerY + 0.22F), (double)(centerZ + 0.27F), 0.0D, 0.0D, 0.0D);
		} else {
			world.spawnParticle("smoke", (double)centerX, (double)centerY, (double)centerZ, 0.0D, 0.0D, 0.0D);
			world.spawnParticle("flame", (double)centerX, (double)centerY, (double)centerZ, 0.0D, 0.0D, 0.0D);
		}
	}
}