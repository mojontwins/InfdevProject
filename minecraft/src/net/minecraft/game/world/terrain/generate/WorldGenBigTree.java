package net.minecraft.game.world.terrain.generate;

import java.util.Random;
import net.minecraft.game.world.World;
import net.minecraft.game.world.block.Block;
import util.MathHelper;

/**
 * Places a large, branching tree: a central trunk, a set of outward-splayed
 * branch trunks, and a rounded crown of leaves on top. The whole shape is
 * generated procedurally from a small handful of parameters, using the noise
 * approach that Minecraft originally used before the leaf-canopy generators of
 * later versions.
 *
 * <p>The generator works in two passes: it first collects a list of "leaf
 * nodes" (branch tips, each describing where a branch trunk must be drawn and
 * where a little disc of leaves must be grown), then it places the trunk and
 * branch wood and finally paints the leaf discs.
 */
public final class WorldGenBigTree extends WorldGenerator {
	private static final byte[] otherCoordPairs = new byte[]{(byte) 2, (byte) 0, (byte) 0, (byte) 1, (byte) 2, (byte) 1};
	private Random rand = new Random();
	private World worldObj;
	private int[] basePos = new int[]{0, 0, 0};
	private int heightLimit = 0;
	private int height;
	private double heightAttenuation = 0.618D;
	private double branchSlope = 0.381D;
	private double scaleWidth = 1.0D;
	private double leafDensity = 1.0D;
	private int trunkSize = 1;
	private int heightLimitLimit = 12;
	private int leafDistanceLimit = 4;
	private int[][] leafNodes;

	/**
	 * Draws a straight 3D line of solid wood from {@code start} to {@code end}.
	 * The axis with the largest displacement is stepped along and the other two
	 * coordinates are interpolated to match, giving a smooth diagonal of blocks.
	 * (The {@code blockId} argument of the original signature was never passed
	 * through and has been removed — this always places wood.)
	 */
	private void placeBlockLine(int[] start, int[] end) {
		int[] delta = new int[]{0, 0, 0};
		int maxAxis = 0;
		for(int axis = 0; axis < 3; ++axis) {
			delta[axis] = end[axis] - start[axis];
			if(Math.abs(delta[axis]) > Math.abs(delta[maxAxis])) {
				maxAxis = axis;
			}
		}

		if(delta[maxAxis] != 0) {
			byte axisB = otherCoordPairs[maxAxis];
			byte axisC = otherCoordPairs[maxAxis + 3];
			byte step = delta[maxAxis] > 0 ? (byte) 1 : (byte) -1;
			double slopeB = (double) delta[axisB] / (double) delta[maxAxis];
			double slopeC = (double) delta[axisC] / (double) delta[maxAxis];
			int[] pos = new int[]{0, 0, 0};
			int i = 0;

			for(int limit = delta[maxAxis] + step; i != limit; i += step) {
				pos[maxAxis] = MathHelper.floor_double((double) (start[maxAxis] + i) + 0.5D);
				pos[axisB] = MathHelper.floor_double((double) start[axisB] + (double) i * slopeB + 0.5D);
				pos[axisC] = MathHelper.floor_double((double) start[axisC] + (double) i * slopeC + 0.5D);
				this.worldObj.setTileNoUpdate(pos[0], pos[1], pos[2], Block.wood.blockID);
			}
		}
	}

	/**
	 * Checks the blocks along the line from {@code start} to {@code end} until it
	 * hits something solid (neither air nor leaves). Returns the distance from the
	 * start to the first obstruction, or -1 if the whole line is clear.
	 */
	private int checkBlockLine(int[] start, int[] end) {
		int[] delta = new int[]{0, 0, 0};
		int maxAxis = 0;
		for(int axis = 0; axis < 3; ++axis) {
			delta[axis] = end[axis] - start[axis];
			if(Math.abs(delta[axis]) > Math.abs(delta[maxAxis])) {
				maxAxis = axis;
			}
		}

		if(delta[maxAxis] == 0) {
			return -1;
		} else {
			byte axisB = otherCoordPairs[maxAxis];
			byte axisC = otherCoordPairs[maxAxis + 3];
			byte step = delta[maxAxis] > 0 ? (byte) 1 : (byte) -1;
			double slopeB = (double) delta[axisB] / (double) delta[maxAxis];
			double slopeC = (double) delta[axisC] / (double) delta[maxAxis];
			int[] pos = new int[]{0, 0, 0};
			int i = 0;

			int endIndex;
			for(endIndex = delta[maxAxis] + step; i != endIndex; i += step) {
				pos[maxAxis] = start[maxAxis] + i;
				pos[axisB] = (int) ((double) start[axisB] + (double) i * slopeB);
				pos[axisC] = (int) ((double) start[axisC] + (double) i * slopeC);
				int blockId = this.worldObj.getBlockId(pos[0], pos[1], pos[2]);
				if(blockId != 0 && blockId != Block.leaves.blockID) {
					break;
				}
			}

			return i == endIndex ? -1 : Math.abs(i);
		}
	}

