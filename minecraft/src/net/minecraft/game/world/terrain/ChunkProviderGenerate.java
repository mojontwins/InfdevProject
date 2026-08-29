package net.minecraft.game.world.terrain;

import java.util.Random;
import net.minecraft.game.world.World;
import net.minecraft.game.world.block.Block;
import net.minecraft.game.world.chunk.Chunk;
import net.minecraft.game.world.chunk.IChunkProvider;
import net.minecraft.game.world.terrain.generate.WorldGenBigTree;
import net.minecraft.game.world.terrain.generate.WorldGenMinable;
import net.minecraft.game.world.terrain.noise.NoiseGeneratorOctaves;

/**
 * The procedural terrain provider. It builds every chunk from scratch out of
 * layered fractal noise rather than loading it from disk:
 *
 * <ol>
 *   <li><b>Height field</b> — a coarse 5x5x17 grid of noise is sampled and
 *       blended into a single height map that encodes the low and high
 *       frequency shape of the land.</li>
 *   <li><b>Terrain fill</b> — the coarse grid is tri-linearly interpolated up
 *       to full resolution; positive values become stone and water is left
 *       below sea level, then a surface pass carves grass, sand, gravel and
 *       dirt onto the top of each column.</li>
 *   <li><b>Decoration</b> — {@link #populate} scatters ore veins and trees.</li>
 * </ol>
 *
 * <p>Two throw-away noise generators are constructed in the constructor purely
 * to advance the RNG stream; they must not be removed because every later
 * generator's seed depends on how much randomness the early {@link Random}
 * object has already consumed.
 */
public final class ChunkProviderGenerate implements IChunkProvider {
	private static final int GRID_WIDTH = 5;
	private static final int GRID_HEIGHT = 17;
	private static final int GRID_DEPTH = 5;
	private static final int NOISE_ARRAY_SIZE = GRID_WIDTH * GRID_HEIGHT * GRID_DEPTH; // 425
	private static final int SEA_LEVEL = 64;

	private Random rand;
	private NoiseGeneratorOctaves noiseGen1;
	private NoiseGeneratorOctaves noiseGen2;
	private NoiseGeneratorOctaves noiseGen3;
	private NoiseGeneratorOctaves noiseGen4;
	private NoiseGeneratorOctaves noiseGen5;
	private NoiseGeneratorOctaves mobSpawnerNoise;
	private World worldObj;
	private double[] noiseArray;
	private double[] noise3;
	private double[] noise1;
	private double[] noise2;

	public ChunkProviderGenerate(World world, long seed) {
		this.worldObj = world;
		this.rand = new Random(seed);
		new Random(seed);
		this.noiseGen1 = new NoiseGeneratorOctaves(this.rand, 16);
		this.noiseGen2 = new NoiseGeneratorOctaves(this.rand, 16);
		this.noiseGen3 = new NoiseGeneratorOctaves(this.rand, 8);
		this.noiseGen4 = new NoiseGeneratorOctaves(this.rand, 4);
		this.noiseGen5 = new NoiseGeneratorOctaves(this.rand, 4);
		new NoiseGeneratorOctaves(this.rand, 5);
		this.mobSpawnerNoise = new NoiseGeneratorOctaves(this.rand, 5);
	}

