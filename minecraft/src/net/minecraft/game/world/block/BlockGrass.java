package net.minecraft.game.world.block;

import java.util.Random;
import net.minecraft.game.world.World;
import net.minecraft.game.world.material.Material;

/**
 * Grass block: spreads to adjacent dirt when well lit and dies back to dirt
 * when its top stays dark. The top/side textures are hard-coded sprite indices
 * (0 = grass top, 2 = dirt, 3 = grass side).
 */
public final class BlockGrass extends Block {
	protected BlockGrass(int blockID) {
		super(blockID, Material.ground);
		this.blockIndexInTexture = 3;
		this.setTickOnLoad(true);
	}

	@Override
	public final int getBlockTextureFromSide(int side) {
		return side == 1 ? 0 : (side == 0 ? 2 : 3);
	}

	@Override
	public final void updateTick(World world, int x, int y, int z, Random random) {
		if(world.getBlockLightValue(x, y + 1, z) < 4 && world.getBlockMaterial(x, y + 1, z).getCanBlockGrass()) {
			if(random.nextInt(4) == 0) {
				world.setBlockWithNotify(x, y, z, Block.dirt.blockID);
			}
		} else {
			if(world.getBlockLightValue(x, y + 1, z) >= 9) {
				int spreadX = x + random.nextInt(3) - 1;
				int spreadY = y + random.nextInt(5) - 3;
				int spreadZ = z + random.nextInt(3) - 1;
				if(world.getBlockId(spreadX, spreadY, spreadZ) == Block.dirt.blockID && world.getBlockLightValue(spreadX, spreadY + 1, spreadZ) >= 4 && !world.getBlockMaterial(spreadX, spreadY + 1, spreadZ).getCanBlockGrass()) {
					world.setBlockWithNotify(spreadX, spreadY, spreadZ, Block.grass.blockID);
				}
			}
		}
	}

	@Override
	public final int idDropped(int metadata, Random random) {
		return Block.dirt.idDropped(0, random);
	}

	/** Grass is a valid plant base (alongside dirt). */
	@Override
	public boolean canGrowPlants(int metadata) {
		return true;
	}
}