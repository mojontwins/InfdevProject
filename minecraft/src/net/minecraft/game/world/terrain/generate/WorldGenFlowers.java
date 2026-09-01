package net.minecraft.game.world.terrain.generate;

import java.util.Random;
import net.minecraft.game.world.World;
import net.minecraft.game.world.block.Block;
import net.minecraft.game.world.block.BlockFlower;

/**
 * Scatters a short plant (flower or mushroom) across a chunk. Picks up to 64
 * random cells in a 16-block box and, wherever the cell is currently air and
 * the plant can stay there, places it. Carries a metadata so that blocks that
 * use metadata to distinguish variants (e.g. flower red/yellow, mushroom
 * red/brown) can pick the right sub-type — this is how our version differs from
 * upstream, which used one block id per variant.
 */
public final class WorldGenFlowers extends WorldGenerator {
	private final int plantBlockId;
	private final int plantMetadata;

	public WorldGenFlowers(int plantBlockId) {
		this(plantBlockId, 0);
	}

	public WorldGenFlowers(int plantBlockId, int plantMetadata) {
		this.plantBlockId = plantBlockId;
		this.plantMetadata = plantMetadata;
	}

	@Override
	public final boolean generate(World world, Random random, int x, int y, int z) {
		for(int i = 0; i < 64; ++i) {
			int px = x + random.nextInt(8) - random.nextInt(8);
			int py = y + random.nextInt(4) - random.nextInt(4);
			int pz = z + random.nextInt(8) - random.nextInt(8);
			if(world.getBlockId(px, py, pz) == 0 && ((BlockFlower)Block.blocksList[this.plantBlockId]).canBlockStay(world, px, py, pz)) {
				world.setBlockAndMetadata(px, py, pz, this.plantBlockId, this.plantMetadata);
			}
		}

		return true;
	}
}