	@Override
	public final void setScale(double width, double height, double leafDistance) {
		this.heightLimitLimit = 12;
		this.leafDistanceLimit = 5;
		this.scaleWidth = 1.0D;
		this.leafDensity = 1.0D;
	}

	@Override
	public final boolean generate(World world, Random random, int x, int y, int z) {
		this.worldObj = world;
		long seed = random.nextLong();
		this.rand.setSeed(seed);
		this.basePos[0] = x;
		this.basePos[1] = y;
		this.basePos[2] = z;

		if(this.heightLimit == 0) {
			this.heightLimit = 5 + this.rand.nextInt(this.heightLimitLimit);
		}

		int[] base = new int[]{this.basePos[0], this.basePos[1], this.basePos[2]};
		int[] top = new int[]{this.basePos[0], this.basePos[1] + this.heightLimit - 1, this.basePos[2]};
		int groundBlock = this.worldObj.getBlockId(this.basePos[0], this.basePos[1] - 1, this.basePos[2]);
		boolean canGrow;
		if(groundBlock != Block.grass.blockID && groundBlock != Block.dirt.blockID) {
			canGrow = false;
		} else {
			int clearHeight = this.checkBlockLine(base, top);
			if(clearHeight == -1) {
				canGrow = true;
			} else if(clearHeight < 6) {
				canGrow = false;
			} else {
				this.heightLimit = clearHeight;
				canGrow = true;
			}
		}

		if(!canGrow) {
			return false;
		} else {
			this.height = (int) ((double) this.heightLimit * this.heightAttenuation);
			if(this.height >= this.heightLimit) {
				this.height = this.heightLimit - 1;
			}

			int nodesPerLayer = (int) (1.382D + Math.pow(this.leafDensity * (double) this.heightLimit / 13.0D, 2.0D));
			if(nodesPerLayer <= 0) {
				nodesPerLayer = 1;
			}

			int[][] leafNodesBuffer = new int[nodesPerLayer * this.heightLimit][4];
			int branchY = this.basePos[1] + this.heightLimit - this.leafDistanceLimit;
			int leafNodeCount = 1;
			int crownBase = this.basePos[1] + this.height;
			int workingHeight = branchY - this.basePos[1];
			leafNodesBuffer[0][0] = this.basePos[0];
			leafNodesBuffer[0][1] = branchY;
			leafNodesBuffer[0][2] = this.basePos[2];
			leafNodesBuffer[0][3] = crownBase;
			--branchY;

			int branchZ;
			while(workingHeight >= 0) {
				int branchIndex = 0;
				float layerRadius;
				if((double) workingHeight < (double) ((float) this.heightLimit) * 0.3D) {
					layerRadius = -1.618F;
				} else {
					float radiusMax = (float) this.heightLimit / 2.0F;
					float radiusAtHeight = (float) this.heightLimit / 2.0F - (float) workingHeight;
					float radius;
					if(radiusAtHeight == 0.0F) {
						radius = radiusMax;
					} else if(Math.abs(radiusAtHeight) >= radiusMax) {
						radius = 0.0F;
					} else {
						radius = (float) Math.sqrt(Math.pow((double) Math.abs(radiusMax), 2.0D) - Math.pow((double) Math.abs(radiusAtHeight), 2.0D));
					}

					radius *= 0.5F;
					layerRadius = radius;
				}

				float branchRadius = layerRadius;
				if(branchRadius < 0.0F) {
					--branchY;
					--workingHeight;
				} else {
					for(; branchIndex < nodesPerLayer; ++branchIndex) {
						double branchLength = this.scaleWidth * (double) branchRadius * ((double) this.rand.nextFloat() + 0.328D);
						double branchAngle = (double) this.rand.nextFloat() * 2.0D * 3.14159D;
						int branchPosX = (int) (branchLength * Math.sin(branchAngle) + (double) this.basePos[0] + 0.5D);
						branchZ = (int) (branchLength * Math.cos(branchAngle) + (double) this.basePos[2] + 0.5D);
						int[] branchStart = new int[]{branchPosX, branchY, branchZ};
						int[] branchEnd = new int[]{branchPosX, branchY + this.leafDistanceLimit, branchZ};
						if(this.checkBlockLine(branchStart, branchEnd) == -1) {
							branchEnd = new int[]{this.basePos[0], this.basePos[1], this.basePos[2]};
							double horizontalDistance = Math.sqrt(Math.pow((double) Math.abs(this.basePos[0] - branchStart[0]), 2.0D) + Math.pow((double) Math.abs(this.basePos[2] - branchStart[2]), 2.0D));
							double branchDrop = horizontalDistance * this.branchSlope;
							if((double) branchStart[1] - branchDrop > (double) crownBase) {
								branchEnd[1] = crownBase;
							} else {
								branchEnd[1] = (int) ((double) branchStart[1] - branchDrop);
							}

							if(this.checkBlockLine(branchEnd, branchStart) == -1) {
								leafNodesBuffer[leafNodeCount][0] = branchPosX;
								leafNodesBuffer[leafNodeCount][1] = branchY;
								leafNodesBuffer[leafNodeCount][2] = branchZ;
								leafNodesBuffer[leafNodeCount][3] = branchEnd[1];
								++leafNodeCount;
							}
						}
					}

					--branchY;
					--workingHeight;
				}
			}

			this.leafNodes = new int[leafNodeCount][4];
			System.arraycopy(leafNodesBuffer, 0, this.leafNodes, 0, leafNodeCount);

			for(int node = 0; node < this.leafNodes.length; ++node) {
				int leafNodeX = this.leafNodes[node][0];
				int leafNodeY = this.leafNodes[node][1];
				int leafNodeZ = this.leafNodes[node][2];

				for(int leafY = leafNodeY; leafY < leafNodeY + this.leafDistanceLimit; ++leafY) {
					int layer = leafY - leafNodeY;
					float leafRadius = layer >= 0 && layer < this.leafDistanceLimit ? (layer != 0 && layer != this.leafDistanceLimit - 1 ? 3.0F : 2.0F) : -1.0F;
					int discRadius = (int) ((double) leafRadius + 0.618D);
					byte axisX = otherCoordPairs[1];
					byte axisZ = otherCoordPairs[4];
					int[] centre = new int[]{leafNodeX, leafY, leafNodeZ};
					int[] pos = new int[]{0, 0, 0};
					pos[1] = centre[1];

					for(int dx = -discRadius; dx <= discRadius; ++dx) {
						pos[0] = centre[0] + dx;
						for(int dz = -discRadius; dz <= discRadius; ++dz) {
							double distance = Math.sqrt(Math.pow((double) Math.abs(dx) + 0.5D, 2.0D) + Math.pow((double) Math.abs(dz) + 0.5D, 2.0D));
							if(distance > (double) leafRadius) {
								continue;
							}

							pos[2] = centre[2] + dz;
							int blockId = this.worldObj.getBlockId(pos[0], pos[1], pos[2]);
							if(blockId == 0 || blockId == Block.leaves.blockID) {
								this.worldObj.setTileNoUpdate(pos[0], pos[1], pos[2], Block.leaves.blockID);
							}
						}
					}
				}
			}

			int trunkBottomX = this.basePos[0];
			int trunkBottomY = this.basePos[1];
			int trunkTopY = this.basePos[1] + this.height;
			int trunkBottomZ = this.basePos[2];
			int[] trunkBottom = new int[]{trunkBottomX, trunkBottomY, trunkBottomZ};
			int[] trunkTop = new int[]{trunkBottomX, trunkTopY, trunkBottomZ};
			this.placeBlockLine(trunkBottom, trunkTop);
			if(this.trunkSize == 2) {
				++trunkBottom[0];
				++trunkTop[0];
				this.placeBlockLine(trunkBottom, trunkTop);
				++trunkBottom[2];
				++trunkTop[2];
				this.placeBlockLine(trunkBottom, trunkTop);
				trunkBottom[0] += -1;
				trunkTop[0] += -1;
				this.placeBlockLine(trunkBottom, trunkTop);
			}

			int[] branchBase = new int[]{this.basePos[0], this.basePos[1], this.basePos[2]};
			for(int node = 0; node < this.leafNodes.length; ++node) {
				int[] leafNode = this.leafNodes[node];
				int[] branchTip = new int[]{leafNode[0], leafNode[1], leafNode[2]};
				branchBase[1] = leafNode[3];
				int branchHeight = branchBase[1] - this.basePos[1];
				if((double) branchHeight >= (double) this.heightLimit * 0.2D) {
					this.placeBlockLine(branchBase, branchTip);
				}
			}

			return true;
		}
	}
}
