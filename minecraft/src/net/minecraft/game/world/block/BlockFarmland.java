package net.minecraft.game.world.block;

import java.util.Random;
import net.minecraft.game.physics.AxisAlignedBB;
import net.minecraft.game.world.World;
import net.minecraft.game.world.material.Material;

/**
 * Tilled soil. Its metadata is the moisture level 0-7: nearby water keeps it
 * hydrated, walking or a solid block overhead reverts it to plain dirt, and a
 * parched plot with nothing planted also crumbles back to dirt.
 */
public final class BlockFarmland extends Block {
	protected BlockFarmland(int blockID) {
		super(blockID, Material.ground);
		this.blockIndexInTexture = 87;
		this.setTickOnLoad(true);
		this.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 15.0F / 16.0F, 1.0F);
		this.setLightOpacity(255);
	}

	@Override
	public final AxisAlignedBB getCollisionBoundingBoxFromPool(int x, int y, int z) {
		return new AxisAlignedBB((double)x, (double)y, (double)z, (double)(x + 1), (double)(y + 1), (double)(z + 1));
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
	public final int getBlockTextureFromSideAndMetadata(int side, int metadata) {
		return side == 1 && metadata > 0 ? this.blockIndexInTexture - 1 : (side == 1 ? this.blockIndexInTexture : 2);
	}

	@Override
	public final void updateTick(World world, int x, int y, int z, Random random) {
		if(random.nextInt(5) == 0) {
			if(this.isHydrated(world, x, y, z)) {
				world.setBlockMetadataWithNotify(x, y, z, 7);
				return;
			}
			int moisture = world.getBlockMetadata(x, y, z);
			if(moisture > 0) {
				world.setBlockMetadataWithNotify(x, y, z, moisture - 1);
				return;
			}
			if(world.getBlockId(x, y + 1, z) != Block.crops.blockID) {
				world.setBlockWithNotify(x, y, z, Block.dirt.blockID);
			}
		}
	}

	private boolean isHydrated(World world, int x, int y, int z) {
		for(int scanX = x - 4; scanX <= x + 4; ++scanX) {
			for(int scanY = y; scanY <= y + 1; ++scanY) {
				for(int scanZ = z - 4; scanZ <= z + 4; ++scanZ) {
					if(world.getBlockMaterial(scanX, scanY, scanZ) == Material.water) {
						return true;
					}
				}
			}
		}
		return false;
	}

	@Override
	public final void onEntityWalking(World world, int x, int y, int z) {
		if(world.rand.nextInt(4) == 0) {
			world.setBlockWithNotify(x, y, z, Block.dirt.blockID);
		}
	}

	@Override
	public final void onNeighborBlockChange(World world, int x, int y, int z, int neighborID) {
		super.onNeighborBlockChange(world, x, y, z, neighborID);
		Material aboveMaterial = world.getBlockMaterial(x, y + 1, z);
		if(aboveMaterial.isSolid()) {
			world.setBlockWithNotify(x, y, z, Block.dirt.blockID);
		}
	}

	@Override
	public final int idDropped(int metadata, Random random) {
		return Block.dirt.idDropped(0, random);
	}
}