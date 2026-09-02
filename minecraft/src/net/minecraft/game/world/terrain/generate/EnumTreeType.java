package net.minecraft.game.world.terrain.generate;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import net.minecraft.game.world.block.Block;
import net.minecraft.game.world.block.BlockLeaves;
import net.minecraft.game.world.block.BlockLog;
import net.minecraft.game.world.block.BlockSapling;
import net.minecraft.game.world.block.BlockState;

/**
 * A tree variant descriptor. Each element carries the three block states that
 * define a tree (leaves, wood, sapling), whether it grows from a single sapling
 * or a 2x2 cluster, and knows how to produce a {@link WorldGenerator} for it.
 *
 * <p>New variants are added by appending enum constants and populating the three
 * lookup maps in the static initializer. All lookups fall back to {@link #OAK}
 * so that code that calls {@link #findTreeTypeFromLeaves}/{@link #findTreeTypeFromSapling}
 * etc. always has a well-defined answer.
 */
public enum EnumTreeType {
	OAK("oak", new BlockState(Block.leaves, BlockLeaves.OAK), new BlockState(Block.wood, BlockLog.OAK), new BlockState(Block.sapling, BlockSapling.OAK));

	private final String name;
	private final BlockState leaves;
	private final BlockState wood;
	private final BlockState sapling;
	private final boolean needsFourSaplings;

	EnumTreeType(String name, BlockState leaves, BlockState wood, BlockState sapling, boolean needsFourSaplings) {
		this.name = name;
		this.leaves = leaves;
		this.wood = wood;
		this.sapling = sapling;
		this.needsFourSaplings = needsFourSaplings;
	}

	EnumTreeType(String name, BlockState leaves, BlockState wood, BlockState sapling) {
		this(name, leaves, wood, sapling, false);
	}

	public String getName() {
		return this.name;
	}

	public BlockState getLeaves() {
		return this.leaves;
	}

	public BlockState getWood() {
		return this.wood;
	}

	public BlockState getSapling() {
		return this.sapling;
	}

	public boolean getNeedsFourSaplings() {
		return this.needsFourSaplings;
	}

	/**
	 * Produces a {@link WorldGenerator} for this tree type. {@link #OAK} is the
	 * only defined variant and keeps the historical 1-in-10 chance of a big tree.
	 */
	public WorldGenerator getGenerator(Random rand) {
		return rand.nextInt(10) == 0 ? new WorldGenBigTree() : new WorldGenTrees();
	}

	private static final Map<BlockState, EnumTreeType> FROM_LEAVES = new HashMap<>();
	private static final Map<BlockState, EnumTreeType> FROM_SAPLING = new HashMap<>();
	private static final Map<BlockState, EnumTreeType> FROM_WOOD   = new HashMap<>();

	static {
		for (EnumTreeType type : EnumTreeType.values()) {
			FROM_LEAVES.put(type.leaves,   type);
			FROM_SAPLING.put(type.sapling, type);
			FROM_WOOD.put(type.wood,      type);
		}
	}

	public static EnumTreeType findTreeTypeFromLeaves(int blockID, int metadata) {
		EnumTreeType type = FROM_LEAVES.get(new BlockState(blockID, metadata));
		return type != null ? type : OAK;
	}

	public static EnumTreeType findTreeTypeFromSapling(int blockID, int metadata) {
		EnumTreeType type = FROM_SAPLING.get(new BlockState(blockID, metadata));
		return type != null ? type : OAK;
	}

	public static EnumTreeType findTreeTypeFromWood(int blockID, int metadata) {
		EnumTreeType type = FROM_WOOD.get(new BlockState(blockID, metadata));
		return type != null ? type : OAK;
	}
}
