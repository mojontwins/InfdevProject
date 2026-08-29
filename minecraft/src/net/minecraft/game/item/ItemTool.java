package net.minecraft.game.item;

import java.util.stream.Stream;
import net.minecraft.game.world.block.Block;

/**
 * Shared behaviour of the digging tools (shovel, pickaxe, axe): which types of
 * block they cut through fastest, how much durability each swing costs, and how
 * hard they hit.
 */
public class ItemTool extends Item {
	/**
	 * The blocks this tool is "effective against", each as an {@link ItemStack}
	 * whose {@code itemDamage} field holds the block metadata (state) it matches:
	 * {@code -1} means "any metadata", any other value requires that exact state.
	 * A shovel is therefore effective against every grass block, but can be
	 * narrowed later to, say, only stair blocks facing one direction.
	 */
	private final ItemStack[] blocksEffectiveAgainst;
	private final float efficiencyOnProperMaterial;
	private final int damageVsEntity;

	/**
	 * @param materialTier 0 = wood/gold, 1 = stone, 2 = steel (iron), 3 = diamond.
	 * The higher the tier, the more durability and the faster the digging.
	 */
	public ItemTool(int itemID, int baseDamage, int materialTier, ItemStack[] blocksEffectiveAgainst) {
		super(itemID);
		this.blocksEffectiveAgainst = blocksEffectiveAgainst;
		this.maxStackSize = 1;
		this.maxDamage = 32 << materialTier;
		if (materialTier == 3) {
			this.maxDamage <<= 1;
		}

		this.efficiencyOnProperMaterial = (float) ((materialTier + 1) << 1);
		this.damageVsEntity = baseDamage + materialTier;
	}

	/** Returns an {@link ItemStack} describing one block state for the effective list. */
	protected static ItemStack blockStack(Block block, int metadata) {
		return new ItemStack(block.blockID, 1, metadata);
	}

	/** Digging speed bonus against the blocks this tool is designed for. */
	@Override
	public final float getStrVsBlock(Block block) {
		return this.getStrVsBlock(block, -1);
	}

	@Override
	public final float getStrVsBlock(Block block, int metadata) {
		return Stream.of(this.blocksEffectiveAgainst)
				.anyMatch(entry -> entry.itemID == block.blockID && (entry.itemDamage == -1 || entry.itemDamage == metadata))
				? this.efficiencyOnProperMaterial
				: 1.0F;
	}

	@Override
	public final void hitEntity(ItemStack stack) {
		stack.damageItem(2);
	}

	@Override
	public final void onBlockDestroyed(ItemStack stack) {
		stack.damageItem(1);
	}

	@Override
	public final int getDamageVsEntity() {
		return this.damageVsEntity;
	}
}