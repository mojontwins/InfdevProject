package net.minecraft.game.world.block;

import java.util.Random;
import net.minecraft.game.physics.AxisAlignedBB;
import net.minecraft.game.world.IBlockAccess;
import net.minecraft.game.world.World;
import net.minecraft.game.world.material.Material;

/**
 * Fire. The two tables rate every block id for flammability: how eagerly fire
 * spreads onto it ({@link #chanceToEncourageFire}) and how long it burns before
 * being consumed ({@link #abilityToCatchFire}). The block ages 0-15 in its
 * metadata, spreading to neighbours only late in its life, and dies when it has
 * nothing burnable left around or beneath it.
 */
public final class BlockFire extends Block {
	private int[] chanceToEncourageFire = new int[256];
	private int[] abilityToCatchFire = new int[256];

	protected BlockFire(int blockID, int textureIndex) {
		super(blockID, textureIndex, Material.fire);
		// Planks/wood catch fire with odds 5/20, while wood, leaves, bookshelves and
		// cloth burn at the eager 30/60 pace; TNT sparks almost at once.
		this.setBurnRate(Block.planks.blockID, 5, 20);
		this.setBurnRate(Block.wood.blockID, 5, 5);
		this.setBurnRate(Block.leaves.blockID, 30, 60);
		this.setBurnRate(Block.bookshelf.blockID, 30, 20);
		this.setBurnRate(Block.cloth.blockID, 30, 60);
		this.setBurnRate(Block.tnt.blockID, 15, 100);

		this.setTickOnLoad(true);
	}

	private void setBurnRate(int blockID, int burnChance, int burnTime) {
		this.chanceToEncourageFire[blockID] = burnChance;
		this.abilityToCatchFire[blockID] = burnTime;
	}

	@Override
	public final AxisAlignedBB getCollisionBoundingBoxFromPool(int x, int y, int z) {
		return null;
	}

	@Override
	public final boolean isOpaqueCube() {
		return false;
	}

	@Override
	public final boolean renderAsNormalBlock() {
		return false;
	}

	@Override
	public final int getRenderType() {
		return 3;
	}

	@Override
	public final int quantityDropped(Random random) {
		return 0;
	}

	@Override
	public final int tickRate() {
		return 20;
	}

	@Override
	public final void updateTick(World world, int x, int y, int z, Random random) {
		int fireLevel = world.getBlockMetadata(x, y, z);
		if(fireLevel < 15) {
			world.setBlockMetadataWithNotify(x, y, z, fireLevel + 1);
			world.scheduleBlockUpdate(x, y, z, this.blockID);
		}

		if(!this.canNeighborCatchFire(world, x, y, z)) {
			if(!world.isSolid(x, y - 1, z) || fireLevel > 3) {
				world.setBlockWithNotify(x, y, z, 0);
			}
		} else if(!this.canBlockCatchFire(world, x, y - 1, z) && fireLevel == 15 && random.nextInt(4) == 0) {
			world.setBlockWithNotify(x, y, z, 0);
		} else {
			if(fireLevel % 5 == 0 && fireLevel > 5) {
				this.tryToCatchBlockOnFire(world, x + 1, y, z, 300, random);
				this.tryToCatchBlockOnFire(world, x - 1, y, z, 300, random);
				this.tryToCatchBlockOnFire(world, x, y - 1, z, 100, random);
				this.tryToCatchBlockOnFire(world, x, y + 1, z, 200, random);
				this.tryToCatchBlockOnFire(world, x, y, z - 1, 300, random);
				this.tryToCatchBlockOnFire(world, x, y, z + 1, 300, random);

				for(int spreadX = x - 1; spreadX <= x + 1; ++spreadX) {
					for(int spreadZ = z - 1; spreadZ <= z + 1; ++spreadZ) {
						for(int spreadY = y - 1; spreadY <= y + 4; ++spreadY) {
							if(spreadX != x || spreadY != y || spreadZ != z) {
								int ignitionChance = 100;
								if(spreadY > y + 1) {
									ignitionChance = 100 + (spreadY - (y + 1)) * 100;
								}

								int catchChance;
								if(world.getBlockId(spreadX, spreadY, spreadZ) != 0) {
									catchChance = 0;
								} else {
									int nearestFuel = this.getChanceToEncourageFire(world, spreadX + 1, spreadY, spreadZ, 0);
									nearestFuel = this.getChanceToEncourageFire(world, spreadX - 1, spreadY, spreadZ, nearestFuel);
									nearestFuel = this.getChanceToEncourageFire(world, spreadX, spreadY - 1, spreadZ, nearestFuel);
									nearestFuel = this.getChanceToEncourageFire(world, spreadX, spreadY + 1, spreadZ, nearestFuel);
									nearestFuel = this.getChanceToEncourageFire(world, spreadX, spreadY, spreadZ - 1, nearestFuel);
									nearestFuel = this.getChanceToEncourageFire(world, spreadX, spreadY, spreadZ + 1, nearestFuel);
									catchChance = nearestFuel;
								}

								if(catchChance > 0 && random.nextInt(ignitionChance) <= catchChance) {
									world.setBlockWithNotify(spreadX, spreadY, spreadZ, this.blockID);
								}
							}
						}
					}
				}
			}
		}
	}

