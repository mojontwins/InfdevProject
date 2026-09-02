package net.minecraft.game.world.block;

import java.util.Random;
import net.minecraft.game.world.World;
import net.minecraft.game.world.terrain.generate.EnumTreeType;
import net.minecraft.game.world.terrain.generate.WorldGenerator;

/**
 * A sapling: light and time accumulate in its metadata, and at metadata 15 the
 * block grows into a tree — usually a regular tree, with a small chance of a
 * big tree.
 *
 * <h2>Metadata layout</h2>
 * <ul>
 *   <li><b>Bit 3 ({@code & 8})</b> — the "ready to grow" state. {@code 0} = still
 *       accumulating, {@code 8} = the next growth tick will attempt to generate
 *       a tree. Set by {@link #updateTick} when the growth conditions are met;
 *       cleared implicitly by the block being replaced with the tree.
 * <li><b>Bits 4-7 ({@code & 0xF0})</b> — the {@link EnumTreeType} subtype.
 *       Currently only {@link #OAK} is defined. The growth path strips bit 3
 *       before looking up the tree type, and the dropped sapling item clears
 *       bit 3 with {@code & 0xF7} so a broken sapling starts accumulating
 *       again instead of immediately regrowing.
 *   <li><b>Bits 0-2</b> — currently unused.
 * </ul>
 */
public final class BlockSapling extends BlockFlower {
	public static final int OAK = 0;

	protected BlockSapling(int blockID, int textureIndex) {
		super(blockID, textureIndex);
		this.setBlockBounds(10.0F * 0.01F, 0.0F, 10.0F * 0.01F, 0.9F, 0.8F, 0.9F);
	}

	/** Saplings always render on tile 15; the parent class's metadata-based
	 * flower texture swap must not apply. The tile is already green-tinted.
	 *
	 * <p>NOTE: when sapling subtypes are added, the incoming {@code metadata}
	 * should be masked to strip bit 3 before selecting the tile (e.g.
	 * {@code metadata & 0xF0} if subtypes live in the upper nibble), so that
	 * the growth-state bit does not corrupt the variant lookup. */
	@Override
	public int getBlockTextureFromSideAndMetadata(int side, int metadata) {
		return 15;
	}

	/**
	 * The dropped item carries the sapling's subtype, with the "ready" bit (3)
	 * cleared so a freshly broken sapling starts accumulating again instead of
	 * immediately regrowing. {@code metadata & 0xF7} preserves all subtype bits
	 * (4-7) and other future bits while zeroing bit 3.
	 */
	@Override
	public int damageDropped(int metadata) {
		return metadata & 0xF7;
	}

	@Override
	public final void updateTick(World world, int x, int y, int z, Random random) {
		super.updateTick(world, x, y, z, random);
		if(world.getBlockLightValue(x, y + 1, z) >= 10 && random.nextInt(20) == 0) {
			int metadata = world.getBlockMetadata(x, y, z);
			// NOTE: bit 3 (& 8) is the "ready to grow" state. The other bits
			// (currently 0-2) are left untouched so a future sapling subtype
			// can keep its type across growth.
			if((metadata & 8) == 0) {
				world.setBlockMetadataWithNotify(x, y, z, metadata | 8);
				return;
			}
			this.growTree(world, x, y, z, random);
		}
	}

	/**
	 * Grows the tree the sapling at {@code (x, y, z)} is meant to become. For
	 * single-sapling trees the sapling cell is cleared and the generator is run;
	 * on failure the cell is restored. For 2x2 trees the surrounding cells are
	 * scanned and the first matching 2x2 is consumed; on failure the four cells
	 * are restored.
	 */
	public final void growTree(World world, int x, int y, int z, Random rand) {
		int saplingId = world.getBlockId(x, y, z);
		int meta = world.getBlockMetadata(x, y, z) & 0xF0;

		EnumTreeType tree = EnumTreeType.findTreeTypeFromSapling(saplingId, meta);
		WorldGenerator worldGen = tree.getGenerator(rand);

		if(tree.getNeedsFourSaplings()) {
			// Search (dx, dz) in {0,-1}x{0,-1} for a 2x2 cluster of matching saplings.
			// The block that just ticked is one of the four; we walk both axes downward
			// so the cluster containing it is always examined.
			boolean2x2Search:
			for(int dx = 0; dx >= -1; --dx) {
				for(int dz = 0; dz >= -1; --dz) {
					if(this.sameSapling(world, x + dx,     y, z + dz,     saplingId, meta) &&
					   this.sameSapling(world, x + dx + 1, y, z + dz,     saplingId, meta) &&
					   this.sameSapling(world, x + dx,     y, z + dz + 1, saplingId, meta) &&
					   this.sameSapling(world, x + dx + 1, y, z + dz + 1, saplingId, meta)) {

						world.setTileNoUpdate(x + dx,     y, z + dz,     0);
						world.setTileNoUpdate(x + dx + 1, y, z + dz,     0);
						world.setTileNoUpdate(x + dx,     y, z + dz + 1, 0);
						world.setTileNoUpdate(x + dx + 1, y, z + dz + 1, 0);

						if(worldGen == null || !worldGen.generate(world, rand, x + dx, y, z + dz)) {
							world.setBlockAndMetadata(x + dx,     y, z + dz,     saplingId, meta);
							world.setBlockAndMetadata(x + dx + 1, y, z + dz,     saplingId, meta);
							world.setBlockAndMetadata(x + dx,     y, z + dz + 1, saplingId, meta);
							world.setBlockAndMetadata(x + dx + 1, y, z + dz + 1, saplingId, meta);
						}
						break boolean2x2Search;
					}
				}
			}
		} else {
			world.setTileNoUpdate(x, y, z, 0);
			if(worldGen == null || !worldGen.generate(world, rand, x, y, z)) {
				world.setTileNoUpdate(x, y, z, saplingId);
				world.setBlockMetadataWithNotify(x, y, z, meta);
			}
		}
	}

	/** True if the cell holds the same sapling id and same upper-nibble metadata. */
	private boolean sameSapling(World world, int x, int y, int z, int expectedId, int expectedMeta) {
		return world.getBlockId(x, y, z) == expectedId
			&& (world.getBlockMetadata(x, y, z) & 0xF0) == expectedMeta;
	}
}
