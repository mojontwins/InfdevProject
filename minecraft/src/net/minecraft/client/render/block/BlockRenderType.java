package net.minecraft.client.render.block;

/**
 * Registry mapping each block render type (the value of {@code Block#getRenderType()})
 * to the handler strategy that owns its geometry.
 *
 * <p>The table is indexed by render-type id, so dispatch is a single array
 * lookup. Two types share slot 5 in this era: redstone wire shadows the
 * thin-panel ladder type in older maps, so the ladder is registered last and
 * wins — the two handlers are visually so close that the shadowing went
 * unnoticed until the port.
 */
public enum BlockRenderType {

	NORMAL(0, new RenderBlockNormal()),
	PLANT(1, new RenderBlockPlant()),
	TORCH(2, new RenderBlockTorch()),
	FIRE(3, new RenderBlockFire()),
	FLUID(4, new RenderBlockFluid()),
	REDSTONE_WIRE(5, new RenderBlockRedstoneWire()),
	LADDER(5, new RenderBlockLadder()),
	CROPS(6, new RenderBlockCrops()),
	DOOR(7, new RenderBlockDoor()),
	LADDER_WALL(8, new RenderBlockLadderWall()),
	RAIL(9, new RenderBlockRail()),
	STAIRS(10, new RenderBlockStairs()),
	FENCE(11, new RenderBlockFence()),
	LEVER(12, new RenderBlockLever()),
	CACTUS(13, new RenderBlockCactus()),
	BED(14, new RenderBlockBed()),
	REPEATER(15, new RenderBlockRepeater()),
	PANE(18, new RenderBlockPane()),
	VINE(20, new RenderBlockVine()),
	LILYPAD(23, new RenderBlockLilyPad());

	/** One entry per render-type slot, indexed by render type. */
	private static final BlockRenderType[] BY_RENDER_TYPE = new BlockRenderType[24];

	static {
		for(BlockRenderType type : values()) {
			BY_RENDER_TYPE[type.renderType] = type;
		}
	}

	public final int renderType;
	private final BlockRenderHandler handler;

	private BlockRenderType(int renderType, BlockRenderHandler handler) {
		this.renderType = renderType;
		this.handler = handler;
	}

	public BlockRenderHandler handler() {
		return this.handler;
	}

	/** The handler registered for the given render type, in a single table lookup. */
	public static BlockRenderType get(int renderType) {
		BlockRenderType type = renderType >= 0 && renderType < BY_RENDER_TYPE.length ? BY_RENDER_TYPE[renderType] : null;
		if(type == null) {
			throw new IllegalArgumentException("No render handler registered for render type " + renderType);
		}
		return type;
	}
}