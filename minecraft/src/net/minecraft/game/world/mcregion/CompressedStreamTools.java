package net.minecraft.game.world.mcregion;

import com.mojang.nbt.NBTBase;
import com.mojang.nbt.NBTTagCompound;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * Low-level NBT (de)serialization used by the McRegion chunk format. Unlike
 * {@link net.minecraft.client.LoadingScreenRenderer} (which hard-wires gzip), these
 * methods read and write plain, uncompressed named compounds so the
 * {@link RegionFile} container can choose its own compression (zlib for the
 * McRegion chunk payload). The two call sites this replaces produced gzip'd
 * {@code c.x.z.dat} files; McRegion chunks are deflate'd instead.
 *
 * <p>Deliberately a small public utility: {@code RegionFile}/{@code RegionFileCache} are its
 * primary consumers, but the chunk provider also uses it to read the decompressed payload a
 * region hands back.
 */
public final class CompressedStreamTools {
	private CompressedStreamTools() {
	}

	/** Reads a named compound tag from an already-decompressed stream. */
	public static NBTTagCompound read(DataInput in) throws IOException {
		NBTBase tag = NBTBase.readNamedTag(in);
		if(tag instanceof NBTTagCompound) {
			return (NBTTagCompound)tag;
		}
		throw new IOException("Root tag must be a named compound tag");
	}

	/** Writes a named compound tag to the given stream (left uncompressed). */
	public static void write(NBTTagCompound tag, DataOutput out) throws IOException {
		NBTBase.writeNamedTag(tag, out);
	}
}
