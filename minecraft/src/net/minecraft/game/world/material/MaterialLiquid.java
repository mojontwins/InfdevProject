package net.minecraft.game.world.material;

/**
 * A liquid material (water, lava): has no collision box and flows to fill its space.
 * Kept as a distinct subclass so {@link BlockFluid} can tell water from lava apart and apply
 * its flow, drain-into-air and fire-extinguishing rules only to genuine liquids.
 */
public final class MaterialLiquid extends Material {
	@Override
	public final boolean getIsLiquid() {
		return true;
	}

	@Override
	public final boolean isSolid() {
		return false;
	}
}