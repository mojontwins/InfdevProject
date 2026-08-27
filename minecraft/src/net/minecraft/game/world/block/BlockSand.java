package net.minecraft.game.world.block;

import net.minecraft.game.entity.misc.EntityFallingSand;
import net.minecraft.game.world.World;
import net.minecraft.game.world.material.Material;

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

	public static boolean canFallBelow(World world, int x, int y, int z) {
		int blockID = world.getBlockId(x, y, z);
		if(blockID == 0) {
			return true;
		} else if(blockID == Block.fire.blockID) {
			return true;
		} else {
			Block block = Block.blocksList[blockID];
			Material material = block == null ? Material.air : block.blockMaterial;
			return material == Material.water ? true : material == Material.lava;
		}
	}
}