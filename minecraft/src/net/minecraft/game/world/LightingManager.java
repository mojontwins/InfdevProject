package net.minecraft.game.world;

import java.util.ArrayList;
import java.util.List;

/**
 * Owns the deferred light-update queue for a {@link World}. Whenever the block
 * layout changes (placing/removing blocks, height-map edits in a chunk), the
 * affected region is enqueued as a rectangular {@link MetadataChunkBlock} box;
 * the queue is drained in the render loop (see
 * {@link net.minecraft.client.Minecraft}) so lighting stays correct while
 * spreading the work over several frames.
 *
 * <p>Extracted from {@link World} in the refactor: the box queue made up most
 * of {@code World}'s lighting bookkeeping, so moving it here keeps the queue,
 * its cap constants and the drain/merge logic together. {@code World} keeps its
 * public {@code scheduleLightingUpdate}/{@code updatingLighting}/
 * {@code lightUpdatesNeeded} entry points as thin delegates, so external
 * callers (chunks, the render loop) are unchanged.
 */
final class LightingManager {
	/** Max boxes processed by a single {@link #updatingLighting()} call. */
	private static final int MAX_LIGHTING_BOXES_PER_CALL = 100000;
	/** Enqueueing pauses to drain once the backlog reaches this size. */
	private static final int MAX_LIGHTING_QUEUE_SIZE = 1000000;
	/** Drain target size after a paused queue-overflow flush. */
	private static final int LIGHTING_QUEUE_DRAIN_SIZE = 500000;

	/** The owning world, used for re-lighting each box as it is popped. */
	private final World world;
	/** Rectangular light-update regions still waiting to be processed. */
	private final List<MetadataChunkBlock> lightingToUpdate = new ArrayList<>();

	LightingManager(World world) {
		this.world = world;
	}

	/** Number of pending light-update boxes waiting to be processed. */
	final int lightUpdatesNeeded() {
		return this.lightingToUpdate.size();
	}

	/**
	 * Pops pending light-update boxes off the back of the queue and processes
	 * them, up to {@link #MAX_LIGHTING_BOXES_PER_CALL} per call. Returns true
	 * if the budget was hit (caller should call again next tick).
	 */
	final boolean updatingLighting() {
		int remainingBudget = MAX_LIGHTING_BOXES_PER_CALL;

		while(this.lightingToUpdate.size() > 0) {
			if(--remainingBudget <= 0) {
				return true;
			}
			MetadataChunkBlock box = this.lightingToUpdate.remove(this.lightingToUpdate.size() - 1);
			box.updateLight(this.world);
		}
		return false;
	}

	/**
	 * Schedules a light-update box. Tries to merge into the four most recent
	 * entries of the same light type to keep the queue short. If the queue
	 * grows beyond {@link #MAX_LIGHTING_QUEUE_SIZE}, half is drained by
	 * processing on the calling thread.
	 */
	final void scheduleLightingUpdate(EnumSkyBlock lightType, int x1, int y1, int z1, int x2, int y2, int z2) {
		if(x1 > x2 || y1 > y2 || z1 > z2) {
			return;
		}
		int queueSize = this.lightingToUpdate.size();
		int scanCount = Math.min(4, queueSize);
		for(int i = 0; i < scanCount; ++i) {
			MetadataChunkBlock box = this.lightingToUpdate.get(queueSize - i - 1);
			if(box.lightType == lightType && box.tryMerge(x1, y1, z1, x2, y2, z2)) {
				return;
			}
		}
		this.lightingToUpdate.add(new MetadataChunkBlock(lightType, x1, y1, z1, x2, y2, z2));
		if(this.lightingToUpdate.size() > MAX_LIGHTING_QUEUE_SIZE) {
			while(this.lightingToUpdate.size() > LIGHTING_QUEUE_DRAIN_SIZE) {
				this.updatingLighting();
			}
		}
	}
}
