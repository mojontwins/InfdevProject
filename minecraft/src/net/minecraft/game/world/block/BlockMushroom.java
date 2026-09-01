package net.minecraft.game.world.block;

import java.util.Random;
import net.minecraft.game.world.World;

/**
 * A mushroom: instead of the flower's grass/dirt support rule it will grow on
 * any opaque block, but only while it is dimly lit (light level 12 is the
 * cutoff). The metadata carries the mushroom variant (0 = brown tile 29,
 * 1 = red tile 28); spreading preserves it. The {@link #metadataToTexture}
 * array makes the mapping easy to extend with more variants later.
 */
public final class BlockMushroom extends BlockFlower {
	protected BlockMushroom(int blockID, int textureIndex) {
		super(blockID, textureIndex);
		this.setBlockBounds(0.3F, 0.0F, 0.3F, 0.7F, 0.4F, 0.7F);
		this.metadataToTexture = new int[]{29, 28};
	}

	@Override
	protected final boolean canThisPlantGrowOnThisBlockID(int belowBlockID) {
		return Block.opaqueCubeLookup[belowBlockID];
	}

	@Override
	public final boolean canBlockStay(World world, int x, int y, int z) {
		if(world.getBlockLightValue(x, y, z) <= 12) {
			int belowBlockID = world.getBlockId(x, y - 1, z);
			if(Block.opaqueCubeLookup[belowBlockID]) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Mushroom tick: the base class runs the survival check (so the mushroom
	 * dies if it loses its dim light or its support); then, on a 1 % chance,
	 * the mushroom spreads to a random neighbour air cell that passes
	 * {@link #canBlockStay}. The new mushroom inherits the original's
	 * metadata, so a red mushroom produces more red mushrooms and a brown
	 * mushroom produces more brown mushrooms.
	 */
	@Override
	public final void updateTick(World world, int x, int y, int z, Random rand) {
		super.updateTick(world, x, y, z, rand);
		if(rand.nextInt(100) != 0) {
			return;
		}
		int nx = x + rand.nextInt(3) - rand.nextInt(3);
		int ny = y + rand.nextInt(2) - rand.nextInt(2);
		int nz = z + rand.nextInt(3) - rand.nextInt(3);
		int currentMetadata = world.getBlockMetadata(x, y, z);
		if(world.getBlockId(nx, ny, nz) == 0 && this.canBlockStay(world, nx, ny, nz)) {
			world.setBlockAndMetadataWithNotify(nx, ny, nz, this.blockID, currentMetadata);
		}
	}
}
