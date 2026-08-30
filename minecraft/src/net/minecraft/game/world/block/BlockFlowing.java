package net.minecraft.game.world.block;

import java.util.Arrays;
import java.util.Random;
import net.minecraft.game.world.World;
import net.minecraft.game.world.material.Material;

/**
 * The "moving" role of a liquid (waterMoving id 8, lavaMoving id 10): the cell
 * that is still spreading. Its metadata is the <em>flow decay</em> — how many
 * levels this cell sits below its nearest source (a source being decay 0, and
 * decay + 8 marking a column that is falling vertically).
 *
 * <p>Each tick a cell re-derives its decay from the four horizontal neighbours
 * ({@link #getSmallestFlowDecay}): a cell whose cheapest route is exhausted (or
 * runs below eight levels) dries up into air at the next opportunity, a cell
 * directly under a falling column inherits that column's level, and a cell
 * flanked by two sources regenerates into a source of its own when it has solid
 * ground (or another source) below. Then it pushes one cell of decay
 * {@code decay + 1} into any open cell below and, when standing on a solid
 * floor, into the cheapest sideways directions (the {@link #calculateFlowCost}
 * walk chooses directions that reach open air over the shortest distance).
 * When nothing moved, the cell converts itself into the still role
 * ({@link #settleToStill}); a settled cell wakes up again on a neighbour change
 * (see {@link BlockStationary#onNeighborBlockChange}).
 *
 * <p>Lava moves slower: one extra decay level per step, and a spreading edge
 * holds position three times out of four, so it sputters and bubbles instead of
 * flooding. (This is the b1.7.3 flowing-liquids engine, backported onto the
 * earlier block set.)
 */
public final class BlockFlowing extends BlockFluid {
	/** Horizontal neighbour offsets for the four directions 0..3 = -X, +X, -Z, +Z. */
	private static final int[][] HORIZONTAL_OFFSETS = {
		{-1, 0}, {1, 0}, {0, -1}, {0, 1}
	};

	/** Number of adjacent cells that are full sources (decay 0), tallied while rebreathing. */
	private int adjacentSourceCount;
	/** Cheapest route-to-open-air cost per direction, and the directions that are on it. */
	private final int[] flowCost = new int[4];
	private final boolean[] optimalFlowDirection = new boolean[4];

	protected BlockFlowing(int blockID, Material material) {
		super(blockID, material);
	}

	@Override
	public final void onBlockAdded(World world, int x, int y, int z) {
		super.onBlockAdded(world, x, y, z);
		// A freshly placed or flowed-in cell must run its own spread pass.
		if(world.getBlockId(x, y, z) == this.blockID) {
			world.scheduleBlockUpdate(x, y, z, this.blockID);
		}
	}