	private void tryToCatchBlockOnFire(World world, int x, int y, int z, int chance, Random random) {
		int catchChance = this.abilityToCatchFire[world.getBlockId(x, y, z)];
		if(random.nextInt(chance) < catchChance) {
			boolean isTNT = world.getBlockId(x, y, z) == Block.tnt.blockID;
			if(random.nextInt(2) == 0) {
				world.setBlockWithNotify(x, y, z, this.blockID);
			} else {
				world.setBlockWithNotify(x, y, z, 0);
			}
			if(isTNT) {
				Block.tnt.onBlockDestroyedByPlayer(world, x, y, z, 0);
			}
		}
	}

	private boolean canNeighborCatchFire(World world, int x, int y, int z) {
		return this.canBlockCatchFire(world, x + 1, y, z) || this.canBlockCatchFire(world, x - 1, y, z) || this.canBlockCatchFire(world, x, y - 1, z) || this.canBlockCatchFire(world, x, y + 1, z) || this.canBlockCatchFire(world, x, y, z - 1) || this.canBlockCatchFire(world, x, y, z + 1);
	}

	@Override
	public final boolean isCollidable() {
		return false;
	}

	public final boolean canBlockCatchFire(IBlockAccess blockAccess, int x, int y, int z) {
		return this.chanceToEncourageFire[blockAccess.getBlockId(x, y, z)] > 0;
	}

	private int getChanceToEncourageFire(World world, int x, int y, int z, int currentMax) {
		int fuelChance = this.chanceToEncourageFire[world.getBlockId(x, y, z)];
		return Math.max(fuelChance, currentMax);
	}

	@Override
	public final boolean canPlaceBlockAt(World world, int x, int y, int z) {
		return world.isSolid(x, y - 1, z) || this.canNeighborCatchFire(world, x, y, z);
	}

	@Override
	public final void onNeighborBlockChange(World world, int x, int y, int z, int neighborID) {
		if(!world.isSolid(x, y - 1, z) && !this.canNeighborCatchFire(world, x, y, z)) {
			world.setBlockWithNotify(x, y, z, 0);
		}
	}

	@Override
	public final void onBlockAdded(World world, int x, int y, int z) {
		if(!world.isSolid(x, y - 1, z) && !this.canNeighborCatchFire(world, x, y, z)) {
			world.setBlockWithNotify(x, y, z, 0);
		} else {
			world.scheduleBlockUpdate(x, y, z, this.blockID);
		}
	}

	public final boolean getChanceOfNeighborsEncouragingFire(int blockID) {
		return this.chanceToEncourageFire[blockID] > 0;
	}

	public final void fireSpread(World world, int x, int y, int z) {
		boolean fireSet = false;
		fireSet = fireCheck(world, x, y + 1, z);
		if(!fireSet) {
			fireSet = fireCheck(world, x - 1, y, z);
		}
		if(!fireSet) {
			fireSet = fireCheck(world, x + 1, y, z);
		}
		if(!fireSet) {
			fireSet = fireCheck(world, x, y, z - 1);
		}
		if(!fireSet) {
			fireSet = fireCheck(world, x, y, z + 1);
		}
		if(!fireSet) {
			fireSet = fireCheck(world, x, y - 1, z);
		}
		if(!fireSet) {
			world.setBlockWithNotify(x, y, z, Block.fire.blockID);
		}
	}

