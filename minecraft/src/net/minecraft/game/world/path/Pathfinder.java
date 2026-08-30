package net.minecraft.game.world.path;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.game.entity.Entity;
import net.minecraft.game.world.World;
import net.minecraft.game.world.material.Material;
import util.MathHelper;

/**
 * A* pathfinder for creature movement. The search runs over block cells and
 * only ever looks at the four horizontal neighbours of a node (plus optional
 * 1-block descents) - it cannot jump, swim into water, or squeeze through gaps
 * narrower than the entity's footprint.
 *
 * <p>The A* bookkeeping lives on {@link PathPoint}: the {@link Path} field is
 * the open list (a min-heap keyed on F = G + H), {@link #pointMap} maps each
 * coordinate to its single node instance so the heap can be re-weighted in
 * place, and {@link #pathOptions} is a scratch buffer for the neighbour cells
 * of the node being expanded. If the target turns out to be unreachable, the
 * search still returns a partial path to whatever reachable node came closest.
 */
public final class Pathfinder {
	private World world;
	private Path path = new Path();
	private Map<Integer, PathPoint> pointMap = new HashMap<>();
	private PathPoint[] pathOptions = new PathPoint[32];

	public Pathfinder(World world) {
		this.world = world;
	}

	/**
	 * Plans a path from {@code entity} to {@code target}. The {@code range}
	 * argument is historic and unused - the search always caps candidates at
	 * {@code 16.0F} blocks from the target.
	 */
	public final PathEntity createEntityPathTo(Entity entity, Entity target, float range) {
		return this.addToPath(entity, target.posX, target.boundingBox.minY, target.posZ, 16.0F);
	}

	/** Plans a path from {@code entity} to the centre of the given block cell. */
	public final PathEntity createEntityPathToXYZ(Entity entity, int targetX, int targetY, int targetZ, float range) {
		return this.addToPath(entity, (double)((float)targetX + 0.5F), (double)((float)targetY + 0.5F), (double)((float)targetZ + 0.5F), 16.0F);
	}

	/**
	 * Runs one A* search from the entity's current cell to {@code targetX/Y/Z}.
	 * The entity's own footprint is captured as {@code sizePoint} (width+1 by
	 * height+1) and used to test whether cells around it are passable.
	 */
	private PathEntity addToPath(Entity entity, double targetX, double targetY, double targetZ, float range) {
		this.path.clearPath();
		this.pointMap.clear();
		PathPoint startPoint = this.openPoint(MathHelper.floor_double(entity.boundingBox.minX), MathHelper.floor_double(entity.boundingBox.minY), MathHelper.floor_double(entity.boundingBox.minZ));
		PathPoint targetPoint = this.openPoint(MathHelper.floor_double(targetX - (double)(entity.width / 2.0F)), MathHelper.floor_double(targetY), MathHelper.floor_double(targetZ - (double)(entity.width / 2.0F)));
		PathPoint sizePoint = new PathPoint(MathHelper.floor_float(entity.width + 1.0F), MathHelper.floor_float(entity.height + 1.0F), MathHelper.floor_float(entity.width + 1.0F));
		float maxDistance = range;

		startPoint.totalPathDistance = 0.0F;
		startPoint.distanceToNext = startPoint.distanceTo(targetPoint);
		startPoint.distanceToTarget = startPoint.distanceToNext;
		this.path.addPoint(startPoint);
		PathPoint closestPoint = startPoint;

		while(true) {
			if(this.path.isPathEmpty()) {
				// No reachable node left: return the best partial path, or null when
				// the start cell itself was never even expanded.
				return closestPoint == startPoint ? null : createEntityPath(closestPoint);
			}

			PathPoint current = this.path.dequeue();
			if(current.hash == targetPoint.hash) {
				return createEntityPath(targetPoint);
			}

			if(current.distanceTo(targetPoint) < closestPoint.distanceTo(targetPoint)) {
				closestPoint = current;
			}

			current.isFirst = true;

			// The cell straight above the node is open, neighbours may also sit one
			// block higher - this is what lets the path climb single steps.
			byte stepUp = 0;
			if(this.getVerticalOffset(current.xCoord, current.yCoord + 1, current.zCoord, sizePoint) > 0) {
				stepUp = 1;
			}

			// Gather the four sideways neighbours in the original order (+Z, -X, +X, -Z).
			// Each is only a candidate while it stays inside maxDistance of the target.
			int optionCount = 0;
			PathPoint candidatePosZ = this.getSafePoint(entity, current.xCoord, current.yCoord, current.zCoord + 1, sizePoint, stepUp);
			PathPoint candidateNegX = this.getSafePoint(entity, current.xCoord - 1, current.yCoord, current.zCoord, sizePoint, stepUp);
			PathPoint candidatePosX = this.getSafePoint(entity, current.xCoord + 1, current.yCoord, current.zCoord, sizePoint, stepUp);
			PathPoint candidateNegZ = this.getSafePoint(entity, current.xCoord, current.yCoord, current.zCoord - 1, sizePoint, stepUp);
			if(candidatePosZ != null && !candidatePosZ.isFirst && candidatePosZ.distanceTo(targetPoint) < maxDistance) {
				this.pathOptions[optionCount++] = candidatePosZ;
			}
			if(candidateNegX != null && !candidateNegX.isFirst && candidateNegX.distanceTo(targetPoint) < maxDistance) {
				this.pathOptions[optionCount++] = candidateNegX;
			}
			if(candidatePosX != null && !candidatePosX.isFirst && candidatePosX.distanceTo(targetPoint) < maxDistance) {
				this.pathOptions[optionCount++] = candidatePosX;
			}
			if(candidateNegZ != null && !candidateNegZ.isFirst && candidateNegZ.distanceTo(targetPoint) < maxDistance) {
				this.pathOptions[optionCount++] = candidateNegZ;
			}

			// Relax each surviving candidate: a shorter route through `current`
			// replaces its stored one and re-weights it inside the open list.
			for(int option = 0; option < optionCount; ++option) {
				PathPoint candidate = this.pathOptions[option];
				float newTotalDistance = current.totalPathDistance + current.distanceTo(candidate);
				if(!candidate.isAssigned() || newTotalDistance < candidate.totalPathDistance) {
					candidate.previous = current;
					candidate.totalPathDistance = newTotalDistance;
					candidate.distanceToNext = candidate.distanceTo(targetPoint);
					if(candidate.isAssigned()) {
						this.path.changeDistance(candidate, candidate.totalPathDistance + candidate.distanceToNext);
					} else {
						candidate.distanceToTarget = candidate.totalPathDistance + candidate.distanceToNext;
						this.path.addPoint(candidate);
					}
				}
			}
		}
	}

