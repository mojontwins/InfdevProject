package net.minecraft.game.world.block;

import java.util.Random;
import net.minecraft.game.world.World;
import net.minecraft.game.world.material.Material;

/**
 * The "still" role of a liquid (waterStill id 9, lavaStill id 11): the quiet,
 * settled form that worldgen and flowing cells hand back to. It never spreads
 * on its own — a still ocean stays put — but a neighbour change (an edge just
 * opened up, or another liquid arrived) lets it hand control back to the moving
 * role via {@link #wakeToMoving}, which is {@link BlockFlowing}'s favourite
 * job. A still block of lava additionally keeps a slow smoulder tick so it can
 * ignite flammable neighbours up to a few cells above its surface.
 */
public final class BlockStationary extends BlockFluid {
	protected BlockStationary(int blockID, Material material) {
		super(blockID, material);
		this.movingId = blockID - 1;
		this.stillId = blockID;
		// Ocean water needs no upkeep; lava needs to prod things alight.
		this.setTickOnLoad(material == Material.lava);
	}

	@Override
	public final void onNeighborBlockChange(World world, int x, int y, int z, int neighborID) {
		super.onNeighborBlockChange(world, x, y, z, neighborID);
		if(world.getBlockId(x, y, z) == this.blockID) {
			this.wakeToMoving(world, x, y, z);
		}
	}

	/** Re-arms a settled cell as a flowing one so it can react to this change. */
	private void wakeToMoving(World world, int x, int y, int z) {
		int decay = world.getBlockMetadata(x, y, z);
		world.setBlockAndMetadata(x, y, z, this.movingId, decay);
		world.markBlocksDirty(x, y, z, x, y, z);
		world.scheduleBlockUpdate(x, y, z, this.movingId);
	}

	@Override
	public final void updateTick(World world, int x, int y, int z, Random random) {
		if(this.blockMaterial != Material.lava) {
			return;
		}

		// Stationary lava smoulders: take a short upward walk and ignite any
		// flammable neighbour it finds sitting in open air.
		int steps = random.nextInt(3);
		for(int step = 0; step < steps; ++step) {
			x += random.nextInt(3) - 1;
			++y;
			z += random.nextInt(3) - 1;
			if(world.getBlockId(x, y, z) == 0) {
				if(this.isFlammable(world, x - 1, y, z)
					|| this.isFlammable(world, x + 1, y, z)
					|| this.isFlammable(world, x, y, z - 1)
					|| this.isFlammable(world, x, y, z + 1)
					|| this.isFlammable(world, x, y - 1, z)
					|| this.isFlammable(world, x, y + 1, z)) {
					world.setBlockWithNotify(x, y, z, Block.fire.blockID);
					return;
				}
			} else if(Block.blocksList[world.getBlockId(x, y, z)].blockMaterial.getIsSolid()) {
				return;
			}
		}
	}

	/** This version has no material "burning" flag, so the fire-spread table stands in. */
	private boolean isFlammable(World world, int x, int y, int z) {
		return Block.fire.getChanceOfNeighborsEncouragingFire(world.getBlockId(x, y, z));
	}
}