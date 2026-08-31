package net.minecraft.game.world;

import java.util.LinkedList;
import java.util.List;

import net.minecraft.game.world.block.Block;

/**
 * Owns the queue of scheduled block ticks for a {@link World} and advances it
 * once per world tick. Blocks that need to react on a delay (fire spreading,
 * water/lava flowing, stationary fluids) register themselves via
 * {@link #scheduleBlockUpdate(int, int, int, int)}; each entry counts down its
 * {@code scheduledTime} every tick until it fires, at which point the block's
 * {@link Block#updateTick} is invoked if the position still holds the same
 * block id.
 *
 * <p>Extracted from {@link World} in the refactor: the list was the badly-named
 * {@code unloadedEntityList} (it has nothing to do with entities). As an
 * instance collaborator it keeps its own mutable queue and the up-to-200-entries
 * per-tick cap, leaving {@link World#tick()} a single call into
 * {@link #updateTicks()}.
 */
final class BlockTickScheduler {
	/** Hard cap on how many pending block ticks are processed in one world tick. */
	private static final int MAX_TICKS_PER_FRAME = 200;

	/** The owning world, used for block lookups and the shared random source. */
	private final World world;
	/** The FIFO of block-tick entries still waiting to fire. */
	private List<NextTickListEntry> pendingBlockTicks;

	BlockTickScheduler(World world) {
		this.world = world;
		this.pendingBlockTicks = new LinkedList<>();
	}

	/**
	 * Queues a block tick for a future update. {@code blockID} is the block id
	 * at this position that should be ticked. If non-zero, the block's own
	 * {@link Block#tickRate()} defines how many ticks to wait.
	 */
	final void scheduleBlockUpdate(int x, int y, int z, int blockID) {
		NextTickListEntry entry = new NextTickListEntry(x, y, z, blockID);
		if(blockID > 0) {
			int delay = Block.blocksList[blockID].tickRate();
			entry.scheduledTime = delay;
		}
		this.pendingBlockTicks.add(entry);
	}

	/**
	 * Advances the scheduled-block-tick queue one world tick. Processes up to
	 * {@link #MAX_TICKS_PER_FRAME} entries: an entry with remaining delay is
	 * re-queued so its countdown continues, and one that is ready fires only if
	 * the block at its position is still the one that scheduled it.
	 */
	final void updateTicks() {
		int ticksToProcess = this.pendingBlockTicks.size();
		if(ticksToProcess > MAX_TICKS_PER_FRAME) {
			ticksToProcess = MAX_TICKS_PER_FRAME;
		}
		for(int i = 0; i < ticksToProcess; ++i) {
			NextTickListEntry entry = this.pendingBlockTicks.remove(0);
			if(entry.scheduledTime > 0) {
				--entry.scheduledTime;
				this.pendingBlockTicks.add(entry);
			} else if(this.world.blockExists(entry.xCoord, entry.yCoord, entry.zCoord)) {
				int currentBlockID = this.world.getBlockId(entry.xCoord, entry.yCoord, entry.zCoord);
				if(currentBlockID == entry.blockID && currentBlockID > 0) {
					Block.blocksList[currentBlockID].updateTick(this.world, entry.xCoord, entry.yCoord, entry.zCoord, this.world.rand);
				}
			}
		}
	}
}
