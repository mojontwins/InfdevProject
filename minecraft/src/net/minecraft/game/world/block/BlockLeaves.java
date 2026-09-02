package net.minecraft.game.world.block;

import java.util.Random;
import net.minecraft.game.item.Item;
import net.minecraft.game.item.ItemStack;
import net.minecraft.game.world.World;
import net.minecraft.game.world.material.Material;
import net.minecraft.game.world.terrain.generate.EnumTreeType;

/**
 * Leaves: the crown blocks of a tree. They decay when cut off from their trunk.
 *
 * <p>The decay protocol follows beta 1.7.3. Removing a log (see
 * {@link BlockLog#onBlockRemoval}) stamps the {@link #DECAY_CHECK_BIT} onto every
 * leaf in a 4-block box around it. When such a marked leaf next ticks, it
 * builds a 9&times;9&times;9 picture of its neighbourhood — logs are distance
 * 0, leaves are passable, anything else is a wall — and flood-fills outward from
 * each log for at most four steps. A leaf that turns out to be no farther than
 * four leaf-to-leaf hops from a log keeps its place (the mark is cleared); a
 * leaf with no log in range is dropped and removed.
 *
 * <p>The scratch grid is a flat, padded {@code int[]} so the flood-fill walks
 * by adding a constant stride rather than recomputing indices, and the relative
 * position of every probe cell (its world offset and its grid index) is
 * precomputed once at class load into {@link #PROBE_DX}/{@link #PROBE_DY}/
 * {@link #PROBE_DZ}/{@link #PROBE_CURSOR}.
 */
public final class BlockLeaves extends BlockLeavesBase implements IBlockWithSubtypes {
	public static final int OAK = 0;

	/** Metadata bit a log stamps onto a leaf to make it re-verify its trunk next tick. */
	public static final int DECAY_CHECK_BIT = 8;

	// --- Decay search geometry -------------------------------------------------
	// The 9x9x9 search box sits inside an 11x11x11 padded grid. The one-cell pad
	// lets every +-1 neighbour stride land on a real slot, so the inner flood-fill
	// loop needs no bounds checks.
	private static final int RADIUS = 4;                 // connectivity distance to a log
	private static final int SPAN = RADIUS * 2 + 1;      // 9 cells per axis
	private static final int GRID = SPAN + 2;            // 11 (padded dimension)
	private static final int GRID_AREA = GRID * GRID;    // 121
	private static final int HALF = GRID >> 1;           // 5 (centre offset)
	private static final int CENTER = HALF * GRID_AREA + HALF * GRID + HALF; // 665
	private static final int CELL_COUNT = SPAN * SPAN * SPAN; // 729

	/** The six +-1 neighbour strides, in x/y/z pairs. */
	private static final int[] NEIGHBOR_STRIDES = { GRID_AREA, -GRID_AREA, GRID, -GRID, 1, -1 };

	// Precomputed per-probe-cell tables: the block offset (dx/dy/dz) and the flat
	// grid cursor for each of the 729 cells, in scan order (x-major, z-minor).
	private static final int[] PROBE_DX = new int[CELL_COUNT];
	private static final int[] PROBE_DY = new int[CELL_COUNT];
	private static final int[] PROBE_DZ = new int[CELL_COUNT];
	private static final int[] PROBE_CURSOR = new int[CELL_COUNT];

	static {
		int i = 0;
		for(int dx = -RADIUS; dx <= RADIUS; ++dx) {
			for(int dy = -RADIUS; dy <= RADIUS; ++dy) {
				for(int dz = -RADIUS; dz <= RADIUS; ++dz) {
					PROBE_DX[i] = dx;
					PROBE_DY[i] = dy;
					PROBE_DZ[i] = dz;
					PROBE_CURSOR[i] = (dx + HALF) * GRID_AREA + (dy + HALF) * GRID + (dz + HALF);
					++i;
				}
			}
		}
	}

	/** Scratch grid reused across ticks (the block is a singleton, so one array serves). */
	private final int[] adjacency = new int[GRID * GRID * GRID];

	protected BlockLeaves(int blockID, int textureIndex) {
		super(blockID, textureIndex, Material.leaves);
		this.setTickOnLoad(true);
	}

	@Override
	public final void updateTick(World world, int x, int y, int z, Random random) {
		int metadata = world.getBlockMetadata(x, y, z);
		// Only a leaf a removed log has flagged needs re-checking; the rest stay put.
		if((metadata & DECAY_CHECK_BIT) == 0) {
			return;
		}

		// Wait for every corner chunk so a half-loaded world never drops a leaf the
		// generator hasn't finished. The box is one larger than the search radius.
		int extent = RADIUS + 1;
		if(!world.checkChunksExist(x - extent, y - extent, z - extent, x + extent, y + extent, z + extent)) {
			return;
		}

		// Classify the neighbourhood: 0 = log, -2 = leaves, -1 = anything else.
		for(int i = 0; i < CELL_COUNT; ++i) {
			int id = world.getBlockId(x + PROBE_DX[i], y + PROBE_DY[i], z + PROBE_DZ[i]);
			adjacency[PROBE_CURSOR[i]] = id == Block.wood.blockID ? 0 : (id == Block.leaves.blockID ? -2 : -1);
		}

		// Flood from each log (distance 0) outward through leaves, at most RADIUS steps.
		for(int distance = 1; distance <= RADIUS; ++distance) {
			for(int i = 0; i < CELL_COUNT; ++i) {
				int cursor = PROBE_CURSOR[i];
				if(adjacency[cursor] == distance - 1) {
					for(int stride : NEIGHBOR_STRIDES) {
						int neighbor = cursor + stride;
						if(adjacency[neighbor] == -2) {
							adjacency[neighbor] = distance;
						}
					}
				}
			}
		}

		if(adjacency[CENTER] >= 0) {
			// Still tethered to a log: keep the leaves and clear the re-check mark.
			world.setBlockAndMetadata(x, y, z, this.blockID, metadata & ~DECAY_CHECK_BIT);
		} else {
			this.removeLeaves(world, x, y, z);
		}
	}

	/** Drops the leaf's usual drops and replaces it with air. */
	private void removeLeaves(World world, int x, int y, int z) {
		this.dropBlockAsItem(world, x, y, z, world.getBlockMetadata(x, y, z));
		world.setBlockWithNotify(x, y, z, 0);
	}

	@Override
	public final int getBlockTextureFromSideAndMetadata(int side, int metadata) {
		return this.blockIndexInTexture;
	}

	@Override
	public final int quantityDropped(Random random) {
		return random.nextInt(10) == 0 ? 1 : 0;
	}

	@Override
	public final ItemStack itemStackDropped(int metadata, Random random) {
		if(random.nextInt(50) == 0) {
			return new ItemStack(Item.apple.shiftedIndex, 1, 0);
		} else if(random.nextInt(10) == 0) {
			return new ItemStack(Item.stick.shiftedIndex, 1, 0);
		}

		EnumTreeType tree = EnumTreeType.findTreeTypeFromLeaves(this.blockID, metadata);
		BlockState sapling = tree.getSapling();
		return new ItemStack(sapling.getBlockID(), 1, sapling.getMetadata());
	}
}