	@Override
	public final void updateTick(World world, int x, int y, int z, Random random) {
		int decay = this.getFlowDecay(world, x, y, z);
		// Lava's surface drops one level per two decay steps: it creeps.
		int decayStep = this.blockMaterial == Material.lava ? 2 : 1;

		boolean canSettle = true;
		if(decay > 0) {
			// Rebreathe from the four neighbours: the re-derived decay is the cheapest
			// neighbour level plus one step. An exhausted route (no neighbour) or a
			// level eight deep dooms the cell.
			this.adjacentSourceCount = 0;
			int smallestNeighborDecay = this.getSmallestFlowDecay(world, x - 1, y, z, -100);
			smallestNeighborDecay = this.getSmallestFlowDecay(world, x + 1, y, z, smallestNeighborDecay);
			smallestNeighborDecay = this.getSmallestFlowDecay(world, x, y, z - 1, smallestNeighborDecay);
			smallestNeighborDecay = this.getSmallestFlowDecay(world, x, y, z + 1, smallestNeighborDecay);
			int newDecay = smallestNeighborDecay + decayStep;
			if(newDecay >= 8 || smallestNeighborDecay < 0) {
				newDecay = -1;
			}

			// A falling column directly above feeds this cell without decay limits:
			// its level (marked +8 as "falling") passes straight through.
			int decayAbove = this.getFlowDecay(world, x, y + 1, z);
			if(decayAbove >= 0) {
				newDecay = decayAbove >= 8 ? decayAbove : decayAbove + 8;
			}

			// Two adjacent sources regenerate this cell into a source of its own,
			// provided it rests on solid ground or floats on another source.
			if(this.adjacentSourceCount >= 2 && this.blockMaterial == Material.water) {
				if(world.getBlockMaterial(x, y - 1, z).isSolid()
					|| (world.getBlockMaterial(x, y - 1, z) == this.blockMaterial && world.getBlockMetadata(x, y, z) == 0)) {
					newDecay = 0;
				}
			}

			// Lava holds position (instead of slumping toward a deeper-looking
			// neighbour) three times out of four, so it sputters at the edge.
			if(this.blockMaterial == Material.lava && decay < 8 && newDecay < 8 && newDecay > decay && random.nextInt(4) != 0) {
				newDecay = decay;
				canSettle = false;
			}

			if(newDecay != decay) {
				decay = newDecay;
				if(newDecay < 0) {
					// Nothing feeds this cell anymore: it evaporates entirely.
					world.setBlockWithNotify(x, y, z, 0);
				} else {
					world.setBlockMetadataWithNotify(x, y, z, newDecay);
					world.scheduleBlockUpdate(x, y, z, this.blockID);
					world.notifyBlocksOfNeighborChange(x, y, z, this.blockID);
				}
			} else if(canSettle) {
				this.settleToStill(world, x, y, z);
			}
		} else {
			// A source always goes quiet on its first tick...
			this.settleToStill(world, x, y, z);
		}

		// Downhill first: a falling column (or a partial one) pours into the open
		// cell right below. If the ground already blocks, spread sideways along
		// the cheapest directions found by the cost walk.
		if(this.liquidCanDisplaceBlock(world, x, y - 1, z)) {
			world.setBlockAndMetadataWithNotify(x, y - 1, z, this.blockID, decay >= 8 ? decay : decay + 8);
		} else if(decay >= 0 && (decay == 0 || this.blockBlocksFlow(world, x, y - 1, z))) {
			int spreadDecay = decay + decayStep;
			if(decay >= 8) {
				spreadDecay = 1;
			}
			if(spreadDecay >= 8) {
				return;
			}

			boolean[] optimal = this.getOptimalFlowDirections(world, x, y, z);
			for(int direction = 0; direction < 4; ++direction) {
				if(optimal[direction]) {
					int[] offset = HORIZONTAL_OFFSETS[direction];
					this.flowIntoBlock(world, x + offset[0], y, z + offset[1], spreadDecay);
				}
			}
		}
	}

	/** Converts a quiet cell into the still role, keeping its decay. */
	private void settleToStill(World world, int x, int y, int z) {
		world.setBlockAndMetadata(x, y, z, this.stillId, world.getBlockMetadata(x, y, z));
		world.markBlocksDirty(x, y, z, x, y, z);
	}

	/** Flows liquid into an open cell: loot is dropped (or lava fizzes) and the write goes out. */
	private void flowIntoBlock(World world, int x, int y, int z, int decay) {
		if(this.liquidCanDisplaceBlock(world, x, y, z)) {
			Block occupied = Block.blocksList[world.getBlockId(x, y, z)];
			if(occupied != null) {
				if(this.blockMaterial == Material.lava) {
					this.triggerLavaMixEffects(world, x, y, z);
				} else {
					occupied.dropBlockAsItem(world, x, y, z, world.getBlockMetadata(x, y, z));
				}
			}
			world.setBlockAndMetadataWithNotify(x, y, z, this.blockID, decay);
		}
	}

