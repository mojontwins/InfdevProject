package net.minecraft.game.world.block;

import net.minecraft.game.world.World;
import net.minecraft.game.world.material.Material;

public class BlockSand extends Block {
	public BlockSand(int blockID, int textureIndex) {
		super(blockID, textureIndex, Material.sand);
	}

	@Override
	public final void onBlockAdded(World world, int x, int y, int z) {
		this.fall(world, x, y, z);
	}

	@Override
	public final void onNeighborBlockChange(World world, int x, int y, int z, int neighborID) {
		this.fall(world, x, y, z);
	}

	private void fall(World world, int x, int y, int z) {
		int destinationY = y;
		while(true) {
			int scanY = destinationY - 1;
			int scannedBlockID = world.getBlockId(x, scanY, z);
			boolean canFall;
			if(scannedBlockID == 0) {
				canFall = true;
			} else if(scannedBlockID == Block.fire.blockID) {
				canFall = true;
			} else {
				Material scannedMaterial = Block.blocksList[scannedBlockID].blockMaterial;
				canFall = scannedMaterial == Material.water ? true : scannedMaterial == Material.lava;
			}
			if(!canFall || destinationY < 0) {
				if(destinationY < 0) {
					world.setTileNoUpdate(x, y, z, 0);
				}
				if(destinationY != y) {
					scannedBlockID = world.getBlockId(x, destinationY, z);
					if(scannedBlockID > 0 && Block.blocksList[scannedBlockID].blockMaterial != Material.air) {
						world.setTileNoUpdate(x, destinationY, z, 0);
					}
					world.swap(x, y, z, x, destinationY, z);
				}
				return;
			}
			--destinationY;
			if(world.getBlockId(x, destinationY, z) == Block.fire.blockID) {
				world.setTileNoUpdate(x, destinationY, z, 0);
			}
		}
	}
}