package net.minecraft.game.world.terrain;

import java.util.Random;
import net.minecraft.game.world.World;
import net.minecraft.game.world.block.Block;
import net.minecraft.game.world.block.BlockSand;
import net.minecraft.game.world.chunk.Chunk;
import net.minecraft.game.world.chunk.IChunkProvider;
import net.minecraft.game.world.terrain.generate.WorldGenBigTree;
import net.minecraft.game.world.terrain.generate.WorldGenMinable;
import net.minecraft.game.world.terrain.noise.NoiseGeneratorOctaves;

public final class ChunkProviderGenerate implements IChunkProvider {
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

	public ChunkProviderGenerate(World var1, long var2) {
		this.worldObj = var1;
		this.rand = new Random(var2);
		new Random(var2);
		this.noiseGen1 = new NoiseGeneratorOctaves(this.rand, 16);
		this.noiseGen2 = new NoiseGeneratorOctaves(this.rand, 16);
		this.noiseGen3 = new NoiseGeneratorOctaves(this.rand, 8);
		this.noiseGen4 = new NoiseGeneratorOctaves(this.rand, 4);
		this.noiseGen5 = new NoiseGeneratorOctaves(this.rand, 4);
		new NoiseGeneratorOctaves(this.rand, 5);
		this.mobSpawnerNoise = new NoiseGeneratorOctaves(this.rand, 5);
	}

	public final Chunk provideChunk(int var1, int var2) {
		this.rand.setSeed((long)var1 * 341873128712L + (long)var2 * 132897987541L);
		byte[] var3 = new byte[-Short.MIN_VALUE];
		Chunk var4 = new Chunk(this.worldObj, var3, var1, var2);
		int var10003 = var1 << 2;
		int var10005 = var2 << 2;
		int var8 = var10005;
		int var7 = var10003;
		double[] var6 = this.noiseArray;
		ChunkProviderGenerate var71 = this;
		if(var6 == null) {
			var6 = new double[425];
		}

		this.noise3 = this.noiseGen3.generateNoiseOctaves(this.noise3, var7, 0, var8, 5, 17, 5, 8.555150000000001D, 4.277575000000001D, 8.555150000000001D);
		this.noise1 = this.noiseGen1.generateNoiseOctaves(this.noise1, var7, 0, var8, 5, 17, 5, 684.412D, 684.412D, 684.412D);
		this.noise2 = this.noiseGen2.generateNoiseOctaves(this.noise2, var7, 0, var8, 5, 17, 5, 684.412D, 684.412D, 684.412D);
		var7 = 0;

		for(var8 = 0; var8 < 5; ++var8) {
			for(int var9 = 0; var9 < 5; ++var9) {
				for(int var10 = 0; var10 < 17; ++var10) {
					double var63 = ((double)var10 - 8.5D) * 12.0D;
					if(var63 < 0.0D) {
						var63 *= 2.0D;
					}

					double var65 = var71.noise1[var7] / 512.0D;
					double var67 = var71.noise2[var7] / 512.0D;
					double var69 = (var71.noise3[var7] / 10.0D + 1.0D) / 2.0D;
					double var61;
					if(var69 < 0.0D) {
						var61 = var65;
					} else if(var69 > 1.0D) {
						var61 = var67;
					} else {
						var61 = var65 + (var67 - var65) * var69;
					}

					var61 -= var63;
					var6[var7] = var61;
					++var7;
				}
			}
		}

		this.noiseArray = var6;

		int var72;
		int var73;
		for(var72 = 0; var72 < 4; ++var72) {
			for(var73 = 0; var73 < 4; ++var73) {
				for(var7 = 0; var7 < 16; ++var7) {
					double var75 = this.noiseArray[(var72 * 5 + var73) * 17 + var7];
					double var77 = this.noiseArray[(var72 * 5 + var73 + 1) * 17 + var7];
					double var12 = this.noiseArray[((var72 + 1) * 5 + var73) * 17 + var7];
					double var14 = this.noiseArray[((var72 + 1) * 5 + var73 + 1) * 17 + var7];
					double var16 = this.noiseArray[(var72 * 5 + var73) * 17 + var7 + 1];
					double var18 = this.noiseArray[(var72 * 5 + var73 + 1) * 17 + var7 + 1];
					double var20 = this.noiseArray[((var72 + 1) * 5 + var73) * 17 + var7 + 1];
					double var22 = this.noiseArray[((var72 + 1) * 5 + var73 + 1) * 17 + var7 + 1];

					for(int var24 = 0; var24 < 8; ++var24) {
						double var25 = (double)var24 / 8.0D;
						double var27 = var75 + (var16 - var75) * var25;
						double var29 = var77 + (var18 - var77) * var25;
						double var31 = var12 + (var20 - var12) * var25;
						double var33 = var14 + (var22 - var14) * var25;

						for(int var82 = 0; var82 < 4; ++var82) {
							double var36 = (double)var82 / 4.0D;
							double var38 = var27 + (var31 - var27) * var36;
							double var40 = var29 + (var33 - var29) * var36;
							int var26 = var82 + (var72 << 2) << 11 | 0 + (var73 << 2) << 7 | (var7 << 3) + var24;

							for(int var35 = 0; var35 < 4; ++var35) {
								double var44 = (double)var35 / 4.0D;
								double var46 = var38 + (var40 - var38) * var44;
								int var83 = 0;
								if((var7 << 3) + var24 < 64) {
									var83 = Block.waterStill.blockID;
								}

								if(var46 > 0.0D) {
									var83 = Block.stone.blockID;
								}

								var3[var26] = (byte)var83;
								var26 += 128;
							}
						}
					}
				}
			}
		}

		for(var72 = 0; var72 < 16; ++var72) {
			for(var73 = 0; var73 < 16; ++var73) {
				double var74 = (double)((var1 << 4) + var72);
				double var76 = (double)((var2 << 4) + var73);
				boolean var13 = this.noiseGen4.generateNoiseOctaves(var74 * (1.0D / 32.0D), var76 * (1.0D / 32.0D), 0.0D) + this.rand.nextDouble() * 0.2D > 0.0D;
				boolean var78 = this.noiseGen4.generateNoiseOctaves(var76 * (1.0D / 32.0D), 109.0134D, var74 * (1.0D / 32.0D)) + this.rand.nextDouble() * 0.2D > 3.0D;
				int var15 = (int)(this.noiseGen5.noiseGenerator(var74 * (1.0D / 32.0D) * 2.0D, var76 * (1.0D / 32.0D) * 2.0D) / 3.0D + 3.0D + this.rand.nextDouble() * 0.25D);
				int var79 = var72 << 11 | var73 << 7 | 127;
				int var17 = -1;
				int var80 = Block.grass.blockID;
				int var19 = Block.dirt.blockID;

				for(int var81 = 127; var81 >= 0; --var81) {
					if(var3[var79] == 0) {
						var17 = -1;
					} else if(var3[var79] == Block.stone.blockID) {
						if(var17 == -1) {
							if(var15 <= 0) {
								var80 = 0;
								var19 = (byte)Block.stone.blockID;
							} else if(var81 >= 60 && var81 <= 65) {
								var80 = Block.grass.blockID;
								var19 = Block.dirt.blockID;
								if(var78) {
									var80 = 0;
								}

								if(var78) {
									var19 = Block.gravel.blockID;
								}

								if(var13) {
									var80 = Block.sand.blockID;
								}

								if(var13) {
									var19 = Block.sand.blockID;
								}
							}

							if(var81 < 64 && var80 == 0) {
								var80 = Block.waterStill.blockID;
							}

							var17 = var15;
							if(var81 >= 63) {
								var3[var79] = (byte)var80;
							} else {
								var3[var79] = (byte)var19;
							}
						} else if(var17 > 0) {
							--var17;
							var3[var79] = (byte)var19;
						}
					}

					--var79;
				}
			}
		}

		var4.generateHeightMap();
		return var4;
	}

