package net.minecraft.game.world.terrain.generate;

import java.util.Random;
import net.minecraft.game.world.World;
import net.minecraft.game.world.block.Block;
import util.MathHelper;

/**
 * Places a vein of a minable block (coal, iron, gold, diamond) as a fat
 * ellipsoid. The vein centre is chosen randomly along a short line segment that
 * is rotated by a random angle around the y-axis, and each of the 17 sampling
 * steps carves out a sphere whose radius varies with a sine curve bulging in
 * the middle, so the result is a lumpy, roughly horizontal deposit.
 */
public final class WorldGenMinable extends WorldGenerator {
	private int minableBlockId;

	public WorldGenMinable(int minableBlockId) {
		this.minableBlockId = minableBlockId;
	}

	@Override
	public final boolean generate(World world, Random random, int x, int y, int z) {
		float angle = random.nextFloat() * (float) Math.PI;
		double x1 = (double) ((float) (x + 8) + MathHelper.sin(angle) * 2.0F);
		double x2 = (double) ((float) (x + 8) - MathHelper.sin(angle) * 2.0F);
		double z1 = (double) ((float) (z + 8) + MathHelper.cos(angle) * 2.0F);
		double z2 = (double) ((float) (z + 8) - MathHelper.cos(angle) * 2.0F);
		double y1 = (double) (y + random.nextInt(3) + 2);
		double y2 = (double) (y + random.nextInt(3) + 2);

		for(int step = 0; step <= 16; ++step) {
			double centreX = x1 + (x2 - x1) * (double) step / 16.0D;
			double centreY = y1 + (y2 - y1) * (double) step / 16.0D;
			double centreZ = z1 + (z2 - z1) * (double) step / 16.0D;
			double radius = (double) (MathHelper.sin((float) step / 16.0F * (float) Math.PI) + 1.0F) * random.nextDouble() + 1.0D;
			double radiusHalf = radius / 2.0D;

			// The loop bounds must floor (not truncate toward zero): the original
			// code used `(int)`, which shrinks the vein when a negative center is
			// involved — `(int)(-12.2)` is -12 while `(int)(+12.2)` is 12, so a
			// negative-center vein reaches one cell fewer and generates less ore
			// in the negative-x / negative-z quadrants. Floors both bounds make a
			// vein at (-c) mirror the vein at (+c) exactly.
			for(int vx = (int) Math.floor(centreX - radiusHalf); vx <= (int) Math.floor(centreX + radiusHalf); ++vx) {
				for(int vy = (int) Math.floor(centreY - radiusHalf); vy <= (int) Math.floor(centreY + radiusHalf); ++vy) {
					for(int vz = (int) Math.floor(centreZ - radiusHalf); vz <= (int) Math.floor(centreZ + radiusHalf); ++vz) {
						double dx = ((double) vx + 0.5D - centreX) / radiusHalf;
						double dy = ((double) vy + 0.5D - centreY) / radiusHalf;
						double dz = ((double) vz + 0.5D - centreZ) / radiusHalf;
						if(dx * dx + dy * dy + dz * dz < 1.0D && world.getBlockId(vx, vy, vz) == Block.stone.blockID) {
							world.setTileNoUpdate(vx, vy, vz, this.minableBlockId);
						}
					}
				}
			}
		}

		return true;
	}
}
