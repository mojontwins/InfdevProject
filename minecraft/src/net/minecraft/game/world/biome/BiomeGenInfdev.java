package net.minecraft.game.world.biome;

import java.util.Random;
import net.minecraft.game.world.World;
import net.minecraft.game.world.block.Block;
import net.minecraft.game.world.terrain.generate.WorldGenBigTree;
import net.minecraft.game.world.terrain.generate.WorldGenFlowers;
import net.minecraft.game.world.terrain.generate.WorldGenMinable;

/**
 * The only {@link BiomeGenerator} of this stage, and the one that embodies the
 * version's default terrain behaviour. Everything it does is byte-for-byte the
 * surface and decoration logic that used to live inline in
 * {@code ChunkProviderGenerate420.replaceBlocks} and {@code ...}.populate:
 *
 * <ul>
 *   <li>{@link #replaceBlocksForBiomeColumn} stamps the per-column surface
 *       (grass over dirt above sea level, sand beaches, gravel beds, bare stone
 *       or still water underwater), using the beach/gravel/dirt values the
 *       chunk provider computes and passes in.</li>
 *   <li>{@link #populateOres} drops the four ore passes (coal generously, iron
 *       a third as much, gold and diamond only on a chunk-local chance).</li>
 *   <li>{@link #decorate} places the tree line derived from the tree-count
 *       noise the provider passes in.</li>
 * </ul>
 *
 * <p>{@code topBlock} / {@code fillerBlock} keep the canonical grass-over-dirt
 * defaults, and {@link #getBiomeID} is 0 (the single registered biome id).
 */
public final class BiomeGenInfdev extends BiomeGenerator {
	/** Shared instance — the biome is stateless and there is only one of it. */
	public static final BiomeGenInfdev INSTANCE = new BiomeGenInfdev();

	/** The single biome id stored in chunk biome arrays. */
	private static final int BIOME_ID = 0;

	private BiomeGenInfdev() {
	}

	@Override
	public final int getBiomeID() {
		return BIOME_ID;
	}

	/**
	 * Replaces the surface of one column. This is the exact top-down walk that
	 * used to live in {@code ChunkProviderGenerate420.replaceBlocks}: it finds
	 * the first stone cell below the air, decides the surface/filler blocks
	 * from the passed-in depth/beach/gravel state, and buries {@code dirtDepth}
	 * filler blocks beneath the cap.
	 */
	@Override
	public final void replaceBlocksForBiomeColumn(
			World world, Random rand,
			int chunkX, int chunkZ, int x, int z,
			byte[] blocks, int seaLevel,
			boolean sandBeach, boolean gravelBed, int dirtDepth) {
		// Start the column walk at the top cell (x, z, 127) and step down one
		// cell at a time; each subsequent blockIndex is one cell lower.
		int blockIndex = x << 11 | z << 7 | 127;
		int dirtRemaining = -1;
		int surfaceBlock = this.topBlock();
		int groundBlock = this.fillerBlock();

		for(int y = 127; y >= 0; --y) {
			if(blocks[blockIndex] == 0) {
				dirtRemaining = -1;
			} else if(blocks[blockIndex] == Block.stone.blockID) {
				if(dirtRemaining == -1) {
					if(dirtDepth <= 0) {
						surfaceBlock = 0;
						groundBlock = Block.stone.blockID;
					} else if(y >= 60 && y <= 65) {
						surfaceBlock = this.topBlock();
						groundBlock = this.fillerBlock();
						if(gravelBed) {
							surfaceBlock = 0;
						}

						if(gravelBed) {
							groundBlock = Block.gravel.blockID;
						}

						if(sandBeach) {
							surfaceBlock = Block.sand.blockID;
						}

						if(sandBeach) {
							groundBlock = Block.sand.blockID;
						}
					}

					if(y < seaLevel && surfaceBlock == 0) {
						surfaceBlock = Block.waterStill.blockID;
					}

					dirtRemaining = dirtDepth;
					if(y >= 63) {
						blocks[blockIndex] = (byte)surfaceBlock;
					} else {
						blocks[blockIndex] = (byte)groundBlock;
					}
				} else if(dirtRemaining > 0) {
					--dirtRemaining;
					blocks[blockIndex] = (byte)groundBlock;
				}
			}

			--blockIndex;
		}
	}

