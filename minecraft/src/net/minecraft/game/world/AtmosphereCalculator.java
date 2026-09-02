package net.minecraft.game.world;

import net.minecraft.game.physics.Vec3D;
import util.MathHelper;

/**
 * Pure functions that turn a world-time tick and three palette longs into the
 * RGB colours the renderer paints on the sky, fog and clouds. Lives outside
 * {@link World} because it does not depend on live world state (no entity list,
 * no chunk cache, no lighting queue) — the only inputs are the time tick, a
 * {@code partialTick} for sub-tick interpolation, and three pre-baked palette
 * longs. Methods are {@code static} so the renderer can call them with no
 * virtual dispatch.
 *
 * <p>The palettes are produced by {@link WorldType} and read at world
 * construction in {@link World}; they encode RGB in 24 bits (8 bits per
 * channel, big-endian inside a {@code long}).
 */
final class AtmosphereCalculator {
	private AtmosphereCalculator() {
	}

	/**
	 * Returns the world time in [0, 1) by combining the world time tick (mod
	 * 24000, the length of one in-game day) with the render partial tick, and
	 * wrapping into [0, 1). Subtracts 0.25 so the sky is aligned with the
	 * a1.1.2/b1.7.3 convention: the sun is overhead (noon) at world time 6000
	 * and midnight falls at 18000. Infdev 20100420 originally used 0.15 (noon
	 * at 3600); the 0.15 -> 0.25 change landed between Infdev and Alpha.
	 */
	static float getCelestialAngle(long worldTime, float partialTick) {
		int timeOfDay = (int) (worldTime % 24000L);
		float celestialAngle = ((float) timeOfDay + partialTick) / 24000.0F - 0.25F;
		if (celestialAngle < 0.0F) {
			++celestialAngle;
		}
		if (celestialAngle > 1.0F) {
			--celestialAngle;
		}
		return celestialAngle;
	}

	/**
	 * Reshapes a raw celestial angle into the cosine wave the rest of the
	 * sky/fog/cloud math expects: cosine of {@code angle * 2π} scaled to [0..2]
	 * then offset to [-0.5..1.5] and clamped to [0..1]. 1.0 = day, 0.0 = night.
	 */
	private static float dayFactor(float celestialAngle) {
		float factor = MathHelper.cos(celestialAngle * (float) Math.PI * 2.0F) * 2.0F + 0.5F;
		if (factor < 0.0F) factor = 0.0F;
		if (factor > 1.0F) factor = 1.0F;
		return factor;
	}

	private static Vec3D colorFromPalette(long paletteRGB, float dayFactor) {
		float r = (float) (paletteRGB >> 16 & 255L) / 255.0F;
		float g = (float) (paletteRGB >> 8 & 255L) / 255.0F;
		float b = (float) (paletteRGB & 255L) / 255.0F;
		r *= dayFactor;
		g *= dayFactor;
		b *= dayFactor;
		return new Vec3D(r, g, b);
	}

	/**
	 * Returns the sky-color RGB for the current celestial angle, dimmed by a
	 * day/night factor. The factor is a cosine wave around 0.5 (1.0 = day, 0.0
	 * = midnight), so the color is multiplied by it.
	 */
	static Vec3D getSkyColor(long skyPalette, float celestialAngle) {
		return colorFromPalette(skyPalette, dayFactor(celestialAngle));
	}

	/**
	 * Returns the fog-color RGB. Asymmetric tint makes the horizon look warmer.
	 */
	static Vec3D getFogColor(long fogPalette, float celestialAngle) {
		float factor = dayFactor(celestialAngle);
		float r = (float) (fogPalette >> 16 & 255L) / 255.0F;
		float g = (float) (fogPalette >> 8 & 255L) / 255.0F;
		float b = (float) (fogPalette & 255L) / 255.0F;
		r *= factor * 0.94F + 0.06F;
		g *= factor * 0.94F + 0.06F;
		b *= factor * 0.91F + 0.09F;
		return new Vec3D(r, g, b);
	}

	/**
	 * Returns the cloud-color RGB. Same formula as the sky but with an
	 * asymmetric tint (clouds are slightly warmer at night).
	 */
	static Vec3D getCloudColor(long cloudPalette, float celestialAngle) {
		float factor = dayFactor(celestialAngle);
		float r = (float) (cloudPalette >> 16 & 255L) / 255.0F;
		float g = (float) (cloudPalette >> 8 & 255L) / 255.0F;
		float b = (float) (cloudPalette & 255L) / 255.0F;
		r *= factor * 0.9F + 0.1F;
		g *= factor * 0.9F + 0.1F;
		b *= factor * 0.85F + 0.15F;
		return new Vec3D(r, g, b);
	}

	/**
	 * Star brightness is a 0..1 value, brightest near midnight, drops to 0 in
	 * daylight. Cubic falloff to make stars visibly snap on at dusk.
	 */
	static float getStarBrightness(float celestialAngle) {
		float factor = 1.0F - (MathHelper.cos(celestialAngle * (float) Math.PI * 2.0F) * 2.0F + 12.0F / 16.0F);
		if (factor < 0.0F) factor = 0.0F;
		if (factor > 1.0F) factor = 1.0F;
		return factor * factor * 0.5F;
	}
}
