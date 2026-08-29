package net.minecraft.game.world.terrain.noise;

import java.util.Random;

/**
 * Combines several {@link NoiseGeneratorPerlin} instances into a summation of
 * octaves. Each octave is a single Perlin noise field sampled at a different
 * frequency, so that both broad, low-frequency features and the finer,
 * high-frequency detail contribute to the final value.
 */
public final class NoiseGeneratorOctaves extends NoiseGenerator {
	private NoiseGeneratorPerlin[] generatorCollection;
	private int octaves;

	public NoiseGeneratorOctaves(Random random, int octaves) {
		this.octaves = octaves;
		this.generatorCollection = new NoiseGeneratorPerlin[octaves];

		for(int i = 0; i < octaves; ++i) {
			this.generatorCollection[i] = new NoiseGeneratorPerlin(random);
		}
	}

	/**
	 * Returns the two-dimensional fractal noise at (x, y). With each higher
	 * octave the sampling frequency is halved and the contribution weight kept
	 * equal, giving an evenly weighted blend of features of every scale.
	 */
	public final double noiseGenerator(double x, double y) {
		double value = 0.0D;
		double frequency = 1.0D;

		for(int octave = 0; octave < this.octaves; ++octave) {
			value += this.generatorCollection[octave].generateNoise(x * frequency, y * frequency) / frequency;
			frequency /= 2.0D;
		}

		return value;
	}

	/**
	 * Returns the three-dimensional fractal noise at (x, y, z), again blending
	 * octaves of halving frequency with equal weight.
	 */
	public final double generateNoiseOctaves(double x, double y, double z) {
		double value = 0.0D;
		double frequency = 1.0D;

		for(int octave = 0; octave < this.octaves; ++octave) {
			value += this.generatorCollection[octave].generateNoiseD(x * frequency, y * frequency, z * frequency) / frequency;
			frequency /= 2.0D;
		}

		return value;
	}

	/**
	 * Fills {@code values} with fractal noise sampled over a rectangular grid of
	 * size (width x height x depth) starting at (startX, startY, startZ). Only a
	 * fixed range of noise is ever requested: each octave's small grid is summed
	 * into the same output array.
	 *
	 * @param values       output array; created if null, otherwise cleared first
	 * @param startX       grid origin x (in noise space)
	 * @param startY       grid origin y
	 * @param startZ       grid origin z
	 * @param width        grid size along x
	 * @param height       grid size along y
	 * @param depth        grid size along z
	 * @param xScale       per-unit x frequency
	 * @param yScale       per-unit y frequency
	 * @param zScale       per-unit z frequency
	 */
	public final double[] generateNoiseOctaves(double[] values, int startX, int startY, int startZ, int width, int height, int depth, double xScale, double yScale, double zScale) {
		if(values == null) {
			values = new double[width * height * depth];
		} else {
			for(int i = 0; i < values.length; ++i) {
				values[i] = 0.0D;
			}
		}

		double frequency = 1.0D;

		for(int octave = 0; octave < this.octaves; ++octave) {
			this.generatorCollection[octave].populateNoiseArray(values, startX, startY, startZ, width, height, depth, xScale * frequency, yScale * frequency, zScale * frequency, frequency);
			frequency /= 2.0D;
		}

		return values;
	}
}