	/**
	 * Decoration stage, ore portion: four ore passes (coal generously, iron a
	 * third as much, gold and diamond only when the chunk-local chance hits).
	 * The chunk provider has already re-seeded {@code rand} so the draw order
	 * here is reproducible per chunk.
	 *
	 * <p>Coal runs 20 times, iron 10 times, gold with 50% chance, diamond with
	 * 12.5% chance. All four call the inherited {@link BiomeGenerator#placeOreVein}
	 * with a positive amount (fixed-count) or negative amount (probabilistic).
	 */
	@Override
	public final void populateOres(World world, Random rand, int baseX, int baseZ) {
		WorldGenMinable coalVein = new WorldGenMinable(Block.oreCoal.blockID);
		WorldGenMinable ironVein = new WorldGenMinable(Block.oreIron.blockID);
		placeOreVein(world, rand, coalVein, 128, 20, baseX, baseZ);
		placeOreVein(world, rand, ironVein, 64, 10, baseX, baseZ);

		WorldGenMinable goldVein = new WorldGenMinable(Block.oreGold.blockID);
		WorldGenMinable diamondVein = new WorldGenMinable(Block.oreDiamond.blockID);
		placeOreVein(world, rand, goldVein, 32, -2, baseX, baseZ);
placeOreVein(world, rand, diamondVein, 16, -8, baseX, baseZ);
	}

	/**
	 * Decoration stage, non-ore portion: the tree line, then the flower and
	 * mushroom patches. {@code treeNoise} is the provider-computed tree-count
	 * noise for this chunk's origin, so the biome does not own the noise
	 * generator. The seed for {@code rand} was already fixed by the provider and
	 * the ore draw above has run, so the RNG draw order matches the original
	 * build exactly: tree count, tree placements, then the two yellow flower
	 * patches, the optional red flower, brown mushroom and red mushroom patches
	 * (see {@code decorate} body for the exact chances).
	 */
	@Override
	public final void decorate(World world, Random rand, int baseX, int baseZ, double treeNoise) {
		int treeCount = (int)(treeNoise - rand.nextDouble());
		if(treeCount < 0) {
			treeCount = 0;
		}

		WorldGenBigTree bigTree = new WorldGenBigTree();
		if(rand.nextInt(100) == 0) {
			++treeCount;
		}

		int count = treeCount;
		for(int i = 0; i < count; ++i) {
			int treeX = baseX + rand.nextInt(16) + 8;
			int treeZ = baseZ + rand.nextInt(16) + 8;
			bigTree.setScale(1.0D, 1.0D, 1.0D);
			bigTree.generate(world, rand, treeX, world.getHeightValue(treeX, treeZ), treeZ);
		}

		// Flowers and mushrooms, drawn in the same order and with the same
		// chances as in our 1.1.2 reference: two patches of yellow flowers, then
		// red flowers at 50 %, brown mushrooms at 25 % and red mushrooms at
		// 12.5 %. The variant lives in the block metadata instead of separate
		// block ids, so each patch places the consolidated block with the
		// matching metadata.
		for(int pass = 0; pass < 2; ++pass) {
			int flowerX = baseX + rand.nextInt(16) + 8;
			int flowerY = rand.nextInt(128);
			int flowerZ = baseZ + rand.nextInt(16) + 8;
			new WorldGenFlowers(Block.flowers.blockID, 1).generate(world, rand, flowerX, flowerY, flowerZ);
		}

		if(rand.nextInt(2) == 0) {
			int flowerX = baseX + rand.nextInt(16) + 8;
			int flowerY = rand.nextInt(128);
			int flowerZ = baseZ + rand.nextInt(16) + 8;
			new WorldGenFlowers(Block.flowers.blockID, 0).generate(world, rand, flowerX, flowerY, flowerZ);
		}

		if(rand.nextInt(4) == 0) {
			int mushroomX = baseX + rand.nextInt(16) + 8;
			int mushroomY = rand.nextInt(128);
			int mushroomZ = baseZ + rand.nextInt(16) + 8;
			new WorldGenFlowers(Block.mushrooms.blockID, 0).generate(world, rand, mushroomX, mushroomY, mushroomZ);
		}

		if(rand.nextInt(8) == 0) {
			int mushroomX = baseX + rand.nextInt(16) + 8;
			int mushroomY = rand.nextInt(128);
			int mushroomZ = baseZ + rand.nextInt(16) + 8;
			new WorldGenFlowers(Block.mushrooms.blockID, 1).generate(world, rand, mushroomX, mushroomY, mushroomZ);
		}
	}
}
