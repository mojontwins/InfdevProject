package net.minecraft.game.world.block;

import net.minecraft.game.entity.misc.EntityFallingSand;
import net.minecraft.game.world.World;
import net.minecraft.game.world.material.Material;

/**
 * Sand (and, via {@link BlockGravel}, gravel): a gravity block. When the cell
 * below is air, water, lava, fire or another substitutable block, the block
 * turns into an {@link EntityFallingSand} (or simply drops to the floor below
 * when physics are at their coarsest). See {@link Block#canBeSubstituted}.
 */
public class BlockSand extends Block {
	public static boolean fallInstantly = false;

	public BlockSand(int blockID, int textureIndex) {
		super(blockID, textureIndex, Material.sand);
	}

	@Override
	public void onBlockAdded(World world, int x, int y, int z) {
		this.tryToFall(world, x, y, z);
	}

	@Override
	public void onNeighborBlockChange(World world, int x, int y, int z, int neighborID) {
		this.tryToFall(world, x, y, z);
	}

	private void tryToFall(World world, int x, int y, int z) {
		if(canFallBelow(world, x, y - 1, z) && y >= 0) {
			int chunkRange = 32;
			if(!fallInstantly && world.checkChunksExist(x - chunkRange, y - chunkRange, z - chunkRange, x + chunkRange, y + chunkRange, z + chunkRange)) {
				world.spawnEntityInWorld(new EntityFallingSand(world, (double)x + 0.5D, (double)y + 0.5D, (double)z + 0.5D, this.blockID));
			} else {
				world.setBlockWithNotify(x, y, z, 0);
				while(canFallBelow(world, x, y - 1, z) && y > 0) {
					--y;
				}
				if(y > 0) {
					world.setBlockWithNotify(x, y, z, this.blockID);
				}
			}
		}
	}

	/**
	 * True when the block at (x, y, z) is air or otherwise can be replaced
	 * (fire, water, lava, flowers — see {@link Block#canBeSubstituted}). Sand
	 * falls through such cells.
	 */
	public static boolean canFallBelow(World world, int x, int y, int z) {
		Block block = world.getBlock(x, y, z);
		return block == null || block.canBeSubstituted();
	}
}