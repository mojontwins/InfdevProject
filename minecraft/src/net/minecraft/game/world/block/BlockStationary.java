package net.minecraft.game.world.block;

import java.util.Random;
import net.minecraft.game.world.World;
import net.minecraft.game.world.material.Material;

public final class BlockStationary extends BlockFluid {
	protected BlockStationary(int blockID, Material material) {
		super(blockID, material);
		this.movingId = blockID - 1;
		this.stillId = blockID;
		this.setTickOnLoad(false);
	}

	@Override
	public final void updateTick(World world, int x, int y, int z, Random random) {
	}

	@Override
	public final void onNeighborBlockChange(World world, int x, int y, int z, int neighborID) {
		boolean shouldFlow = this.canFlow(world, x, y - 1, z);
		if(!shouldFlow) {
			shouldFlow = this.canFlow(world, x - 1, y, z);
		}
		if(!shouldFlow) {
			shouldFlow = this.canFlow(world, x + 1, y, z);
		}
		if(!shouldFlow) {
			shouldFlow = this.canFlow(world, x, y, z - 1);
		}
		if(!shouldFlow) {
			shouldFlow = this.canFlow(world, x, y, z + 1);
		}
		if(neighborID != 0) {
			Material neighborMaterial = Block.blocksList[neighborID].blockMaterial;
			if(this.blockMaterial == Material.water && neighborMaterial == Material.lava || neighborMaterial == Material.water && this.blockMaterial == Material.lava) {
				world.setBlockWithNotify(x, y, z, Block.stone.blockID);
				return;
			}
		}
		if(Block.fire.getChanceOfNeighborsEncouragingFire(neighborID)) {
			shouldFlow = true;
		}
		if(shouldFlow) {
			world.setTileNoUpdate(x, y, z, this.movingId);
			world.scheduleBlockUpdate(x, y, z, this.movingId);
		}
	}
}