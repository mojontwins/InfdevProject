package net.minecraft.game.world.block;

/**
 * Marker interface for blocks whose icon and tint depend on metadata.
 * Blocks implementing this interface automatically get an {@link ItemBlockWithSubtypes}
 * instance instead of a plain {@link ItemBlock} when registered in {@link Block#blocksList}.
 */
public interface IBlockWithSubtypes {
    int getBlockTextureFromSideAndMetadata(int side, int metadata);
}