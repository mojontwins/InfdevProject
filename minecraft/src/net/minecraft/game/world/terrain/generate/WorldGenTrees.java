package net.minecraft.game.world.terrain.generate;

import java.util.Random;
import net.minecraft.game.world.World;
import net.minecraft.game.world.block.Block;

/**
 * Places a small oak-style tree: a straight trunk 4–7 blocks tall, topped
 * with a compact sphere of leaves. This is the generator that appeared in Infdev
 * and was the default tree in a1.1.2 before larger variants were added.
 *
 * <p>The algorithm has three phases:
 * <ol>
 *   <li><b>Collision check</b> — walks the column above the origin and aborts
 *       if any block other than air or leaves is found, or if the column
 *       extends beyond y=127.</li>
 *   <li><b>Ground check</b> — the block directly below the origin must be
 *       plantable (grass or dirt); if so it is replaced with plain dirt so
 *       the tree never floats on a grass block.</li>
 *   <li><b>Placement</b> — a leaf sphere is painted around the top of the
 *       trunk, then the trunk itself is filled below it.</li>
 * </ol>
 *
 * <p>Unlike {@link WorldGenBigTree} this generator uses only one trunk and
 * does not place branch nodes, so it is much cheaper to run and produces a
 * smaller, rounder tree.
 */
public final class WorldGenTrees extends WorldGenerator {

	/**
	 * Generates the tree. Returns {@code false} if the position is unsuitable
	 * (collision or bad ground) so the caller can try elsewhere.
	 *
	 * @return {@code true} if the tree was placed, {@code false} otherwise
	 */
	@Override
	public final boolean generate(World world, Random rand, int x, int y, int z) {
		// Trunk height: 4 + 0..3 = 4–7 blocks.
		int trunkHeight = rand.nextInt(3) + 4;
		boolean canPlace = true;

		// ── Phase 1: collision column ──────────────────────────────────────
		// Verify that every cell from y up to (y + 1 + trunkHeight) is either
		// air or leaves.  Leaves are allowed because they may be present from a
		// neighbouring tree that was generated first.  The top of the trunk
		// (y + 1 + trunkHeight) is where the leaf sphere begins, so that row
		// also participates in the collision walk.
		for(int checkY = y; checkY <= y + 1 + trunkHeight; ++checkY) {
			// The collision check has three horizontal radii depending on row:
			//   checkY == y            → radius 0  (trunk base, no horizontal neighbours to check)
			//   checkY == y+1..y+h-1   → radius 1  (the leaf body, except the very top)
			//   checkY >= y+1+trunkH-2 → radius 2  (the widest part of the leaf ball)
			byte radius;
			if(checkY == y) {
				radius = 0;
			} else if(checkY >= y + 1 + trunkHeight - 2) {
				radius = 2;
			} else {
				radius = 1;
			}

			for(int checkX = x - radius; checkX <= x + radius && canPlace; ++checkX) {
				for(int checkZ = z - radius; checkZ <= z + radius && canPlace; ++checkZ) {
					if(checkY < 0 || checkY >= 128) {
						// Outside the world — abort.
						canPlace = false;
					} else {
						int blockID = world.getBlockId(checkX, checkY, checkZ);
						if(blockID != 0 && blockID != Block.leaves.blockID) {
							// Something solid is in the way.
							canPlace = false;
						}
					}
				}
			}
		}

		if(!canPlace) {
			return false;
		}

		// ── Phase 2: ground check ─────────────────────────────────────────
		// The block directly below the origin must be plantable.  If it is, the
		// generator always converts it to plain dirt — this matches the original
		// behaviour and prevents a tree from sitting on top of a grass block.
		if(!world.canPlantsGrowOn(x, y - 1, z) || y >= 128 - trunkHeight - 1) {
			return false;
		}
		world.setTileNoUpdate(x, y - 1, z, Block.dirt.blockID);

		// ── Phase 3: leaf sphere ──────────────────────────────────────────
		// Grow a roughly spherical blob of leaves centred on the trunk top.
		// The loop walks from the bottom of the leaf ball (trunkHeight-3) to
		// the very top (trunkHeight).  For each horizontal slice the radius of
		// the disc is 1 (centre slices) or 2 (top two slices).
		for(int leafY = y - 3 + trunkHeight; leafY <= y + trunkHeight; ++leafY) {
			int discCentreY = leafY - (y + trunkHeight);           // negative offset from crown centre
			int discRadius = 1 - discCentreY / 2;                 // 1 for centre, 2 for topmost slices

			for(int leafX = x - discRadius; leafX <= x + discRadius; ++leafX) {
				int distX = leafX - x;

				for(int leafZ = z - discRadius; leafZ <= z + discRadius; ++leafZ) {
					int distZ = leafZ - z;

					// Skip the four corner blocks of the widest disc slices (where
					// discRadius==2) half the time, giving the sphere a slightly
					// organic, bumpy edge.  The trunk column itself (distX==0 &&
					// distZ==0) is never a leaf corner so this never affects it.
					if((Math.abs(distX) != discRadius || Math.abs(distZ) != discRadius || rand.nextInt(2) != 0 && discCentreY != 0)
							&& !Block.opaqueCubeLookup[world.getBlockId(leafX, leafY, leafZ)]) {
						world.setTileNoUpdate(leafX, leafY, leafZ, Block.leaves.blockID);
					}
				}
			}
		}

		// ── Phase 4: trunk ───────────────────────────────────────────────
		// Fill the column from y (base) up to but not including y+trunkHeight
		// (the leaf ball starts there).  Any pre-existing air or leaves cells
		// are overwritten with wood.
		for(int trunkY = 0; trunkY < trunkHeight; ++trunkY) {
			int trunkID = world.getBlockId(x, y + trunkY, z);
			if(trunkID == 0 || trunkID == Block.leaves.blockID) {
				world.setTileNoUpdate(x, y + trunkY, z, Block.wood.blockID);
			}
		}

		return true;
	}
}