	public final boolean chunkExists(int var1, int var2) {
		return true;
	}

	public final void populate(IChunkProvider var1, int var2, int var3) {
		BlockSand.fallInstantly = true;
		this.rand.setSeed((long)var2 * 318279123L + (long)var3 * 919871212L);
		int var8 = var2 << 4;
		var2 = var3 << 4;

		int var4;
		int var5;
		int var6;
		for(var3 = 0; var3 < 20; ++var3) {
			var4 = var8 + this.rand.nextInt(16);
			var5 = this.rand.nextInt(128);
			var6 = var2 + this.rand.nextInt(16);
			(new WorldGenMinable(Block.oreCoal.blockID)).generate(this.worldObj, this.rand, var4, var5, var6);
		}

		for(var3 = 0; var3 < 10; ++var3) {
			var4 = var8 + this.rand.nextInt(16);
			var5 = this.rand.nextInt(64);
			var6 = var2 + this.rand.nextInt(16);
			(new WorldGenMinable(Block.oreIron.blockID)).generate(this.worldObj, this.rand, var4, var5, var6);
		}

		if(this.rand.nextInt(2) == 0) {
			var3 = var8 + this.rand.nextInt(16);
			var4 = this.rand.nextInt(32);
			var5 = var2 + this.rand.nextInt(16);
			(new WorldGenMinable(Block.oreGold.blockID)).generate(this.worldObj, this.rand, var3, var4, var5);
		}

		if(this.rand.nextInt(8) == 0) {
			var3 = var8 + this.rand.nextInt(16);
			var4 = this.rand.nextInt(16);
			var5 = var2 + this.rand.nextInt(16);
			(new WorldGenMinable(Block.oreDiamond.blockID)).generate(this.worldObj, this.rand, var3, var4, var5);
		}

		var3 = (int)(this.mobSpawnerNoise.noiseGenerator((double)var8 * 0.05D, (double)var2 * 0.05D) - this.rand.nextDouble());
		if(var3 < 0) {
			var3 = 0;
		}

		WorldGenBigTree var9 = new WorldGenBigTree();
		if(this.rand.nextInt(100) == 0) {
			++var3;
		}

		for(var5 = 0; var5 < var3; ++var5) {
			var6 = var8 + this.rand.nextInt(16) + 8;
			int var7 = var2 + this.rand.nextInt(16) + 8;
			var9.setScale(1.0D, 1.0D, 1.0D);
			var9.generate(this.worldObj, this.rand, var6, this.worldObj.getHeightValue(var6, var7), var7);
		}

		BlockSand.fallInstantly = false;
	}

	public final void saveChunks(boolean var1) {
	}

	public final boolean unload100OldestChunks() {
		return false;
	}
}
