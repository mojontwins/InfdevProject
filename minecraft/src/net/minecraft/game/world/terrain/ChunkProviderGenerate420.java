package net.minecraft.game.world.terrain;

import java.util.stream.IntStream;
import net.minecraft.game.world.World;
import net.minecraft.game.world.WorldOptions;
import net.minecraft.game.world.block.Block;
import net.minecraft.game.world.chunk.IChunkProvider;
import net.minecraft.game.world.terrain.generate.WorldGenBigTree;
import net.minecraft.game.world.terrain.generate.WorldGenMinable;
import net.minecraft.game.world.terrain.noise.NoiseGeneratorOctaves;

/**
 * The concrete overworld generator for this version's world format (the
 * "inf-20100420" generation). It implements the abstract pipeline of
 * {@link ChunkProviderGenerate}:
 *
 * <ul>
 *   <li>{@link #initializeNoiseField} samples three octave banks - a fine
 *       blend field and two wide low-frequency fields - and blends them into
 *       the coarse 5x5x17 density field that encodes the land's shape (a
 *       signed scalar: positive ⇒ stone, negative ⇒ water-or-air by y).</li>
 *   <li>{@link #generateTerrain} tri-linearly up-samples that field to the
 *       full 16x16x128 block array, stamping stone above the surface and still
 *       water below {@link #SEA_LEVEL}.</li>
 *   <li>{@link #replaceBlocks} runs the per-column surface pass, carving
 *       grass, dirt, sand and gravel according to the beach/bed noise.</li>
 *   <li>{@link #populate} drops the ore veins and trees; the chunk pipeline
 *       calls it once the chunk's neighbourhood is generated.</li>
 * </ul>
 *
 * <p>The construction order of the noise generators is load-bearing: each
 * {@link NoiseGeneratorOctaves} draws from the shared {@link Random} when it
 * is built, so every later generator's seed depends on how much randomness the
 * stream has already consumed. The throw-away generator in the constructor
 * (whose output is never stored) advances the stream exactly like the original
 * build and must not be removed.
 */
public final class ChunkProviderGenerate420 extends ChunkProviderGenerate {
	private static final int GRID_WIDTH = 5;
	private static final int GRID_HEIGHT = 17;
	private static final int GRID_DEPTH = 5;
	private static final int NOISE_ARRAY_SIZE = GRID_WIDTH * GRID_HEIGHT * GRID_DEPTH; // 425
	private static final int SEA_LEVEL = 64;

	private NoiseGeneratorOctaves noiseGen1;
	private NoiseGeneratorOctaves noiseGen2;
	private NoiseGeneratorOctaves noiseGen3;
	private NoiseGeneratorOctaves noiseGen4;
	private NoiseGeneratorOctaves noiseGen5;
	private NoiseGeneratorOctaves mobSpawnerNoise;
	/** The coarse 3-D density field for the chunk currently being generated. */
	private double[] noiseArray;
	private double[] noise3;
	private double[] noise1;
	private double[] noise2;

	/**
	 * The order of construction is intentional (see the class comment): the six
	 * generators must consume {@link #rand} in the exact sequence the original
	 * build used or every seed-dependent generator changes. The world options
	 * ride along to the base class, where future generation passes can read them.
	 */
	public ChunkProviderGenerate420(World world, long seed, WorldOptions worldOptions) {
		super(world, seed, worldOptions);
		this.noiseGen1 = new NoiseGeneratorOctaves(this.rand, 16);
		this.noiseGen2 = new NoiseGeneratorOctaves(this.rand, 16);
		this.noiseGen3 = new NoiseGeneratorOctaves(this.rand, 8);
		this.noiseGen4 = new NoiseGeneratorOctaves(this.rand, 4);
		this.noiseGen5 = new NoiseGeneratorOctaves(this.rand, 4);
		new NoiseGeneratorOctaves(this.rand, 5);
		this.mobSpawnerNoise = new NoiseGeneratorOctaves(this.rand, 5);
	}

