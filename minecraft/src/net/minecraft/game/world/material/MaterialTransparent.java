package net.minecraft.game.world.material;

/**
 * A transparent material: non-solid like {@link MaterialLogic}, but also fully pass-through and
 * light-transmitting — {@link Material#air} and {@link Material#fire}. Distinct from the logic
 * materials so liquids can settle in it and so it never acts as a solid ceiling above fire.
 */
public final class MaterialTransparent extends Material {
	@Override
	public final boolean isSolid() {
		return false;
	}

	@Override
	public final boolean getCanBlockGrass() {
		return false;
	}

	@Override
	public final boolean getIsSolid() {
		return false;
	}
}