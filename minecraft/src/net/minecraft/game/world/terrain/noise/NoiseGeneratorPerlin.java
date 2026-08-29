package net.minecraft.game.world.terrain.noise;

import java.util.Random;

/**
 * Classic three-dimensional Perlin noise. The unit lattice is assigned a pseudo
 * random gradient at each of its grid points (through a permutation table that
 * acts as the hash), and any point in space is evaluated by taking the smooth,
 * linear blend of the gradients at the eight surrounding lattice corners.
 */
public final class NoiseGeneratorPerlin extends NoiseGenerator {
	private int[] permutations;
	private double xCoord;
	private double yCoord;
	private double zCoord;

	public NoiseGeneratorPerlin() {
		this(new Random());
	}

	/**
	 * Builds the noise field from a seed. The permutation table is initialised to
	 * the identity and then shuffled with a partial Fisher-Yates pass, and each
	 * half of the 512-entry table mirrors the first 256 entries so that lattice
	 * lookups wrap around seamlessly.
	 */
	public NoiseGeneratorPerlin(Random random) {
		this.permutations = new int[512];
		this.xCoord = random.nextDouble() * 256.0D;
		this.yCoord = random.nextDouble() * 256.0D;
		this.zCoord = random.nextDouble() * 256.0D;

		int i;
		for(i = 0; i < 256; this.permutations[i] = i++) {
		}

		for(i = 0; i < 256; ++i) {
			int swapIndex = random.nextInt(256 - i) + i;
			int swap = this.permutations[i];
			this.permutations[i] = this.permutations[swapIndex];
			this.permutations[swapIndex] = swap;
			this.permutations[i + 256] = this.permutations[i];
		}
	}

	/**
	 * Evaluates 3D Perlin noise at (x, y, z).
	 */
	private double generateNoise(double x, double y, double z) {
		double px = x + this.xCoord;
		double py = y + this.yCoord;
		double pz = z + this.zCoord;

		int floorX = (int) px;
		int floorY = (int) py;
		int floorZ = (int) pz;
		if(px < (double) floorX) {
			--floorX;
		}

		if(py < (double) floorY) {
			--floorY;
		}

		if(pz < (double) floorZ) {
			--floorZ;
		}

		int ix = floorX & 255;
		int iy = floorY & 255;
		int iz = floorZ & 255;
		double fx = px - (double) floorX;
		double fy = py - (double) floorY;
		double fz = pz - (double) floorZ;

		double u = smoothStep(fx);
		double v = smoothStep(fy);
		double w = smoothStep(fz);

		int a = this.permutations[ix] + iy;
		int aa = this.permutations[a] + iz;
		int ab = this.permutations[a + 1] + iz;
		int b = this.permutations[ix + 1] + iy;
		int ba = this.permutations[b] + iz;
		int bb = this.permutations[b + 1] + iz;

		return lerp(w, lerp(v, lerp(u, grad(this.permutations[aa], fx, fy, fz), grad(this.permutations[ba], fx - 1.0D, fy, fz)), lerp(u, grad(this.permutations[ab], fx, fy - 1.0D, fz), grad(this.permutations[bb], fx - 1.0D, fy - 1.0D, fz))), lerp(v, lerp(u, grad(this.permutations[aa + 1], fx, fy, fz - 1.0D), grad(this.permutations[ba + 1], fx - 1.0D, fy, fz - 1.0D)), lerp(u, grad(this.permutations[ab + 1], fx, fy - 1.0D, fz - 1.0D), grad(this.permutations[bb + 1], fx - 1.0D, fy - 1.0D, fz - 1.0D))));
	}

	/**
	 * The Perlin fade curve: a smooth (C2) step from 0 to 1 over the unit
	 * interval, removing the lattice-cell boundaries from the result.
	 */
	private static double smoothStep(double t) {
		return t * t * t * (t * (t * 6.0D - 15.0D) + 10.0D);
	}

	private static double lerp(double amount, double a, double b) {
		return a + amount * (b - a);
	}

	/**
	 * Returns the gradient vector's dot product with the offset from its lattice
	 * corner. The low four bits of the permutation index select one of eight
	 * axis-aligned gradient directions.
	 */
	private static double grad(int hash, double x, double y, double z) {
		hash &= 15;
		double u = hash < 8 ? x : y;
		double v = hash < 4 ? y : (hash != 12 && hash != 14 ? z : x);
		return ((hash & 1) == 0 ? u : -u) + ((hash & 2) == 0 ? v : -v);
	}

	public final double generateNoise(double x, double y) {
		return this.generateNoise(x, y, 0.0D);
	}

	public final double generateNoiseD(double x, double y, double z) {
		return this.generateNoise(x, y, z);
	}

	/**
	 * Samples the noise field over a grid of (width x height x depth) points
	 * starting at (startX, startY, startZ) and accumulates each value into
	 * {@code values}. The lattice gradients are only recomputed when the y
	 * lattice cell changes (they stay constant while stepping vertically within
	 * one lattice column).
	 */
	public final void populateNoiseArray(double[] values, int startX, int startY, int startZ, int width, int height, int depth, double xScale, double yScale, double zScale, double frequency) {
		int index = 0;
		double scale = 1.0D / frequency;
		int lastYCell = -1;
		double a0 = 0.0D;
		double a1 = 0.0D;
		double b0 = 0.0D;
		double b1 = 0.0D;

		for(int gx = 0; gx < width; ++gx) {
			double px = (double) (startX + gx) * xScale + this.xCoord;
			int cellX = (int) px;
			if(px < (double) cellX) {
				--cellX;
			}

			int ix = cellX & 255;
			px -= (double) cellX;
			double qx = smoothStep(px);

			for(int gz = 0; gz < depth; ++gz) {
				double pz = (double) (startZ + gz) * zScale + this.zCoord;
				int cellZ = (int) pz;
				if(pz < (double) cellZ) {
					--cellZ;
				}

				int iz = cellZ & 255;
				pz -= (double) cellZ;
				double qz = smoothStep(pz);

				for(int gy = 0; gy < height; ++gy) {
					double py = (double) (startY + gy) * yScale + this.yCoord;
					int cellY = (int) py;
					if(py < (double) cellY) {
						--cellY;
					}

					int iy = cellY & 255;
					py -= (double) cellY;
					double qy = smoothStep(py);

					if(gy == 0 || iy != lastYCell) {
						lastYCell = iy;
						int p00 = this.permutations[ix] + iy;
						int p10 = this.permutations[p00] + iz;
						int p01 = this.permutations[p00 + 1] + iz;
						iy += this.permutations[ix + 1];
						int p11 = this.permutations[iy] + iz;
						iy = this.permutations[iy + 1] + iz;
						a0 = lerp(qx, grad(this.permutations[p10], px, py, pz), grad(this.permutations[p11], px - 1.0D, py, pz));
						a1 = lerp(qx, grad(this.permutations[p01], px, py - 1.0D, pz), grad(this.permutations[iy], px - 1.0D, py - 1.0D, pz));
						b0 = lerp(qx, grad(this.permutations[p10 + 1], px, py, pz - 1.0D), grad(this.permutations[p11 + 1], px - 1.0D, py, pz - 1.0D));
						b1 = lerp(qx, grad(this.permutations[p01 + 1], px, py - 1.0D, pz - 1.0D), grad(this.permutations[iy + 1], px - 1.0D, py - 1.0D, pz - 1.0D));
					}

					double c0 = lerp(qy, a0, a1);
					double c1 = lerp(qy, b0, b1);
					double noise = lerp(qz, c0, c1);
					values[index++] += noise * scale;
				}
			}
		}
	}
}