	/**
	 * Samples the three octave banks over the chunk's 5x5x17 grid and blends
	 * them into the coarse 3-D density field stored in {@link #noiseArray}.
	 * Each cell is a signed scalar: positive ⇒ stone, negative ⇒ water-or-air
	 * (decided by y in {@link #generateTerrain}).
	 */
	@Override
	protected final void initializeNoiseField(int chunkX, int chunkZ) {
		int gridX = chunkX << 2;
		int gridZ = chunkZ << 2;

		this.noise3 = this.noiseGen3.generateNoiseOctaves(this.noise3, gridX, 0, gridZ, GRID_WIDTH, GRID_HEIGHT, GRID_DEPTH, 8.555150000000001D, 4.277575000000001D, 8.555150000000001D);
		this.noise1 = this.noiseGen1.generateNoiseOctaves(this.noise1, gridX, 0, gridZ, GRID_WIDTH, GRID_HEIGHT, GRID_DEPTH, 684.412D, 684.412D, 684.412D);
		this.noise2 = this.noiseGen2.generateNoiseOctaves(this.noise2, gridX, 0, gridZ, GRID_WIDTH, GRID_HEIGHT, GRID_DEPTH, 684.412D, 684.412D, 684.412D);

		double[] densityField = this.noiseArray;
		if(densityField == null) {
			densityField = new double[NOISE_ARRAY_SIZE];
		}

		// Blend the three octave banks into one coarse 5x5x17 density field. The
		// storage order (z, x, y) matches the index math in the upsample below.
		int index = 0;
		for(int gridZIndex = 0; gridZIndex < GRID_DEPTH; ++gridZIndex) {
			for(int gridXIndex = 0; gridXIndex < GRID_WIDTH; ++gridXIndex) {
				for(int gridY = 0; gridY < GRID_HEIGHT; ++gridY) {
					double heightAboveSea = ((double) gridY - 8.5D) * 12.0D;
					if(heightAboveSea < 0.0D) {
						heightAboveSea *= 2.0D;
					}

					double lowFrequency = this.noise1[index] / 512.0D;
					double highFrequency = this.noise2[index] / 512.0D;
					double heightBlend = (this.noise3[index] / 10.0D + 1.0D) / 2.0D;

					double density;
					if(heightBlend < 0.0D) {
						density = lowFrequency;
					} else if(heightBlend > 1.0D) {
						density = highFrequency;
					} else {
						density = lowFrequency + (highFrequency - lowFrequency) * heightBlend;
					}

					density -= heightAboveSea;
					densityField[index] = density;
					++index;
				}
			}
		}

		this.noiseArray = densityField;
	}

