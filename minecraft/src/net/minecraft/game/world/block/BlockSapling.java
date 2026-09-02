package net.minecraft.game.world.block;

import java.util.Random;
import net.minecraft.game.world.World;
import net.minecraft.game.world.terrain.generate.WorldGenBigTree;
import net.minecraft.game.world.terrain.generate.WorldGenTrees;
import net.minecraft.game.world.terrain.generate.WorldGenerator;

/**
 * A sapling: light and time accumulate in its metadata, and at metadata 15 the
 * block grows into a tree — usually a regular {@link WorldGenTrees}, with a
 * small chance of a {@link WorldGenBigTree}.
 *
 * <h2>Metadata layout</h2>
 * <ul>
 *   <li><b>Bit 3 ({@code & 8})</b> — the "ready to grow" state. {@code 0} = still
 *       accumulating, {@code 8} = the next growth tick will attempt to generate
 *       a tree. Set by {@link #updateTick} when the growth conditions are met;
 *       cleared implicitly by the block being replaced with the tree.
 *   <li><b>Bits 0–2 and 4–7</b> — currently unused. When saplings gain real
 *       subtypes, the subtype is expected to live in bits 4–7 (matching
 *       b1.7.3's {@code meta >> 4} layout).
 * </ul>
 *
 * <h2>Future-subtype contract</h2>
 * When sapling subtypes are introduced, any code that reads or stores metadata
 * <em>must</em> mask out bit 3 so the growth state does not collide with the
 * subtype bits. Specifically:
 * <ul>
 *   <li>Reads of metadata for variant/subtype purposes should be
 *       {@code metadata & 0xF7} (or {@code metadata & 0xF0} if subtypes occupy
 *       the upper nibble as in b1.7.3).
 *   <li>Writes that touch the subtype must preserve bit 3, e.g.
 *       {@code (metadata & 8) | (newSubtype & 0xF7)}.
 *   <li>{@link #getBlockTextureFromSideAndMetadata}, {@link #damageDropped} and
 *       any other variant-aware method should accept an already-stripped
 *       metadata and ignore bit 3 entirely.
 * </ul>
 */
public final class BlockSapling extends BlockFlower {
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

	/** The metadata is growth progress, not a variant: a broken sapling always
	 * drops a fresh, damage-0 sapling item. */
	@Override
	public int damageDropped(int metadata) {
		return 0;
	}

	@Override
	public final void updateTick(World world, int x, int y, int z, Random random) {
		super.updateTick(world, x, y, z, random);
		if(world.getBlockLightValue(x, y + 1, z) >= 10 && random.nextInt(20) == 0) {
			int metadata = world.getBlockMetadata(x, y, z);
			// NOTE: bit 3 (& 8) is the "ready to grow" state. The other bits
			// (currently 0–2) are left untouched so a future sapling subtype
			// (when subtypes are added — see
			// docs/blocks_with_subtypes_interface.md) can keep its type
			// across growth.
			if((metadata & 8) == 0) {
				world.setBlockMetadataWithNotify(x, y, z, metadata | 8);
				return;
			}
			world.setTileNoUpdate(x, y, z, 0);
			WorldGenerator treeGenerator = random.nextInt(10) == 0 ? new WorldGenBigTree() : new WorldGenTrees();
			if(!treeGenerator.generate(world, random, x, y, z)) {
				world.setTileNoUpdate(x, y, z, this.blockID);
			}
		}
	}
}
