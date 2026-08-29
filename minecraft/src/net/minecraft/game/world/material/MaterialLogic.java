package net.minecraft.game.world.material;

/**
 * A "logic" material: non-solid block that is neither a liquid nor a solid body — the plants
 * and redstone-class materials ({@link Material#plants}, {@link Material#circuits}). It occupies
 * a block without offering standing room or a stepping surface.
 */
public final class MaterialLogic extends Material {
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