	/**
	 * Looks for a walkable cell near (x, y, z): first the cell itself, then - if
	 * that is blocked - the cell {@code stepExtra} blocks higher. Once a cell is
	 * found, it is slid down up to four steps to the floor of any drop, so the
	 * path does not float in mid-air; cells whose floor is water or lava are
	 * rejected (the entity would drown/burn, not walk).
	 */
	private PathPoint getSafePoint(Entity entity, int x, int y, int z, PathPoint size, int stepExtra) {
		PathPoint safePoint = null;
		if(this.getVerticalOffset(x, y, z, size) > 0) {
			safePoint = this.openPoint(x, y, z);
		}

		if(safePoint == null && this.getVerticalOffset(x, y + stepExtra, z, size) > 0) {
			safePoint = this.openPoint(x, y + stepExtra, z);
		}

		if(safePoint != null) {
			int stepCount = 0;

			while(true) {
				if(y > 0) {
					int dropBelow = this.getVerticalOffset(x, y - 1, z, size);
					if(dropBelow > 0) {
						if(++stepCount >= 4) {
							return null;
						}

						--y;
						safePoint = this.openPoint(x, y, z);
						continue;
					}
				}

				Material floorMaterial = this.world.getBlockMaterial(x, y - 1, z);
				if(floorMaterial == Material.water || floorMaterial == Material.lava) {
					return null;
				}
				break;
			}
		}

		return safePoint;
	}

	/**
	 * Returns the single cached node for a cell, creating it on first use so
	 * that every open-point and safe-point of the same coordinate is one shared
	 * instance (which is what lets the heap re-weight nodes in place).
	 */
	private final PathPoint openPoint(int x, int y, int z) {
		int hash = x | y << 10 | z << 20;
		PathPoint point = this.pointMap.get(hash);
		if(point == null) {
			point = new PathPoint(x, y, z);
			this.pointMap.put(hash, point);
		}

		return point;
	}

	/**
	 * Classifies the single cell at (x, y, z): 1 = open (walkable), 0 = solid
	 * (impassable), -1 = water or lava. It is called with {@code size} set to
	 * the entity's footprint and the enclosing loops still iterate that whole
	 * volume - but every iteration reads the very same (x, y, z) cell, so the
	 * loops are dead scaffolding that is kept verbatim for fidelity with the
	 * original 2010 code.
	 */
	private int getVerticalOffset(int x, int y, int z, PathPoint size) {
		for(int scanX = x; scanX < x + size.xCoord; ++scanX) {
			for(int scanY = y; scanY < y + size.yCoord; ++scanY) {
				for(int scanZ = z; scanZ < z + size.zCoord; ++scanZ) {
					Material material = this.world.getBlockMaterial(x, y, z);
					if(material.getIsSolid()) {
						return 0;
					}

					if(material == Material.water || material == Material.lava) {
						return -1;
					}
				}
			}
		}

		return 1;
	}

	/** Walks the {@code previous} chain back from the end point, then reverses it into the point array of a {@link PathEntity}. */
	private static PathEntity createEntityPath(PathPoint endPoint) {
		int pathLength = 1;

		PathPoint current;
		for(current = endPoint; current.previous != null; current = current.previous) {
			++pathLength;
		}

		PathPoint[] points = new PathPoint[pathLength];
		current = endPoint;
		--pathLength;

		for(points[pathLength] = endPoint; current.previous != null; points[pathLength] = current) {
			current = current.previous;
			--pathLength;
		}

		return new PathEntity(points);
	}
}