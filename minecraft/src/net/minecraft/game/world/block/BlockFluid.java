package net.minecraft.game.world.block;

import java.util.Random;
import net.minecraft.game.physics.AxisAlignedBB;
import net.minecraft.game.world.IBlockAccess;
import net.minecraft.game.world.World;
import net.minecraft.game.world.material.Material;

public class BlockFluid extends Block {
	protected int stillId;
	protected int movingId;

	protected BlockFluid(int blockID, Material material) {
		super(blockID, material);
		this.blockIndexInTexture = 14;
		if(material == Material.lava) {
			this.blockIndexInTexture = 30;
		}
		Block.isBlockContainer[blockID] = true;
		this.movingId = blockID;
		this.stillId = blockID + 1;
		this.setBlockBounds(0.01F, -0.09F, 0.01F, 1.01F, 0.90999997F, 1.01F);
		this.setTickOnLoad(true);
		this.setResistance(2.0F);
	}

	@Override
	public final int getBlockTextureFromSide(int side) {
		return this.blockMaterial == Material.lava ? this.blockIndexInTexture : (side == 1 ? this.blockIndexInTexture : (side == 0 ? this.blockIndexInTexture : this.blockIndexInTexture + 32));
	}

	@Override
	public final boolean renderAsNormalBlock() {
		return false;
	}

	@Override
	public void onBlockAdded(World world, int x, int y, int z) {
		world.scheduleBlockUpdate(x, y, z, this.movingId);
	}

	@Override
	public void updateTick(World world, int x, int y, int z, Random random) {
		this.update(world, x, y, z, 0);
	}

	public boolean update(World world, int x, int y, int z, int level) {
		boolean flowing = false;
		world.getBlockMaterial(x, y - 1, z).liquidSolidCheck();

		boolean movedDown;
		do {
			--y;
			if(!this.canFlow(world, x, y, z)) {
				break;
			}
			world.getBlockMaterial(x, y - 1, z).liquidSolidCheck();
			movedDown = world.setBlockWithNotify(x, y, z, this.movingId);
			if(movedDown) {
				flowing = true;
			}
		} while(movedDown && this.blockMaterial != Material.lava);

		++y;
		if(this.blockMaterial == Material.water || !flowing) {
			flowing |= this.flow(world, x - 1, y, z);
			flowing |= this.flow(world, x + 1, y, z);
			flowing |= this.flow(world, x, y, z - 1);
			flowing |= this.flow(world, x, y, z + 1);
		}

		if(this.blockMaterial == Material.lava) {
			flowing |= extinguishFireLava(world, x - 1, y, z);
			flowing |= extinguishFireLava(world, x + 1, y, z);
			flowing |= extinguishFireLava(world, x, y, z - 1);
			flowing |= extinguishFireLava(world, x, y, z + 1);
		}

		if(!flowing) {
			world.setTileNoUpdate(x, y, z, this.stillId);
		} else {
			world.scheduleBlockUpdate(x, y, z, this.movingId);
		}

		return flowing;
	}

	protected final boolean canFlow(World world, int x, int y, int z) {
		if(!world.getBlockMaterial(x, y, z).liquidSolidCheck()) {
			return false;
		} else {
			if(this.blockMaterial == Material.water) {
				for(int scanX = x - 2; scanX <= x + 2; ++scanX) {
					for(int scanY = y - 2; scanY <= y + 2; ++scanY) {
						for(int scanZ = z - 2; scanZ <= z + 2; ++scanZ) {
							if(world.getBlockId(scanX, scanY, scanZ) == Block.sponge.blockID) {
								return false;
							}
						}
					}
				}
			}
			return true;
		}
	}

	private static boolean extinguishFireLava(World world, int x, int y, int z) {
		if(Block.fire.getChanceOfNeighborsEncouragingFire(world.getBlockId(x, y, z))) {
			Block.fire.fireSpread(world, x, y, z);
			return true;
		} else {
			return false;
		}
	}

	private boolean flow(World world, int x, int y, int z) {
		if(!this.canFlow(world, x, y, z)) {
			return false;
		} else {
			boolean setBlock = world.setBlockWithNotify(x, y, z, this.movingId);
			if(setBlock) {
				world.scheduleBlockUpdate(x, y, z, this.movingId);
			}
			return false;
		}
	}

	@Override
	public final float getBlockBrightness(IBlockAccess blockAccess, int x, int y, int z) {
		return this.blockMaterial == Material.lava ? 100.0F : super.getBlockBrightness(blockAccess, x, y, z);
	}