	@Override
	public final void randomDisplayTick(World world, int x, int y, int z, Random random) {
		if(random.nextInt(24) == 0) {
			world.playSoundEffect((double)((float)x + 0.5F), (double)((float)y + 0.5F), (double)((float)z + 0.5F), "fire.fire", 1.0F + random.nextFloat(), random.nextFloat() * 0.7F + 0.3F);
		}

		int particleIndex;
		float particleX;
		float particleY;
		float particleZ;
		if(!world.isSolid(x, y - 1, z) && !Block.fire.canBlockCatchFire(world, x, y - 1, z)) {
			if(Block.fire.canBlockCatchFire(world, x - 1, y, z)) {
				for(particleIndex = 0; particleIndex < 2; ++particleIndex) {
					particleX = (float)x + random.nextFloat() * 0.1F;
					particleY = (float)y + random.nextFloat();
					particleZ = (float)z + random.nextFloat();
					world.spawnParticle("largesmoke", (double)particleX, (double)particleY, (double)particleZ, 0.0D, 0.0D, 0.0D);
				}
			}

			if(Block.fire.canBlockCatchFire(world, x + 1, y, z)) {
				for(particleIndex = 0; particleIndex < 2; ++particleIndex) {
					particleX = (float)(x + 1) - random.nextFloat() * 0.1F;
					particleY = (float)y + random.nextFloat();
					particleZ = (float)z + random.nextFloat();
					world.spawnParticle("largesmoke", (double)particleX, (double)particleY, (double)particleZ, 0.0D, 0.0D, 0.0D);
				}
			}

			if(Block.fire.canBlockCatchFire(world, x, y, z - 1)) {
				for(particleIndex = 0; particleIndex < 2; ++particleIndex) {
					particleX = (float)x + random.nextFloat();
					particleY = (float)y + random.nextFloat();
					particleZ = (float)z + random.nextFloat() * 0.1F;
					world.spawnParticle("largesmoke", (double)particleX, (double)particleY, (double)particleZ, 0.0D, 0.0D, 0.0D);
				}
			}

			if(Block.fire.canBlockCatchFire(world, x, y, z + 1)) {
				for(particleIndex = 0; particleIndex < 2; ++particleIndex) {
					particleX = (float)x + random.nextFloat();
					particleY = (float)y + random.nextFloat();
					particleZ = (float)(z + 1) - random.nextFloat() * 0.1F;
					world.spawnParticle("largesmoke", (double)particleX, (double)particleY, (double)particleZ, 0.0D, 0.0D, 0.0D);
				}
			}

			if(Block.fire.canBlockCatchFire(world, x, y + 1, z)) {
				for(particleIndex = 0; particleIndex < 2; ++particleIndex) {
					particleX = (float)x + random.nextFloat();
					particleY = (float)(y + 1) - random.nextFloat() * 0.1F;
					particleZ = (float)z + random.nextFloat();
					world.spawnParticle("largesmoke", (double)particleX, (double)particleY, (double)particleZ, 0.0D, 0.0D, 0.0D);
				}
			}
		} else {
			for(particleIndex = 0; particleIndex < 3; ++particleIndex) {
				particleX = (float)x + random.nextFloat();
				particleY = (float)y + random.nextFloat() * 0.5F + 0.5F;
				particleZ = (float)z + random.nextFloat();
				world.spawnParticle("largesmoke", (double)particleX, (double)particleY, (double)particleZ, 0.0D, 0.0D, 0.0D);
			}
		}
	}

	private static boolean fireCheck(World world, int x, int y, int z) {
		int blockID = world.getBlockId(x, y, z);
		if(blockID == Block.fire.blockID) {
			return true;
		} else if(blockID == 0) {
			world.setBlockWithNotify(x, y, z, Block.fire.blockID);
			return true;
		} else {
			return false;
		}
	}
}