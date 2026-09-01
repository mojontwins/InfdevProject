package net.minecraft.game.world;

import net.minecraft.game.entity.Entity;

/**
 * Observer for world events.  Implemented by the renderer so it can react
 * to block changes, sound playback, particle spawning, and entity
 * (de)registration without taking a hard dependency on the world.
 */
public interface IWorldAccess {
    /** A single block at (x, y, z) (and its neighbours) has changed. */
    void markBlockAndNeighborsNeedsUpdate(int x, int y, int z);

    /** A range of blocks has changed. */
    void markBlockRangeNeedsUpdate(int x1, int y1, int z1, int x2, int y2, int z2);

    /** A sound effect has been triggered in the world. */
    void playSound(String sound, double x, double y, double z, float volume, float pitch);

    /** A particle has been spawned. */
    void spawnParticle(String particle, double x, double y, double z, double dx, double dy, double dz);

    /** A new entity has been added; the renderer should load its skin. */
    void obtainEntitySkin(Entity entity);

    /** An entity has been removed; the renderer should release its skin. */
    void releaseEntitySkin(Entity entity);

    /** Force every visible chunk renderer to rebuild (used when skylight changes). */
    void updateAllRenderers();
}
