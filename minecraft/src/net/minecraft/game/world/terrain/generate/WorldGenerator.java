package net.minecraft.game.world.terrain.generate;

import java.util.Random;
import net.minecraft.game.world.World;

/**
 * Base class for all world generators that place structures (ore veins, trees)
 * into a {@link World}. The terrain population pass drives one of these from a
 * chunk-level loop, giving each structure a world, a shared random and a target
 * world position.
 */
public abstract class WorldGenerator {
	/**
	 * Tries to place the structure around the given position.
	 *
	 * @param world  the world to modify
	 * @param random shared random used by the caller (seed already advanced)
	 * @param x      target block x coordinate
	 * @param y      target block y coordinate (usually the surface height)
	 * @param z      target block z coordinate
	 * @return true if the structure was placed
	 */
	public abstract boolean generate(World world, Random random, int x, int y, int z);

	/**
	 * Adjusts how the structure is scaled before being generated. The base
	 * implementation is a no-op; tree generators override it to configure their
	 * size parameters.
	 *
	 * @param width         horizontal scale
	 * @param height        vertical scale
	 * @param leafDistance  foliage distance from the trunk tip
	 */
	public void setScale(double width, double height, double leafDistance) {
	}
}