	/**
	 * Cost of a direction: how far (in steps) a surface cell would have to travel
	 * before meeting open air — 0 when it can drop immediately, 1-3 after the
	 * recursion, 1000 when it is boxed in. The two-source regeneration rule makes
	 * a full source count as open air.
	 */
	private int calculateFlowCost(World world, int x, int y, int z, int distance, int fromDirection) {
		int bestCost = 1000;
		for(int direction = 0; direction < 4; ++direction) {
			// Never walk straight back through the cell we came from.
			if(direction == (fromDirection ^ 1)) {
				continue;
			}
			int[] offset = HORIZONTAL_OFFSETS[direction];
			int neighborX = x + offset[0];
			int neighborZ = z + offset[1];
			if(!this.blockBlocksFlow(world, neighborX, y, neighborZ)
				&& (world.getBlockMaterial(neighborX, y, neighborZ) != this.blockMaterial
					|| world.getBlockMetadata(neighborX, y, neighborZ) != 0)) {
				if(!this.blockBlocksFlow(world, neighborX, y - 1, neighborZ)) {
					return distance;
				}
				if(distance < 4) {
					int branchCost = this.calculateFlowCost(world, neighborX, y, neighborZ, distance + 1, direction);
					if(branchCost < bestCost) {
						bestCost = branchCost;
					}
				}
			}
		}
		return bestCost;
	}

	/** The cheapest sideways routes out of a grounded surface cell. */
	private boolean[] getOptimalFlowDirections(World world, int x, int y, int z) {
		for(int direction = 0; direction < 4; ++direction) {
			this.flowCost[direction] = 1000;
			int[] offset = HORIZONTAL_OFFSETS[direction];
			int neighborX = x + offset[0];
			int neighborZ = z + offset[1];
			if(!this.blockBlocksFlow(world, neighborX, y, neighborZ)
				&& (world.getBlockMaterial(neighborX, y, neighborZ) != this.blockMaterial
					|| world.getBlockMetadata(neighborX, y, neighborZ) != 0)) {
				if(!this.blockBlocksFlow(world, neighborX, y - 1, neighborZ)) {
					this.flowCost[direction] = 0;
				} else {
					this.flowCost[direction] = this.calculateFlowCost(world, neighborX, y, neighborZ, 1, direction);
				}
			}
		}

		int minimumCost = Arrays.stream(this.flowCost).min().getAsInt();
		for(int direction = 0; direction < 4; ++direction) {
			this.optimalFlowDirection[direction] = this.flowCost[direction] == minimumCost;
		}
		return this.optimalFlowDirection;
	}

	/**
	 * Whether a cell forms an unbroken body that liquid cannot push past. Only the
	 * opaque materials count (so an open-faced plant or empty air washes away,
	 * exactly as b1.7.3 treats them; this block set has no ladder/reed/door
	 * exceptions to carry over).
	 */
	private boolean blockBlocksFlow(World world, int x, int y, int z) {
		int blockID = world.getBlockId(x, y, z);
		if(blockID == 0) {
			return false;
		}
		Block block = Block.blocksList[blockID];
		if(block == null) {
			return false;
		}
		return block.blockMaterial.getIsSolid();
	}

	protected int getSmallestFlowDecay(World world, int x, int y, int z, int bestDecay) {
		int decay = this.getFlowDecay(world, x, y, z);
		if(decay < 0) {
			return bestDecay;
		}
		if(decay == 0) {
			++this.adjacentSourceCount;
		}
		if(decay >= 8) {
			decay = 0;
		}
		return bestDecay >= 0 && decay >= bestDecay ? bestDecay : decay;
	}

	/**
	 * Whether liquid may flow into a cell in its path: not into the same liquid
	 * (one liquid only fills its own cell), never into lava, and only past the
	 * pass-through materials (air, plants, fire — anything {@link #blockBlocksFlow}
	 * lets through).
	 */
	private boolean liquidCanDisplaceBlock(World world, int x, int y, int z) {
		Material material = world.getBlockMaterial(x, y, z);
		return material != this.blockMaterial && material != Material.lava && !this.blockBlocksFlow(world, x, y, z);
	}
}