	@Override
	public boolean shouldSideBeRendered(IBlockAccess blockAccess, int x, int y, int z, int side) {
		int neighborBlockID = blockAccess.getBlockId(x, y, z);
		return neighborBlockID != this.movingId && neighborBlockID != this.stillId ? (side != 1 || blockAccess.getBlockId(x - 1, y, z) != 0 && blockAccess.getBlockId(x + 1, y, z) != 0 && blockAccess.getBlockId(x, y, z - 1) != 0 && blockAccess.getBlockId(x, y, z + 1) != 0 ? super.shouldSideBeRendered(blockAccess, x, y, z, side) : true) : false;
	}

	@Override
	public boolean isCollidable() {
		return false;
	}

	@Override
	public AxisAlignedBB getCollisionBoundingBoxFromPool(int x, int y, int z) {
		return null;
	}

	@Override
	public boolean isOpaqueCube() {
		return false;
	}

	@Override
	public void onNeighborBlockChange(World world, int x, int y, int z, int neighborID) {
		if(neighborID != 0) {
			Material neighborMaterial = Block.blocksList[neighborID].blockMaterial;
			if(this.blockMaterial == Material.water && neighborMaterial == Material.lava || neighborMaterial == Material.water && this.blockMaterial == Material.lava) {
				world.setBlockWithNotify(x, y, z, Block.stone.blockID);
			}
		}
		world.scheduleBlockUpdate(x, y, z, this.blockID);
	}

	@Override
	public int tickRate() {
		return this.blockMaterial == Material.lava ? 25 : 5;
	}

	@Override
	public int quantityDropped(Random random) {
		return 0;
	}

	@Override
	public int getRenderBlockPass() {
		return this.blockMaterial == Material.water ? 1 : 0;
	}

	@Override
	public final void randomDisplayTick(World world, int x, int y, int z, Random random) {
		if(random.nextInt(128) == -1 && world.getBlockMaterial(x, y + 1, z).getIsSolid()) {
			if(this.blockMaterial == Material.lava) {
				world.playSoundEffect((double)((float)x + 0.5F), (double)((float)y + 0.5F), (double)((float)z + 0.5F), "liquid.lava", random.nextFloat() * 0.25F + 12.0F / 16.0F, random.nextFloat() * 0.5F + 0.3F);
			}
			if(this.blockMaterial == Material.water) {
				world.playSoundEffect((double)((float)x + 0.5F), (double)((float)y + 0.5F), (double)((float)z + 0.5F), "liquid.water", random.nextFloat() * 0.25F + 12.0F / 16.0F, random.nextFloat() + 0.5F);
			}
		}

		if(this.blockMaterial == Material.lava && world.getBlockMaterial(x, y + 1, z) == Material.air && !world.isSolid(x, y + 1, z) && random.nextInt(100) == 0) {
			double particleX = (double)((float)x + random.nextFloat());
			double particleY = (double)y + this.maxY;
			double particleZ = (double)((float)z + random.nextFloat());
			world.spawnParticle("lava", particleX, particleY, particleZ, 0.0D, 0.0D, 0.0D);
		}

		if(this.blockMaterial == Material.water) {
			int particleIndex;
			if(liquidAirCheck(world, x + 1, y, z)) {
				for(particleIndex = 0; particleIndex < 4; ++particleIndex) {
					world.spawnParticle("splash", (double)((float)(x + 1) + 2.0F / 16.0F), (double)y, (double)((float)z + random.nextFloat()), 0.0D, 0.0D, 0.0D);
				}
			}
			if(liquidAirCheck(world, x - 1, y, z)) {
				for(particleIndex = 0; particleIndex < 4; ++particleIndex) {
					world.spawnParticle("splash", (double)((float)x - 2.0F / 16.0F), (double)y, (double)((float)z + random.nextFloat()), 0.0D, 0.0D, 0.0D);
				}
			}
			if(liquidAirCheck(world, x, y, z + 1)) {
				for(particleIndex = 0; particleIndex < 4; ++particleIndex) {
					world.spawnParticle("splash", (double)((float)x + random.nextFloat()), (double)y, (double)((float)(z + 1) + 2.0F / 16.0F), 0.0D, 0.0D, 0.0D);
				}
			}
			if(liquidAirCheck(world, x, y, z - 1)) {
				for(particleIndex = 0; particleIndex < 4; ++particleIndex) {
					world.spawnParticle("splash", (double)((float)x + random.nextFloat()), (double)y, (double)((float)z - 2.0F / 16.0F), 0.0D, 0.0D, 0.0D);
				}
			}
		}
	}

	private static boolean liquidAirCheck(World world, int x, int y, int z) {
		Material blockMaterial = world.getBlockMaterial(x, y, z);
		Material belowMaterial = world.getBlockMaterial(x, y - 1, z);
		return !blockMaterial.getIsSolid() && !blockMaterial.getIsLiquid() ? belowMaterial.getIsSolid() || belowMaterial.getIsLiquid() : false;
	}
}