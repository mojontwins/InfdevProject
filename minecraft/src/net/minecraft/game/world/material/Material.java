package net.minecraft.game.world.material;

/**
 * The physical/mechanical identity of a {@link net.minecraft.game.world.block.Block}: not the
 * concrete block type, but the class of material it behaves like (ground, rock, water, fire, ...).
 *
 * <p>This is the tiny, hand-tuned ancestor of the modern {@code MapColor}-based {@code Material}
 * hierarchy. Each behaviour is a single boolean the subclasses flip; the block classes then ask
 * these questions ("is this a liquid?", "does it block movement?") to decide flow, fall, grass
 * spread and rendering without having to switch on specific block ids.
 *
 * <p>The default (base) material is a solid, opaque block material: solids {@link #isSolid()},
 * block grass growth, and are themselves "solid" in the {@link #getIsSolid()} sense (see below).
 */
public class Material {
	/** Air: transparent, non-solid pass-through that water falls into and nothing can stand on. */
	public static final Material air = new MaterialTransparent();
	public static final Material ground = new Material();
	public static final Material wood = new Material();
	public static final Material rock = new Material();
	public static final Material iron = new Material();
	/** Water: a liquid; flows and displaces via {@code BlockFluid}. */
	public static final Material water = new MaterialLiquid();
	/** Lava: a liquid, like water but hotter (sets things alight, turns to stone next to water). */
	public static final Material lava = new MaterialLiquid();
	public static final Material leaves = new Material();
	/** Plants (tall grass, flowers, mushrooms, saplings, reeds): non-solid, no collision. */
	public static final Material plants = new MaterialLogic();
	public static final Material sponge = new Material();
	public static final Material cloth = new Material();
	/** Fire: transparent, non-solid, lets light/grass through. */
	public static final Material fire = new MaterialTransparent();
	public static final Material sand = new Material();
	/** Circuits (redstone wire, repeaters, levers, pressure plates): non-solid logic blocks. */
	public static final Material circuits = new MaterialLogic();
	public static final Material glass = new Material();
	public static final Material tnt = new Material();

	/**
	 * Whether this material is a liquid (water or lava). Liquids are non-solid and swap/drain
	 * against other liquids. Overridden by {@link MaterialLiquid}.
	 */
	public boolean getIsLiquid() {
		return false;
	}

	/**
	 * Whether this material is a "liquid pass-through": neither a liquid itself, nor solid.
	 * Pure movement helper — true means a block of this material offers no floor and no body,
	 * so a liquid column can settle in it (used by {@code BlockFluid} to decide where to flow).
	 */
	public final boolean liquidSolidCheck() {
		return !this.getIsLiquid() && !this.isSolid();
	}

	/**
	 * Whether a block of this material physically blocks movement (has a collision box) and
	 * supports walking. Reversed from the default by every non-solid subclass.
	 */
	public boolean isSolid() {
		return true;
	}

	/**
	 * Whether a block of this material lets grass propagate through it when its surface is lit.
	 * Opaque solids return true (grass can grow over them); transparent/plants-logic blocks
	 * return false so they act as an air-like layer that permits spreading to the block under.
	 */
	public boolean getCanBlockGrass() {
		return true;
	}

	/**
	 * Whether this material forms an opaque body with a "top" — a solid ceiling that fire/​splash
	 * tests and the pathfinder can sit beneath. For typical solids this agrees with
	 * {@link #isSolid()}; the one deliberate exception is a {@link MaterialLiquid}, which has no
	 * collision box ({@code isSolid() == false}) but is still treated as an unbroken surface
	 * ({@code getIsSolid() == true}), so water stops splash particles and lets lava ignite.
	 */
	public boolean getIsSolid() {
		return true;
	}
}