	/**
	 * Tri-linearly up-samples the coarse height field into the chunk's block
	 * buffer. Each tile reads its 2x2x2 grid corner box (n000..n111, the "0/1"
	 * suffix is the x/y/z offset), interpolates vertically first (i000..i101),
	 * then along x (i00/i01), then z, and stamps every cell based on the
	 * interpolated **density** value: positive density is always stone, and
	 * negative density is water (below {@link #SEA_LEVEL}) or air (above).
	 */
	@Override
	protected final void generateTerrain(int chunkX, int chunkZ, byte[] blocks) {
		this.initializeNoiseField(chunkX, chunkZ);

		for(int tileX = 0; tileX < 4; ++tileX) {
			for(int tileZ = 0; tileZ < 4; ++tileZ) {
				for(int blockY = 0; blockY < 16; ++blockY) {
					double n000 = this.noiseArray[(tileX * GRID_WIDTH + tileZ) * GRID_HEIGHT + blockY];
					double n001 = this.noiseArray[(tileX * GRID_WIDTH + tileZ + 1) * GRID_HEIGHT + blockY];
					double n100 = this.noiseArray[((tileX + 1) * GRID_WIDTH + tileZ) * GRID_HEIGHT + blockY];
					double n101 = this.noiseArray[((tileX + 1) * GRID_WIDTH + tileZ + 1) * GRID_HEIGHT + blockY];
					double n010 = this.noiseArray[(tileX * GRID_WIDTH + tileZ) * GRID_HEIGHT + blockY + 1];
					double n011 = this.noiseArray[(tileX * GRID_WIDTH + tileZ + 1) * GRID_HEIGHT + blockY + 1];
					double n110 = this.noiseArray[((tileX + 1) * GRID_WIDTH + tileZ) * GRID_HEIGHT + blockY + 1];
					double n111 = this.noiseArray[((tileX + 1) * GRID_WIDTH + tileZ + 1) * GRID_HEIGHT + blockY + 1];

					for(int yStep = 0; yStep < 8; ++yStep) {
						double yFrac = (double) yStep / 8.0D;
						double i000 = n000 + (n010 - n000) * yFrac;
						double i001 = n001 + (n011 - n001) * yFrac;
						double i100 = n100 + (n110 - n100) * yFrac;
						double i101 = n101 + (n111 - n101) * yFrac;

						for(int xStep = 0; xStep < 4; ++xStep) {
							double xFrac = (double) xStep / 4.0D;
							double i00 = i000 + (i100 - i000) * xFrac;
							double i01 = i001 + (i101 - i001) * xFrac;
							int blockIndex = (xStep + (tileX << 2)) << 11 | (tileZ << 2) << 7 | (blockY << 3) + yStep;

							for(int zStep = 0; zStep < 4; ++zStep) {
								double zFrac = (double) zStep / 4.0D;
								double density = i00 + (i01 - i00) * zFrac;
								int block = 0;
								if((blockY << 3) + yStep < SEA_LEVEL) {
									block = Block.waterStill.blockID;
								}

								if(density > 0.0D) {
									block = Block.stone.blockID;
								}

								blocks[blockIndex] = (byte) block;
								blockIndex += 128;
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Walks every column top-down and replaces the surface of the plain stone
	 * volume with the world's surface layer. The beach/bed noise is sampled per
	 * column (sand and gravel), a dirt depth fades off with a further noise
	 * bend, and the column is capped with grass above water, sand on the beach
	 * line or bare stone under water.
	 */
	@Override
	protected final void replaceBlocks(int chunkX, int chunkZ, byte[] blocks) {
		for(int x = 0; x < 16; ++x) {
			for(int z = 0; z < 16; ++z) {
				// Per-column surface decisions use the coarse biome-ish noise: a
				// sand beach (noise + jitter above 0), a gravel bed (above 3),
				// and a dirt depth that fades off with yet another noise bend.
				double worldX = (double) ((chunkX << 4) + x);
				double worldZ = (double) ((chunkZ << 4) + z);
				boolean sandBeach = this.noiseGen4.generateNoiseOctaves(worldX * (1.0D / 32.0D), worldZ * (1.0D / 32.0D), 0.0D) + this.rand.nextDouble() * 0.2D > 0.0D;
				boolean gravelBed = this.noiseGen4.generateNoiseOctaves(worldZ * (1.0D / 32.0D), 109.0134D, worldX * (1.0D / 32.0D)) + this.rand.nextDouble() * 0.2D > 3.0D;
				int dirtDepth = (int) (this.noiseGen5.noiseGenerator(worldX * (1.0D / 32.0D) * 2.0D, worldZ * (1.0D / 32.0D) * 2.0D) / 3.0D + 3.0D + this.rand.nextDouble() * 0.25D);
				// Walk the column top-down; blockIndex lands on (x, z, 127)
				// first and decrements by one cell per step.
				int blockIndex = x << 11 | z << 7 | 127;
				int dirtRemaining = -1;
				int surfaceBlock = Block.grass.blockID;
				int groundBlock = Block.dirt.blockID;

				for(int y = 127; y >= 0; --y) {
					if(blocks[blockIndex] == 0) {
						dirtRemaining = -1;
					} else if(blocks[blockIndex] == Block.stone.blockID) {
						if(dirtRemaining == -1) {
							if(dirtDepth <= 0) {
								surfaceBlock = 0;
								groundBlock = (byte) Block.stone.blockID;
							} else if(y >= 60 && y <= 65) {
								surfaceBlock = Block.grass.blockID;
								groundBlock = Block.dirt.blockID;
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

							if(y < SEA_LEVEL && surfaceBlock == 0) {
								surfaceBlock = Block.waterStill.blockID;
							}

							dirtRemaining = dirtDepth;
							if(y >= 63) {
								blocks[blockIndex] = (byte) surfaceBlock;
							} else {
								blocks[blockIndex] = (byte) groundBlock;
							}
						} else if(dirtRemaining > 0) {
							--dirtRemaining;
							blocks[blockIndex] = (byte) groundBlock;
						}
					}

					--blockIndex;
				}
			}
		}
	}

	/**
	 * Decoration stage: four ore passes (coal generously, iron a third as much,
	 * gold and diamond only when the chunk-local chance hits) followed by the
	 * tree line derived from the mob-spawn noise. Re-seeds the chunk RNG first
	 * so decoration is reproducible per chunk.
	 */
	@Override
	public final void populate(IChunkProvider chunkProvider, int chunkX, int chunkZ) {
		this.rand.setSeed((long) chunkX * 318279123L + (long) chunkZ * 919871212L);
		int baseX = chunkX << 4;
		int baseZ = chunkZ << 4;

		// The ore passes run as stream repeats; each placeOreVein call consumes
		// the shared Random in the same order as the original build.
		WorldGenMinable coalVein = new WorldGenMinable(Block.oreCoal.blockID);
		WorldGenMinable ironVein = new WorldGenMinable(Block.oreIron.blockID);
		IntStream.range(0, 20).forEach(i -> placeOreVein(coalVein, 128, baseX, baseZ));
		IntStream.range(0, 10).forEach(i -> placeOreVein(ironVein, 64, baseX, baseZ));

		WorldGenMinable goldVein = new WorldGenMinable(Block.oreGold.blockID);
		WorldGenMinable diamondVein = new WorldGenMinable(Block.oreDiamond.blockID);
		if(this.rand.nextInt(2) == 0) {
			placeOreVein(goldVein, 32, baseX, baseZ);
		}

		if(this.rand.nextInt(8) == 0) {
			placeOreVein(diamondVein, 16, baseX, baseZ);
		}

		int treeCount = (int) (this.mobSpawnerNoise.noiseGenerator((double) baseX * 0.05D, (double) baseZ * 0.05D) - this.rand.nextDouble());
		if(treeCount < 0) {
			treeCount = 0;
		}

		WorldGenBigTree bigTree = new WorldGenBigTree();
		if(this.rand.nextInt(100) == 0) {
			++treeCount;
		}

		IntStream.range(0, treeCount).forEach(i -> {
			int treeX = baseX + this.rand.nextInt(16) + 8;
			int treeZ = baseZ + this.rand.nextInt(16) + 8;
			bigTree.setScale(1.0D, 1.0D, 1.0D);
			bigTree.generate(this.worldObj, this.rand, treeX, this.worldObj.getHeightValue(treeX, treeZ), treeZ);
		});
	}

	/** Drops a single ore vein at a random cell of the chunk's base coordinates. */
	private final void placeOreVein(WorldGenMinable vein, int yUpperBound, int baseX, int baseZ) {
		int x = baseX + this.rand.nextInt(16);
		int y = this.rand.nextInt(yUpperBound);
		int z = baseZ + this.rand.nextInt(16);
		vein.generate(this.worldObj, this.rand, x, y, z);
	}
}