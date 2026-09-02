package net.minecraft.game.item;

import net.minecraft.game.world.block.Block;
import net.minecraft.game.world.block.IBlockWithSubtypes;

/**
 * An {@link ItemBlock} that is automatically swapped in when a block declares
 * {@link IBlockWithSubtypes}. It sets {@link Item#getHasSubTypes hasSubTypes=true}
 * so the damage bar is never drawn, and overrides
 * {@link Item#getIconFromDamage(int)} and {@link Item#getColorFromDamage(int)}
 * to delegate to the wrapped block.
 */
public final class ItemBlockWithSubtypes extends ItemBlock {

    public ItemBlockWithSubtypes(int itemID) {
        super(itemID);
        this.setHasSubTypes(true);
    }

    @Override
    public int getIconFromDamage(int damage) {
        return Block.blocksList[this.blockID].getBlockTextureFromSideAndMetadata(2, damage);
    }

    @Override
    public int getColorFromDamage(int damage) {
        return Block.blocksList[this.blockID].getRenderColor(damage);
    }
}