	public final Chunk provideChunk(int chunkX, int chunkZ) {
		this.rand.setSeed((long) chunkX * 341873128712L + (long) chunkZ * 132897987541L);
		byte[] blocks = new byte[-Short.MIN_VALUE];
		Chunk chunk = new Chunk(this.worldObj, blocks, chunkX, chunkZ);
		int gridX = chunkX << 2;
		int gridZ = chunkZ << 2;

		this.noise3 = this.noiseGen3.generateNoiseOctaves(this.noise3, gridX, 0, gridZ, GRID_WIDTH, GRID_HEIGHT, GRID_DEPTH, 8.555150000000001D, 4.277575000000001D, 8.555150000000001D);
		this.noise1 = this.noiseGen1.generateNoiseOctaves(this.noise1, gridX, 0, gridZ, GRID_WIDTH, GRID_HEIGHT, GRID_DEPTH, 684.412D, 684.412D, 684.412D);
		this.noise2 = this.noiseGen2.generateNoiseOctaves(this.noise2, gridX, 0, gridZ, GRID_WIDTH, GRID_HEIGHT, GRID_DEPTH, 684.412D, 684.412D, 684.412D);

		double[] heightMap = this.noiseArray;
		if(heightMap == null) {
			heightMap = new double[NOISE_ARRAY_SIZE];
		}

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

					double height;
					if(heightBlend < 0.0D) {
						height = lowFrequency;
					} else if(heightBlend > 1.0D) {
						height = highFrequency;
					} else {
						height = lowFrequency + (highFrequency - lowFrequency) * heightBlend;
					}

					height -= heightAboveSea;
					heightMap[index] = height;
					++index;
				}
			}
		}

		this.noiseArray = heightMap;

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
								double height = i00 + (i01 - i00) * zFrac;
								int block = 0;
								if((blockY << 3) + yStep < SEA_LEVEL) {
									block = Block.waterStill.blockID;
								}

								if(height > 0.0D) {
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

		for(int x = 0; x < 16; ++x) {
			for(int z = 0; z < 16; ++z) {
				double worldX = (double) ((chunkX << 4) + x);
				double worldZ = (double) ((chunkZ << 4) + z);
				boolean sandBeach = this.noiseGen4.generateNoiseOctaves(worldX * (1.0D / 32.0D), worldZ * (1.0D / 32.0D), 0.0D) + this.rand.nextDouble() * 0.2D > 0.0D;
				boolean gravelBed = this.noiseGen4.generateNoiseOctaves(worldZ * (1.0D / 32.0D), 109.0134D, worldX * (1.0D / 32.0D)) + this.rand.nextDouble() * 0.2D > 3.0D;
				int dirtDepth = (int) (this.noiseGen5.noiseGenerator(worldX * (1.0D / 32.0D) * 2.0D, worldZ * (1.0D / 32.0D) * 2.0D) / 3.0D + 3.0D + this.rand.nextDouble() * 0.25D);
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

		chunk.generateHeightMap();
		return chunk;
	}

	public final boolean chunkExists(int chunkX, int chunkZ) {
		return true;
	}

	public final void populate(IChunkProvider chunkProvider, int chunkX, int chunkZ) {
		this.rand.setSeed((long) chunkX * 318279123L + (long) chunkZ * 919871212L);
		int baseX = chunkX << 4;
		int baseZ = chunkZ << 4;

		int x;
		int y;
		int z;
		for(int i = 0; i < 20; ++i) {
			x = baseX + this.rand.nextInt(16);
			y = this.rand.nextInt(128);
			z = baseZ + this.rand.nextInt(16);
			(new WorldGenMinable(Block.oreCoal.blockID)).generate(this.worldObj, this.rand, x, y, z);
		}

		for(int i = 0; i < 10; ++i) {
			x = baseX + this.rand.nextInt(16);
			y = this.rand.nextInt(64);
			z = baseZ + this.rand.nextInt(16);
			(new WorldGenMinable(Block.oreIron.blockID)).generate(this.worldObj, this.rand, x, y, z);
		}

		if(this.rand.nextInt(2) == 0) {
			x = baseX + this.rand.nextInt(16);
			y = this.rand.nextInt(32);
			z = baseZ + this.rand.nextInt(16);
			(new WorldGenMinable(Block.oreGold.blockID)).generate(this.worldObj, this.rand, x, y, z);
		}

		if(this.rand.nextInt(8) == 0) {
			x = baseX + this.rand.nextInt(16);
			y = this.rand.nextInt(16);
			z = baseZ + this.rand.nextInt(16);
			(new WorldGenMinable(Block.oreDiamond.blockID)).generate(this.worldObj, this.rand, x, y, z);
		}

		int treeCount = (int) (this.mobSpawnerNoise.noiseGenerator((double) baseX * 0.05D, (double) baseZ * 0.05D) - this.rand.nextDouble());
		if(treeCount < 0) {
			treeCount = 0;
		}

		WorldGenBigTree bigTree = new WorldGenBigTree();
		if(this.rand.nextInt(100) == 0) {
			++treeCount;
		}

		for(int i = 0; i < treeCount; ++i) {
			int treeX = baseX + this.rand.nextInt(16) + 8;
			int treeZ = baseZ + this.rand.nextInt(16) + 8;
			bigTree.setScale(1.0D, 1.0D, 1.0D);
			bigTree.generate(this.worldObj, this.rand, treeX, this.worldObj.getHeightValue(treeX, treeZ), treeZ);
		}
	}

	public final void saveChunks(boolean flag) {
	}

	public final boolean unload100OldestChunks() {
		return false;
	}
}
